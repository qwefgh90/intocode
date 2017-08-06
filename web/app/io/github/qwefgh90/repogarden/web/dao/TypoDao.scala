

package io.github.qwefgh90.repogarden.web.dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import io.github.qwefgh90.repogarden.web.model.TypoStat
import io.github.qwefgh90.repogarden.web.model.Typo
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

  def selectTypoStat(repositoryId: String, commitSha: String): Future[Option[TypoStat]] = {
    db.run(typoStats.filter(stat => stat.repositoryId === repositoryId && stat.commitSha === commitSha).result.headOption)
  }

  def deleteTypoStat(id: Long): Future[Int] = {
    db.run(typoStats.filter(_.id === id).delete)
  }

  def insertTypo(typo: Typo): Future[Int] = {
    db.run(typos += typo)
  }

  def selectTypoList(id: Long): Future[Seq[Typo]] = {
    db.run(typos.filter(_.parentId === id).result)
  }

  def selectTypoList(repositoryId: String, commitSha: String): Future[Seq[Typo]] = {
    this.selectTypoStat(repositoryId, commitSha).map(stat => stat.get.id.get).flatMap(
      id => this.selectTypoList(id))
  }

  private class TypoStatTable(tag: Tag) extends Table[TypoStat](tag, "TYPOSTAT") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def repositoryId = column[String]("repositoryId")
    def commitSha = column[String]("commitSha")
    def completeTime = column[Long]("completeTime")
    def message = column[String]("message")
    def * = (id.?, repositoryId, commitSha, completeTime, message) <> (TypoStat.tupled, TypoStat.unapply)
  }

  private val typoStats = TableQuery[TypoStatTable]

  private class TypoTable(tag: Tag) extends Table[Typo](tag, "TYPO"){
    def parentId = column[Long]("parentId")
    def path = column[String]("path")
    def treeSha = column[String]("treeSha")
    def issueCount = column[Int]("issueCount")
    def spellCheckResult = column[String]("spellCheckResult")
    def highlight = column[String]("highlight")
    
    def typoStat = foreignKey("PARENT_ID_FK", parentId, typoStats)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (parentId, path, treeSha, issueCount, spellCheckResult, highlight) <> (Typo.tupled, Typo.unapply)
  }
  private val typos = TableQuery[TypoTable]

}
