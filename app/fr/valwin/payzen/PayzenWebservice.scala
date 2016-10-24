package fr.valwin.payzen

import java.time.format.DateTimeFormatter
import javax.xml.datatype.XMLGregorianCalendar

import com.lyra.vads.ws.v5.ValidatePaymentResponse.ValidatePaymentResult
import com.lyra.vads.ws.v5._
import com.profesorfalken.payzen.webservices.sdk.ServiceResult
import com.profesorfalken.payzen.webservices.sdk.builder.PaymentBuilder
import com.profesorfalken.payzen.webservices.sdk.client.ClientV5
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.ws.{WS, WSResponse}
import play.mvc.Http
import play.twirl.api.Xml

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.Logger
import play.api.Play.current

import scala.xml.{Elem, NodeSeq}

/**
 * @author Valentin Kasas
 */
object PayzenWebservice {

  val baseUrl = "https://secure.payzen.eu/vads-ws/v5"
  val timeout = 30.seconds

  def performRequest(body: Xml) = WS.url(baseUrl)
    .withHeaders(
      Http.HeaderNames.CONTENT_TYPE ->  "text/xml; charset=utf-8",
      "SOAPAction" -> ""
    ).post(body)

  def analyzeResponse(response: WSResponse):Either[String, Map[String, String]] = {
     if(Set(200, 201).contains(response.status)){
       val ret: NodeSeq = response.xml \\ "return"
       Right(ret(0).map{elem =>
         elem.label -> elem.text
       }.toMap)
     } else {
       Left("Communication Error")
     }
  }

  def validateResponseHeader(body: Elem, cert: String): Either[String, Unit] = {
    Logger.warn("AAAAAAAAAAAAAAAAAAAAAAA")
    Logger.warn(body.toString)
    Logger.warn("AAAAAAAAAAAAAAAAAAAAAAA")
    val out = for {
      header <- (body \\ "HEADER").headOption
      timestamp <- (header \ "timestamp").headOption.map(_.text)
      requestId <- (header \ "requestId").headOption.map(_.text)
      mode <- (header \ "mode").headOption.map(_.text)
      authToken <- (header \ "authToken").headOption.map(_.text)
    } yield {
      val sig = Signature.computeAuthToken(requestId, timestamp, cert)
      if(authToken == sig){
        Right(())
      } else {
        Left("Response validation failed")
      }
    }
    out  getOrElse Left("Could not gather the response header data for signature validation")
  }

  def modifyAndValidate(shopId: String,
                        comment: String,
                        requestId: String,
                        queryDate: javax.xml.datatype.XMLGregorianCalendar,
                        uuid: String,
                        ctxMode: String,
                        authToken: String): String = {
    val client = new ClientV5(new java.util.HashMap[String, String]())
    val quer = new QueryRequest()
    quer.setUuid(uuid)
    val res: ValidatePaymentResult = client.getPaymentAPIImplPort().validatePayment(new CommonRequest(), quer)
    res.getCommonResponse.getTransactionStatusLabel
  }

  def buildClientParameters(
                             shopId: String,
                             shopKey: String,
                             mode: String
                           ) = {
    val out = new java.util.HashMap[String, String]()
    out.put("shopId", shopId)
    out.put("shopKey", shopKey)
    out.put("mode", mode)
    out.put("endpointHost", "secure.payzen.eu")
    out.put("secureConnection", "true")
    out.put("disableHostnameVerifier", "false")
    out
  }

  def uuidFromLegacy(shopId: String,
                     requestId: String,
                     queryDateStr: String,
                     ctxMode: String,
                     authToken: String,
                     transactionId: String,
                     seqNb: Int,
                     transDate: DateTime,
                     cert: String
                    ): String = {
    val params = buildClientParameters(shopId, cert, ctxMode)
    val client = new ClientV5(params)
    val api = client.getPaymentAPIImplPort

    val transactionKey = new LegacyTransactionKeyRequest()
    transactionKey.setTransactionId(transactionId)
    transactionKey.setCreationDate(PayzenService.toXMLDate(transDate))
    transactionKey.setSequenceNumber(seqNb)

    val keyResult: GetPaymentUuidResponse.LegacyTransactionKeyResult = api.getPaymentUuid(transactionKey)
    keyResult.getPaymentResponse.getTransactionUuid
  }

  def cancel(shopId: String, transDate: javax.xml.datatype.XMLGregorianCalendar, transId: String, seqNb: Int, ctxMode: String, comment: String, signature: String) = {
    val responseFuture = performRequest(views.xml.cancelEnveloppe(shopId, transDate, transId, seqNb, ctxMode, comment, signature))
    analyzeResponse(Await.result(responseFuture, timeout))
  }
}
