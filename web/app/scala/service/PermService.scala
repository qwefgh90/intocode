package io.github.qwefgh90.repogarden.web.service

import javax.inject.Inject
import play.api.{Logger}
import io.github.qwefgh90.repogarden.web.controllers.UserRequest
import io.github.qwefgh90.repogarden.web.dao.TypoDao
import scala.concurrent.{Future,ExecutionContext}

class PermissionService @Inject()(githubProvider: GithubServiceProvider, typoDao: TypoDao)(implicit ec: ExecutionContext) {
  
  @deprecated("It only check user's repositories")
  def hasPermissionThisRepository[A](owner: String, name: String)(implicit request: UserRequest[A]): Boolean = {
    val github = githubProvider.getInstance(request.token)
    val repoOpt = github.getRepository(owner, name)
    repoOpt.map{repo =>
      github.getAllRepositories.find(e => e.getOwner.getName == repo.getOwner.getName && e.getName == repo.getName).isDefined
    }.getOrElse(false)
  }

  def hasPermissionThisTypoStatAsync[A](typoStatId: Long)(implicit request: UserRequest[A]): Future[Boolean] = {
    typoDao.selectTypoStat(typoStatId).map{typoStatOpt =>
      typoStatOpt.map(_.userId == request.user.getId).getOrElse(false)
    }
  }

  def hasPermissionThisTypoAsync[A](typoId: Long)(implicit request: UserRequest[A]): Future[Boolean] = {
    typoDao.selectTypo(typoId).flatMap{typoOpt =>
      typoOpt.map(typo => hasPermissionThisTypoStatAsync(typo.parentId)).getOrElse(Future{false})
    }
  }

  def hasPermissionThisTypoComponentAsync[A](typoCompId: Long)(implicit request: UserRequest[A]): Future[Boolean] = {
    typoDao.selectTypoComponent(typoCompId).flatMap{typoCompOpt =>
      typoCompOpt.map(typoComp => typoComp.parentId.map{parentIdLong => hasPermissionThisTypoAsync(parentIdLong)}.getOrElse(Future{false})).getOrElse(Future{false})
    }
  }
}
