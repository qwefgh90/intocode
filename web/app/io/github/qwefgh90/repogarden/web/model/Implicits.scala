package io.github.qwefgh90.repogarden.web.model
import play.api.libs.json._

object Implicits {
  implicit val initialVectorWrites = new Writes[InitialVector] {
    def writes(vec: InitialVector) = Json.obj(
      "client_id" -> vec.client_id,
      "state" -> vec.state
    )
  }
}