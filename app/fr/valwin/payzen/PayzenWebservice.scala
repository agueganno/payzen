package fr.valwin.payzen

import play.api.libs.ws.{WSResponse, WS}
import play.mvc.Http
import play.twirl.api.Xml
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.Logger
import play.api.Play.current

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
       val ret = response.xml \\ "return"
       Right(ret(0).map{elem =>
         elem.label -> elem.text
       }.toMap)
     } else {
       Left("Communication Error")
     }
  }

  def modifyAndValidate(shopId: String,
                        comment: String,
                        requestId: String,
                        transDate: javax.xml.datatype.XMLGregorianCalendar,
                        uuid: String,
                        ctxMode: String,
                        authToken: String): Either[String, Map[String, String]] = {
    val view = views.xml.modifyAndValidateEnveloppe(shopId, comment, requestId, transDate, ctxMode, authToken, uuid)
    val responseFuture = performRequest(view)
    analyzeResponse(Await.result(responseFuture, timeout))
  }

  def cancel(shopId: String, transDate: javax.xml.datatype.XMLGregorianCalendar, transId: String, seqNb: Int, ctxMode: String, comment: String, signature: String) = {
    val responseFuture = performRequest(views.xml.cancelEnveloppe(shopId, transDate, transId, seqNb, ctxMode, comment, signature))
    analyzeResponse(Await.result(responseFuture, timeout))
  }
}
