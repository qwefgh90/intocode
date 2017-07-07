package util

import org.eclipse.egit.github.core.service._
import play.api.Configuration
import javax.inject._
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import org.eclipse.egit.github.core.Authorization
import org.eclipse.egit.github.core.client.GitHubClient
import play.api.libs.ws._
import scala.concurrent._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/**
 * 
 */
@Singleton
class AuthService @Inject() (configuration: Configuration, encryption: Encryption, ws: WSClient, context: ExecutionContext) {
  private val clientId = configuration.getString("play.clientId")
  require(clientId.nonEmpty)
  private val accessTokenUrl = configuration.getString("play.accessTokenUrl")
  require(accessTokenUrl.nonEmpty)
  private val clientSecret = configuration.getString("play.clientSecret")
  require(clientSecret.nonEmpty)
  
  case class AccessToken(token: String, tokenType: String)
  implicit val accessTokenRead: Reads[AccessToken] = (
    (JsPath \ "access_token").read[String] and
    (JsPath \ "token_type").read[String]
  )(AccessToken.apply _)

  def getState: Array[Byte] = {
    val plainText = System.currentTimeMillis.toString.getBytes 
    encryption.encrypt(plainText)
  }
  
  def getClientId: String = clientId.get
  
  def getAccessToken(code: String, state: String, clientId: String): Future[String] = {
    val response: Future[WSResponse] = ws.url(accessTokenUrl.get)
      .withHeaders("Accept" -> "application/json")
      .post(Map("client_id" -> Seq(clientId)
        , "client_secret" -> Seq(clientSecret.get)
        , "code" -> Seq(code)
        , "state" -> Seq(state)))
    val tokenFuture = response.map(response => response.json.as[AccessToken]
    )(context)
    
    return tokenFuture.map(_.token)(context)
  }
}

@Singleton
class Encryption @Inject() (configuration: Configuration){
  private val keyBytes = configuration.getString("play.keyBytes")
  require(keyBytes.nonEmpty && keyBytes.get.length == 16)
  private val ivBytes = configuration.getString("play.ivBytes")
  require(ivBytes.nonEmpty && ivBytes.get.length == 16)
  
  private val key = new SecretKeySpec(keyBytes.get.getBytes, "AES")
  private val ivSpec = new IvParameterSpec(ivBytes.get.getBytes)
  
  def encrypt(plainText: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
    cipher.doFinal(plainText)
  }
  
  def decrypt(encrypted: Array[Byte]): Array[Byte] = {
		val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
	  cipher.doFinal(encrypted)
  }
}

