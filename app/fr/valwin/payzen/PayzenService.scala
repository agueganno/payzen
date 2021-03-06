package fr.valwin.payzen

import play.api.mvc.{BodyParser, BodyParsers, Results}
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime
import javax.xml.datatype.DatatypeFactory
import java.util.GregorianCalendar

import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

case class ClientParameters(
                             vads_version: String,
                             vads_currency: String,
                             vads_page_action: String,
                             vads_payment_config: String,
                             vads_action_mode: String,
                             vads_ctx_mode: String,
                             vads_site_id: String
                           ){
  lazy val toMap = Map(
    "vads_version" -> vads_version,
    "vads_currency" -> vads_currency,
    "vads_page_action" -> vads_page_action,
    "vads_payment_config" -> vads_payment_config,
    "vads_action_mode" -> vads_action_mode,
    "vads_ctx_mode" -> vads_ctx_mode,
    "vads_site_id" -> vads_site_id
  )
}

object ClientParameters {
  implicit val formatter = Json.format[ClientParameters]
}

case class PayzenData(certificate: String, clientParameters: ClientParameters)

object PayzenData {
  implicit val formatter = Json.format[PayzenData]
}
/**
 * @author Valentin Kasas
 */
object PayzenService {

  type Data = Map[String, String]

  lazy val paramatersDefinition = List(
    ("vads_action_mode",              "None",           "47",     "X"),
    ("vads_amount",                   "n..12",          "09",     "X"),
    ("vads_currency",                 "n3",             "10",     "X"),
    ("vads_ctx_mode",                 "None",           "11",     "X"),
    ("vads_page_action",              "None",           "46",     "X"),
    ("vads_payment_config",           "None",           "07",     "X"),
    ("vads_site_id",                  "n8",             "02",     "X"),
    ("vads_trans_date",               "n14",            "04",     "X"),
    ("vads_trans_id",                 "n6",             "03",     "X"),
    ("vads_version",                  "None",           "01",     "X"),
    ("signature",                     "an40",           "00/70",  "X"),
    ("vads_capture_delay",            "n..3",           "06",     "F"),
    ("vads_contrib",                  "ans..255",       "31",     "F"),
    ("vads_cust_address",             "ans..255",       "19",     "F"),
    ("vads_cust_address_number",      "an..5",          "112",    "F"),
    ("vads_cust_country",             "a2",             "22",     "F"),
    ("vads_cust_email",               "an.127",         "15",     "C"),
    ("vads_cust_id",                  "an..63",         "16",     "F"),
    ("vads_cust_name",                "ans..127",       "18",     "F"),
    ("vads_cust_ last_ name",         "ans..63",        "105",    "F"),
    ("vads_cust_first_name",          "ans..63",        "104",    "F"),
    ("vads_cust_cell_phone",          "an..32",         "77",     "F"),
    ("vads_cust_phone",               "an..32",         "23",     "F"),
    ("vads_cust_title",               "an..63",         "17",     "F"),
    ("vads_cust_city",                "ans..63",        "21",     "F"),
    ("vads_cust status",              "an..63",         "92",     "F"),
    ("vads_cust_state",               "an..63",         "88",     "F"),
    ("vads_cust_zip",                 "an..63",         "20",     "F"),
    ("vads_language",                 "a2",             "12",     "F"),
    ("vads_order_id",                 "an..32",         "13",     "F"),
    ("vads_order_info",               "an..255",        "14",     "F"),
    ("vads_order_info2",              "an..255",        "14",     "F"),
    ("vads_order_info3",              "an..255",        "14",     "F"),
    ("vads_payment_cards",            "an..127",        "08",     "F"),
    ("vads_return_mode",              "GET/POST/NONE",  "48",     "C"),
    ("vads_theme_config",             "ans..255",       "32",     "F"),
    ("vads_validation_mode",          "n..1",           "05",     "F"),
    ("vads_url_success",              "ans..127",       "24",     "F"),
    ("vads_url_referral",             "ans..127",       "26",     "F"),
    ("vads_url_refused",              "ans..127",       "25",     "F"),
    ("vads_url_cancel",               "ans..127",       "27",     "F"),
    ("vads_url_error",                "ans..127",       "29",     "F"),
    ("vads_url_return",               "ans..127",       "28",     "F"),
    ("vads_user_info",                "ans..255",       "61",     "F"),
    ("vads_contracts",                "ans..255",       "62",     "C"),
    ("vads_shop_name",                "ans..255",       "72",     "F"),
    ("vads_redirect_success_timeout", "n..3",           "34",     "F"),
    ("vads_redirect_success_message", "ans..255",       "35",     "F"),
    ("vads_redirect_error_timeout",   "n..3",           "36",     "F"),
    ("vads_redirect_error_message",   "ans..255",       "37",     "F"),
    ("vads_ship_to_city",             "an..63",         "83",     "F"),
    ("vads_ship_to_country",          "a2",             "86",     "F"),
    ("vads_ship_to_name",             "an..127",        "80",     "F"),
    ("vads_ship_to_phone_num",        "an..32",         "87",     "F"),
    ("vads_ship_to_state",            "ans..255",       "84",     "F"),
    ("vads_ship_to_street",           "ans..255",       "81",     "F"),
    ("vads_ship_to_street2",          "ans..255",       "82",     "F"),
    ("vads_ship_to_street_number",    "an..5",          "114",    "F"),
    ("vads_ship_to_zip",              "an..63",         "85",     "F")
  )

  lazy val mandatoryKeys = paramatersDefinition.filter(_._4 == "X").map(_._1).toSet


  private def checkMandatoryParameters(params: Data):Either[PayzenError, Data] = {
    val missingMandatory = mandatoryKeys -- params.keySet.intersect(mandatoryKeys)
    if(missingMandatory.nonEmpty){
      Left(MissingMandatoryParameter(missingMandatory))
    } else {
      Right(params)
    }
  }

  /**
   * Verifies that all mandatory parameters are present, and add the proper
   * signature according to the given parameters.
   *
   * TODO: Each value in the given map is checked for validity against the
   * parameter format definition.
   *
   * @param parameters a Map[String,String] representing the form data
   * @return either a Map[String, String] with all the given parameters plus
   *            the required signature
   *         or a MissingMandatoryParameter containing the missing keys
   *
   */
  def verifyAndSign(certificate: String, parameters: Data):Either[PayzenError,Data] = {
    val data = parameters + ("signature" -> Signature.computeHash(parameters.filterKeys(_.startsWith("vads_")), certificate))
    checkMandatoryParameters(data)
  }

  /**
   * Use this method to verify the authenticity of the form POSTed on your
   * server-server return URL
   *
   * @param data the form data, as a Map[String, String]
   * @return either MissingSignature if the signature is absent from the data
   *         or SignatureError if the signature does not match the data + certificate
   *         or the data itself if the signature is correct
   */
  def verifySignature(certificate: String, data:Data):Either[PayzenError, Data] = {
    if(!data.contains("signature")){
      Left(MissingSignature)
    } else {
      val signature = data("signature")
      if(Signature.computeHash(data.filterKeys(_.startsWith("vads_")), certificate) != signature) {
        Left(SignatureError)
      } else {
        Right(data)
      }
    }
  }

  def prepareData[T](clientData: PayzenData, basket: T)(implicit basket2Data: T => Data):Either[PayzenError, Data] = {
    val parameters = clientData.clientParameters.toMap ++ basket2Data(basket)
    verifyAndSign(clientData.certificate, parameters)
  }

  def parse(certificate: String) = BodyParser {
    BodyParsers.parse.urlFormEncoded.andThen {
      iteratee =>
        iteratee.map{
          either => either.fold(
            error => Left(error),
            data =>  verifySignature(certificate, data.filter(_._2.nonEmpty).map(pair => pair._1 -> pair._2.head).toMap).fold(
              payzenError => Left(Results.BadRequest),
              verifiedData => Right(verifiedData)
            )
          )
        }
    }
  }

  def toXMLDate(date : DateTime) = {
    val xmlDate = new GregorianCalendar()
    xmlDate.setTime(date.toDate)
    DatatypeFactory.newInstance().newXMLGregorianCalendar(xmlDate)
  }

  def confirmOrder(clientData: PayzenData, transDate: DateTime, transId: String, seqNb:Int, amount: Long, remiseDate:DateTime ) = {
    val shopId = clientData.clientParameters.vads_site_id
    val mode = clientData.clientParameters.vads_ctx_mode
    val currency = clientData.clientParameters.vads_currency.toInt
    val format = DateTimeFormat.forPattern("YYYYMMdd").withZoneUTC()
    val parameters:Map[String, String] = Map(
      "1" -> shopId,
      "2" -> format.print(transDate),
      "3" -> transId,
      "4" -> seqNb.toString,
      "5" -> mode,
      "6" -> amount.toString,
      "7" -> currency.toString,
      "8" -> format.print(remiseDate),
      "9" -> ""
    )
    PayzenWebservice.modifyAndValidate(
      shopId,
      toXMLDate(transDate),
      transId,
      seqNb,
      mode,
      amount,
      currency,
      toXMLDate(remiseDate),
      "",
      Signature.computeHash(parameters, clientData.certificate)
    )
  }

  def cancelOrder(clientData: PayzenData, transDate: DateTime, transId: String, seqNb:Int, amount: Long, remiseDate:DateTime) = {
    val shopId = clientData.clientParameters.vads_site_id
    val mode = clientData.clientParameters.vads_ctx_mode
    val format = DateTimeFormat.forPattern("YYYYMMdd").withZoneUTC()
    val parameters:Map[String, String] = Map(
      "1" -> shopId,
      "2" -> format.print(transDate),
      "3" -> transId,
      "4" -> seqNb.toString,
      "5" -> mode,
      "6" -> ""
    )
    PayzenWebservice.cancel(shopId, toXMLDate(transDate), transId, seqNb, mode, "",  Signature.computeHash(parameters, clientData.certificate))

  }

  trait PayzenError
  case object PayzenPluginError extends PayzenError
  case object SignatureError extends PayzenError
  case object MissingSignature extends PayzenError
  case class MissingMandatoryParameter(missingKeys : Set[String]) extends PayzenError

}

