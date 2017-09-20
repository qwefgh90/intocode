package io.github.qwefgh90.repogarden.web.actor

import akka.actor._
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator.{ Subscribe, SubscribeAck, Publish }
import org.eclipse.egit.github.core.{User}

object ClientActor {
  def props(out: ActorRef, rh: RequestHeader, channelId: String, user: User) = Props(new ClientActor(out, rh, channelId, user))
}

class ClientActor(out: ActorRef, rh: RequestHeader, channelId: String, user: User) extends Actor {
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(channelId, self)

  import PubActor._
  def receive = {
    case jsValue: JsValue =>
      Logger.debug(s"${jsValue} is sent in ${channelId}")
      out ! jsValue
    case SubscribeAck(Subscribe(channelId, None, `self`)) ⇒{
      Logger.debug(s"A channel is open: ${channelId}")
      Logger.debug(s"${rh.remoteAddress} starts to subscribe: ${channelId}")
    } 
    case Poison => {
      Logger.debug("terminate: " + channelId.toString)
      context.stop(self)
    }
  }
}
