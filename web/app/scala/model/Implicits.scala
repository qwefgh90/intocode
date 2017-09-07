package io.github.qwefgh90.repogarden.web.model
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import org.eclipse.egit.github.core._

object Implicits extends RepositoryExtension {
  implicit val userReads = new Reads[User]{
    def reads(json: JsValue) = {
      val idOpt = (json \ "id").asOpt[Int]
      val usernameOpt = (json \ "username").asOpt[String]
      val emailOpt = (json \ "email").asOpt[String]
      val imgUrlOpt = (json \ "imgUrl").asOpt[String]
      val optSeq = Seq(idOpt, usernameOpt, emailOpt, imgUrlOpt)
      val validated = optSeq.forall(_.isDefined)

      if(validated){
        val user = new User()
        user.setId(idOpt.get)
        user.setName(usernameOpt.get)
        user.setEmail(emailOpt.get)
        user.setAvatarUrl(imgUrlOpt.get)
        new JsSuccess(user)
      }else
        JsError(s"It failed to parse json to User object. ${optSeq.zipWithIndex.filter(_._1.isEmpty).mkString}")

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
      "id" -> user.getId,
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
      "id" -> user.getId,
      "username" -> user.getName,
      "firstName" -> "",
      "lastName" -> "",
      "expiredDate" -> "",
      "imgUrl" -> user.getAvatarUrl
    )
  }

  implicit val branchToBrowser = new Writes[org.eclipse.egit.github.core.RepositoryBranch] {
    def writes(branch: org.eclipse.egit.github.core.RepositoryBranch) = Json.obj(
      "name" -> branch.getName,
      "commitSha" -> branch.getCommit.getSha
    )
  }

  implicit val commitWritesToBrowser = new OWrites[org.eclipse.egit.github.core.RepositoryCommit] {
    def writes(repoCommit: org.eclipse.egit.github.core.RepositoryCommit) = {
      val commit = repoCommit.getCommit
      Json.obj(
        "sha" -> repoCommit.getSha,
        "message" -> commit.getMessage,
        "date" -> commit.getCommitter.getDate,
        "committerEmail" -> commit.getCommitter.getEmail,
        "committerName" -> commit.getCommitter.getName,
        "url" -> commit.getUrl,
        "treeSha" -> commit.getTree.getSha
      )
    }
  }


  implicit val typoStatsWritesToBrowser = new Writes[Tuple2[TypoStat, org.eclipse.egit.github.core.RepositoryCommit]] {
    def writes(tuple: Tuple2[TypoStat, org.eclipse.egit.github.core.RepositoryCommit]) = {
      val typoStat = tuple._1
      val commit = tuple._2
      (Json.toJsObject(commit)(commitWritesToBrowser)) ++ Json.obj(
        "status" -> typoStat.status,
        "startTime" -> typoStat.startTime,
        "completetime" -> typoStat.completeTime,
        "message" -> typoStat.message,
        "id" -> typoStat.id
      )
    }
  }


  implicit val treeEntryWritesToBrowser = new Writes[io.github.qwefgh90.repogarden.bp.github.Implicits.TreeEntryEx] {
    def writes(entry: io.github.qwefgh90.repogarden.bp.github.Implicits.TreeEntryEx) = {
      Json.obj(
        "seq" -> entry.seq,
        "level" -> entry.level,
        "name" -> entry.name,
        "path" -> entry.entry.getPath,
        "sha" -> entry.entry.getSha,
        "url" -> entry.entry.getUrl,
        "type" -> entry.entry.getType
      )
    }
  }

  implicit val treeExWritesToBrowser = new Writes[io.github.qwefgh90.repogarden.bp.github.Implicits.TreeEx] {
    def writes(tree: io.github.qwefgh90.repogarden.bp.github.Implicits.TreeEx) = {
      Json.arr(tree.list)
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

  implicit val typoToBrowser = new OWrites[Typo] {
    def writes(typo: Typo) = {
      Json.obj(
        "id" -> typo.id,
        "treeSha" -> typo.treeSha,
        "path" -> typo.path
      )
    }
  }

  implicit val typoComponentToBrowser = new OWrites[TypoComponent] {
    def writes(comp: TypoComponent) = {
      Json.obj(
        "id" -> comp.id,
        "from" -> comp.from,
        "to" -> comp.to,
        "suggestedList" -> Json.parse(comp.suggestedList),
        "disabled" -> comp.disabled
      )
    }
  }

  implicit val typoAndComponentsToBrowser = new Writes[Tuple3[Typo, Seq[TypoComponent], String]] {
    def writes(tuple: Tuple3[Typo, Seq[TypoComponent], String]) = {
      val typo = tuple._1
      val components = tuple._2
      val body = tuple._3
      Json.toJsObject(typo)(typoToBrowser) ++ Json.obj("offsetTuple" -> Json.toJson(components)(seq(typoComponentToBrowser)), "body" -> body)
    }
  }
  //SpellCheck
  implicit val spellCheckResultFormat: Format[SpellCheckResult] = (
    (JsPath \ "sentence").format[String] and
      (JsPath \ "positionList").format[List[TypoPosition]]
  )(SpellCheckResult.apply, unlift(SpellCheckResult.unapply))

  implicit val typoPositionFormat: Format[TypoPosition] = (
    (JsPath \ "text").format[String] and
      (JsPath \ "offset").format[Int] and
      (JsPath \ "length").format[Int] and
      (JsPath \ "suggestedList").format[List[String]]
  )(TypoPosition.apply, unlift(TypoPosition.unapply))


}
