package io.github.qwefgh90.repogarden.web.actor

import akka.actor._
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator.{ Subscribe, SubscribeAck, Publish }

object ClientActor {
  def props(out: ActorRef, rh: RequestHeader, channelId: String) = Props(new ClientActor(out, rh, channelId))
}

class ClientActor(out: ActorRef, rh: RequestHeader, channelId: String) extends Actor {
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(channelId, self)

  def receive = {
    case jsValue: JsValue =>
      out ! jsValue
    case SubscribeAck(Subscribe(channelId, None, `self`)) ⇒{
      Logger.debug(s"A channel is open: ${channelId}")
      Logger.debug(s"${rh.remoteAddress} starts to subscribe: ${channelId}")

    }
  }
}
