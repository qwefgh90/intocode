package io.github.qwefgh90.repogarden.web.model
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.eclipse.egit.github.core._

object Implicits extends RepositoryExtension {
  implicit val userReads = new Reads[User]{
    def reads(json: JsValue) = {
      val user = new User()
      user.setLogin((json \ "id").as[String])
      user.setName((json \ "username").as[String])
      user.setEmail((json \ "email").as[String])
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
  
  val userWritesToSession = new Writes[org.eclipse.egit.github.core.User] {
    def writes(user: org.eclipse.egit.github.core.User) = Json.obj(
      "id" -> user.getLogin,
      "email" -> user.getEmail,
      "username" -> user.getName,
      "firstName" -> "",
      "lastName" -> "",
      "expiredDate" -> "",
      "imgUrl" -> user.getAvatarUrl
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

  implicit val branchToBrowser = new Writes[org.eclipse.egit.github.core.RepositoryBranch] {
    def writes(branch: org.eclipse.egit.github.core.RepositoryBranch) = Json.obj(
      "name" -> branch.getName
    )
  }

  implicit val commitWritesToBrowser = new Writes[org.eclipse.egit.github.core.RepositoryCommit] {
    def writes(repoCommit: org.eclipse.egit.github.core.RepositoryCommit) = {
      val commit = repoCommit.getCommit
      Json.obj(
        "sha" -> commit.getSha,
        "message" -> commit.getMessage,
        "date" -> commit.getCommitter.getDate,
        "committerEmail" -> commit.getCommitter.getEmail,
        "committerName" -> commit.getCommitter.getName,
        "url" -> commit.getUrl
      )
    }
  }

  implicit val repositoryWritesToBrowser = new Writes[org.eclipse.egit.github.core.Repository] {
    def writes(repo: org.eclipse.egit.github.core.Repository) = Json.obj(
      "owner" -> repo.getOwner.getName,
      "name" -> repo.getName,
      "accessLink" -> repo.getUrl,
      "activated" -> true
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
