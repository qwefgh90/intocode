package io.github.qwefgh90.repogarden.web.module

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import io.github.qwefgh90.repogarden.web.actor.PubActor
import io.github.qwefgh90.repogarden.web.controllers.{DefualtUserSessionChecker, UserSessionChecker}

class ConfigureModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[PubActor]("pub-actor")
    bind(classOf[UserSessionChecker])
     .to(classOf[DefualtUserSessionChecker])
  }
}
