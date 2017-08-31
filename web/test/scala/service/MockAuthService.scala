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

class MockAuthService(accessToken: String = "", clientId: String = "", state: String = "")(implicit val context: ExecutionContext) extends AuthServiceTrait {
  override def getState: String = "fake"
  override def getClientId: String = "fake"
  override def getAccessToken(code: String, state: String, clientId: String): Future[String] = {
    Future{
      accessToken
    }
  }
}
