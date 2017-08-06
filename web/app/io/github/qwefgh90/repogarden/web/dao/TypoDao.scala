

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
    
    def typoStat = foreignKey("SUP_FK", parentId, typoStats)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (parentId, path, treeSha, issueCount, spellCheckResult, highlight) <> (Typo.tupled, Typo.unapply)
  }
  private val typos = TableQuery[TypoTable]

}
