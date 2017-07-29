package io.github.qwefgh90.repogarden.web.model
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.eclipse.egit.github.core._

object Implicits {
  implicit val userReads = new Reads[User]{
    def reads(json: JsValue) = {
      val user = new User()
      user.setLogin((json \ "id").as[String])
      user.setName((json \ "username").as[String])
      user.setAvatarUrl((json \ "imgUrl").as[String])
      new JsSuccess(user)
    }
  }

  implicit val initialVectorWrites = new Writes[InitialVector] {
    def writes(vec: InitialVector) = Json.obj(
      "client_id" -> vec.client_id,
      "state" -> vec.state
    )
  }
  
  implicit val userInformationWrites = new Writes[UserInformation] {
    def writes(user: UserInformation) = Json.obj(
     "userId" -> user.userId   
    )
  }

  implicit val userWritesToBrowser = new Writes[org.eclipse.egit.github.core.User] {
    def writes(user: org.eclipse.egit.github.core.User) = Json.obj(
      "id" -> user.getLogin,
      "username" -> user.getName,
      "firstName" -> "",
      "lastName" -> "",
      "expiredDate" -> "",
      "imgUrl" -> user.getAvatarUrl
    )
  }

  implicit val repositoryWritesToBrowser = new Writes[org.eclipse.egit.github.core.Repository] {
    def writes(repo: org.eclipse.egit.github.core.Repository) = Json.obj(
      "name" -> repo.getName,
      "accessLink" -> repo.getUrl,
      "activated" -> !repo.isPrivate(),
      "cves" -> List("")
    )
  }

  implicit val cveWritesToBrowser = new Writes[Cve] {
    def writes(cve: Cve) = Json.obj(
      "cve" -> cve.cve,
      "title" -> cve.title,
      "description" -> cve.description,
      "references" -> cve.references
    )
  }
}
