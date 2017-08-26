package io.github.qwefgh90.repogarden.web.actor

import akka.actor._
import javax.inject._
import play.api.libs.json._
import play.api._
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator.{ Subscribe, SubscribeAck, Publish }

object PubActor {
  case class PubMessage(id: Long, value: JsValue)
  case class TerminateMessage(id: Long, value: Option[JsValue])
  case object Poison
}

class PubActor @Inject() (configuration: Configuration) extends Actor {
  import PubActor._
  val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case PubMessage(id, value) ⇒
      mediator ! Publish(id.toString, value)
    case TerminateMessage(id, valueOpt) =>
      valueOpt.map{value => 
        mediator ! Publish(id.toString, value)
      }
      mediator ! Publish(id.toString, Poison)
  }
}
