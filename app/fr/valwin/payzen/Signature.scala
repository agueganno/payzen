package fr.valwin.payzen

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.util.control.Exception._


/**
 * @author Valentin Kasas
 */
object Signature {

  def computeHash(data: Map[String, String], certificate: String) = {
    val toHash = data.toList.sortBy(_._1).foldRight(certificate)((e, a) => s"${e._2}+$a").getBytes("UTF-8")
    val md = MessageDigest.getInstance("SHA-1")
    md.update(toHash, 0, toHash.length)
    val sha1 = md.digest()
    sha1.map{ byte =>
      Integer.toHexString((byte >> 4) & 0xF )  + Integer.toHexString(byte & 0xF)
    }.fold("")(_ + _)
  }

  def computeAuthToken(requestId: String, timeStamp: String, certificate: String): String = {
    val conc = requestId + timeStamp
    hmacSHA256(conc, certificate)
  }

  def hmacSHA256(stringToSign: String, key: String): String = {
    val bytes = encode256 ( key .getBytes( "UTF-8" ), stringToSign .getBytes( "UTF-8" ))
    Base64.getEncoder.encodeToString(bytes)
  }

  def encode256(keyBytes: Array[Byte], text: Array[Byte]): Array[Byte] = {
    val hmacSha1 = (allCatch opt Mac.getInstance("HmacSHA256")).getOrElse(Mac.getInstance("HMAC-SHA-256"))
    val macKey: SecretKeySpec  = new SecretKeySpec( keyBytes, "RAW" )
    hmacSha1.init(macKey)
    hmacSha1.doFinal(text)
  }

}
