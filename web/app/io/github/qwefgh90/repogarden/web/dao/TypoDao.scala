package io.github.qwefgh90.repogarden.web.dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import io.github.qwefgh90.repogarden.web.model.TypoStat
import io.github.qwefgh90.repogarden.web.model.Typo
import io.github.qwefgh90.repogarden.web.model.TypoStatus._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.dbio._

class TypoDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  
  def create(): Future[Any] = {
    db.run(DBIOAction.seq(typoStats.schema.create, typos.schema.create))
  }

  def insertTypoStat(typoStat: TypoStat): Future[Long] = {
    db.run((typoStats returning typoStats.map(_.id)) += typoStat)
  }

  def selectTypoStat(id: Long): Future[Option[TypoStat]] = {
    db.run(typoStats.filter(_.id === id).result.headOption)
  }

  def selectTypoStat(ownerId: Long, repositoryId: Long, commitSha: String): Future[Option[TypoStat]] = {
    db.run(typoStats.filter(stat => stat.ownerId === ownerId && stat.repositoryId === repositoryId && stat.commitSha === commitSha).result.headOption)
  }

  def selectLastTypoStat(ownerId: Long, repositoryId: Long, branchName: String): Future[Option[TypoStat]] = {
    db.run(typoStats.filter(stat => stat.ownerId === ownerId && stat.repositoryId === repositoryId && stat.branchName === branchName).sortBy(_.startTime.desc.nullsFirst).take(1).result.headOption)
  }

  def deleteTypoStat(id: Long): Future[Int] = {
    db.run(typoStats.filter(_.id === id).delete)
  }

  def updateTypoStat(id: Long, status: TypoStatus, message: String): Future[Int] = {
    db.run(typoStats.filter(_.id === id).map(tb => (tb.status, tb.message)).update((status.toString, message)))
  }

  def insertTypo(typo: Typo): Future[Int] = {
    db.run(typos += typo)
  }

  def insertTypos(typoList: Seq[Typo]): Future[Option[Int]] = {
    db.run(typos ++= typoList)
  }

  def selectTypoList(id: Long): Future[Seq[Typo]] = {
    db.run(typos.filter(_.parentId === id).result)
  }

  def selectTypoList(ownerId: Long, repositoryId: Long, commitSha: String): Future[Seq[Typo]] = {
    this.selectTypoStat(ownerId, repositoryId, commitSha).map(stat => stat.get.id.get).flatMap(
      id => this.selectTypoList(id))
  }

  private class TypoStatTable(tag: Tag) extends Table[TypoStat](tag, "TYPOSTAT") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ownerId = column[Long]("owner_id")
    def repositoryId = column[Long]("repository_id")
    def branchName = column[String]("branch_name")
    def commitSha = column[String]("commit_sha")
    def startTime = column[Option[Long]]("start_time")
    def completeTime = column[Option[Long]]("complete_time")
    def status = column[String]("status")
    def message = column[String]("message")
    def userId = column[Long]("user_id")
    def * = (id.?, ownerId, repositoryId, branchName, commitSha, startTime, completeTime, message, status, userId) <> (TypoStat.tupled, TypoStat.unapply)
  }
  private val typoStats = TableQuery[TypoStatTable]

  private class TypoTable(tag: Tag) extends Table[Typo](tag, "TYPO"){
    def parentId = column[Long]("parent_id")
    def path = column[String]("path")
    def treeSha = column[String]("tree_sha")
    def issueCount = column[Int]("issue_count")
    def components = column[String]("components")
    def highlight = column[String]("highlight")
    
    def typoStat = foreignKey("PARENT_ID_FK", parentId, typoStats)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (parentId, path, treeSha, issueCount, components, highlight) <> (Typo.tupled, Typo.unapply)
  }
  private val typos = TableQuery[TypoTable]
}
