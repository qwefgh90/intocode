package io.github.qwefgh90.repogarden.web.service

import play.api.cache._
import javax.inject._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.web.model.State._
import javax.inject._
import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import org.eclipse.egit.github.core.client.GitHubClient
import scala.collection.JavaConverters._
import io.github.qwefgh90.repogarden.web.model._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import scala.concurrent.duration._
import io.github.qwefgh90.repogarden.web.dao._
import scala.concurrent._

@Singleton
class TypoService @Inject() (typoDao: TypoDao)(implicit executionContext: ExecutionContext) {
  def restart() = {
    
  }
  def getProgress(ownerId: String, repositoryId: String, branchName: String): Future[Option[Long]] = {
    typoDao.selectLastTypoStat(ownerId, repositoryId, branchName).map(typoStatOpt => {
      if(typoStatOpt.map(typoStat => State.withName(typoStat.state) == State.PROGRESS).getOrElse(false))
        typoStatOpt.map(_.id.get)
      else
        None
    })
  }
}
