package io.github.qwefgh90.repogarden.web.service

import org.eclipse.egit.github.core.service._
import play.api.Configuration
import play.api.http._
import java.util.Base64
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
import play.Logger

/**
 * this interface is for AuthService and MockAuthService.
 */

trait AuthServiceTrait {
  case class AccessToken(token: String, tokenType: String)
  def getState: String
  def getClientId: String
  def getAccessToken(code: String, state: String): Future[String]
}

class AuthService (configuration: Configuration, encryption: Encryption, ws: WSClient, context: ExecutionContext, baseOAuthBaseUrl: String) extends AuthServiceTrait {
  @Inject def this(configuration: Configuration, encryption: Encryption, ws: WSClient, context: ExecutionContext)
    = this(configuration, encryption, ws, context, configuration.getString("play.baseOAuthBaseUrl").getOrElse("https://github.com"))
  
  private val clientId = configuration.getString("play.clientId")
  require(clientId.nonEmpty)
  private val accessTokenPath = configuration.getString("play.accessTokenPath")
  require(accessTokenPath.nonEmpty)
  private val clientSecret = configuration.getString("play.clientSecret")
  require(clientSecret.nonEmpty)

  private val encoder = Base64.getUrlEncoder
  private val decoder = Base64.getUrlDecoder

  implicit val accessTokenRead: Reads[AccessToken] = (
    (JsPath \ "access_token").read[String] and
    (JsPath \ "token_type").read[String]
  )(AccessToken.apply _)

  /*
   * WARNING
   * it must be unsuggestable random string.
   */
  @deprecated("It is not able to make safe keys which can be suggestable", "AuthService.scala")
  override def getState: String = {
    val plainText = System.currentTimeMillis.toString.getBytes 
    val encrypted = encryption.encrypt(plainText)
    encoder.encodeToString(encrypted)
  }
  
  override def getClientId: String = clientId.get
  
  override def getAccessToken(code: String, state: String): Future[String] = {
    val response: Future[WSResponse] = ws.url(baseOAuthBaseUrl + accessTokenPath.get)
      .withHeaders("Accept" -> "application/json")
      .post(Map("client_id" -> Seq(getClientId)
        , "client_secret" -> Seq(clientSecret.get)
        , "code" -> Seq(code)
        , "state" -> Seq(state)))
         
    val tokenFuture = response.map(response => {
      if(response.status == Status.OK)
        response.json.as[AccessToken]
      else
        throw new RuntimeException(response.body)
    })(context)
    
    return tokenFuture.map(_.token)(context)
  }
}

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

