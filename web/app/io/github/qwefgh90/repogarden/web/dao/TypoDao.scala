package io.github.qwefgh90.repogarden.web.dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import io.github.qwefgh90.repogarden.web.model.TypoStat
import io.github.qwefgh90.repogarden.web.model.{Typo, TypoComponent}
import io.github.qwefgh90.repogarden.web.model.TypoStatus._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.dbio._

class TypoDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  
  def create(): Future[Any] = {
    db.run(DBIOAction.seq(typoStats.schema.create, typos.schema.create, typoComponents.schema.create))
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

  def selectTypoStats(ownerId: Long, repositoryId: Long, userId: Long): Future[Seq[TypoStat]] = {
    db.run(typoStats.filter(stat => stat.ownerId === ownerId && stat.repositoryId === repositoryId && stat.userId === userId).result)
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

  def insertTypo(typo: Typo): Future[Long] = {
    db.run((typos returning typos.map(_.id)) += typo)
  }

  def insertTypoAction(typo: Typo) = {
    (typos returning typos.map(_.id)) += typo
  }

  def insertTypos(typoList: Seq[Typo]): Future[Option[Int]] = {
    db.run(typos ++= typoList)
  }

  def insertTypoAndDetailList(typoList: Seq[(Typo, List[TypoComponent])]): Future[List[Long]] = {
    typoList.foldLeft(Future{List[Long]()}){
      (acc, tuple) => {
        val typo = tuple._1
        val components = tuple._2
        acc.flatMap{ list =>
          db.run(insertTypoAction(typo)
            .flatMap{parentId => insertTypoComponentsAction(parentId, components).map{num => parentId :: list};}
            .transactionally
          )
        }
      }
    }
  }

  def insertTypoComponents(parentId: Long, list: List[TypoComponent]): Future[Option[Int]] = {
    db.run(typoComponents ++= list.map{_.copy(parentId = Some(parentId))})
  }

  def insertTypoComponentsAction(parentId: Long, list: List[TypoComponent]) = {
    typoComponents ++= list.map{_.copy(parentId = Some(parentId))}
  }

  def selectTypoComponentByParentId(parentId: Long): Future[Seq[TypoComponent]] = {
    db.run(typoComponents.filter(_.parentId === parentId).result)
  }

  def selectTypoList(parentId: Long): Future[Seq[Typo]] = {
    db.run(typos.filter(_.parentId === parentId).result)
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
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Long]("parent_id")
    def path = column[String]("path")
    def treeSha = column[String]("tree_sha")
    def issueCount = column[Int]("issue_count")
    def highlight = column[String]("highlight")
    
    def typoStat = foreignKey("PARENT_ID_FK", parentId, typoStats)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id.?, parentId, path, treeSha, issueCount, highlight) <> (Typo.tupled, Typo.unapply)
  }
  private val typos = TableQuery[TypoTable]

  private class TypoComponentTable(tag: Tag) extends Table[TypoComponent](tag, "TYPOCOMPONENT"){
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Option[Long]]("parent_id")
    def path = column[String]("path")
    def from = column[Int]("from")
    def to = column[Int]("to")
    def endLine = column[Int]("end_line")
    def columnNum = column[Int]("column_num")
    def suggestedList = column[String]("suggested_list")
    def disabled = column[Boolean]("disabled")

    def typo = foreignKey("PARENT_ID_FK2", parentId, typos)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id.?, parentId, path, from, to, endLine, columnNum, suggestedList, disabled) <> (TypoComponent.tupled, TypoComponent.unapply)
  }
  private val typoComponents = TableQuery[TypoComponentTable]
}
