package fr.valwin.payzen

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
    val out = for {
      header <- (body \ "HEADER").headOption
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
                        authToken: String): Either[String, Map[String, String]] = {
    val view = views.xml.modifyAndValidateEnveloppe(shopId, requestId, queryDate, ctxMode, authToken, comment, uuid)
    val responseFuture = performRequest(view)
    analyzeResponse(Await.result(responseFuture, timeout))
  }

  def uuidFromLegacy(shopId: String,
                     requestId: String,
                     queryDate: javax.xml.datatype.XMLGregorianCalendar,
                     ctxMode: String,
                     authToken: String,
                     transactionId: String,
                     seqNb: Int,
                     transDate: String,
                     cert: String
                    ): Future[Either[String, String]] = {
    val view = views.xml.getUUIDFromLegacy(shopId, requestId, queryDate, ctxMode, authToken, transactionId, seqNb, transDate)
    performRequest(view).map{ response =>
      val xml = response.xml
      val validate = validateResponseHeader(xml, cert)
      validate.fold(
        e => Left(e),
        s => {
          val uuid = xml \\ "getPaymentUuidResponse" \ "legacyTransactionKeyResult" \\ "transactionUuid"
          Right(uuid.text)
        }
      )
    }
  }

  def cancel(shopId: String, transDate: javax.xml.datatype.XMLGregorianCalendar, transId: String, seqNb: Int, ctxMode: String, comment: String, signature: String) = {
    val responseFuture = performRequest(views.xml.cancelEnveloppe(shopId, transDate, transId, seqNb, ctxMode, comment, signature))
    analyzeResponse(Await.result(responseFuture, timeout))
  }
}
