package io.github.qwefgh90.repogarden.web.controllers

import org.eclipse.egit.github.core.{User}
import play.api.mvc.{RequestHeader}
import javax.inject.{Inject}

class MockUserSessionChecker @Inject()() extends UserSessionChecker {

  override def checkSign(rh: RequestHeader): Boolean = true
  override def getUserFromSession(rh: RequestHeader): Option[User] = Some(new User)
  override def getTokenOfUser(user: User): Option[String] = Some("")
}
