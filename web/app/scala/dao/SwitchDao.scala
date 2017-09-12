package io.github.qwefgh90.repogarden.web.dao

import scala.concurrent.{ ExecutionContext, Future, Await }
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import play.api.Logger
import slick.jdbc.meta.MTable
import io.github.qwefgh90.repogarden.web.model.Switch
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.dbio._

class SwitchDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Switches = TableQuery[SwitchTable]

  lazy val tables = Await.result(db.run(MTable.getTables), Duration(2, TimeUnit.SECONDS)).toList

  def create(): Future[Any] = {
    if (tables.filter(tb => tb.name.name == Switches.baseTableRow.tableName
    ).length == 0)
      db.run(DBIOAction.seq(Switches.schema.create))
    else
      Future{}
    
  }

  def drop(): Future[Any] = {
    db.run(DBIOAction.seq(Switches.schema.drop))
  }

  def select(userId: String, repositoryId: String) = {
    db.run(Switches.filter(tb => tb.userId === userId && tb.repositoryId === repositoryId).result.headOption)
  }

  def all(): Future[Seq[Switch]] = db.run(Switches.result)

  def insert(switch: Switch): Future[Unit] = db.run(Switches += switch).map { _ => () }

  def updateSwitch(userId: String, repositoryId: String, yn: Boolean) = {
    val updateAction = Switches.filter(st => st.userId === userId && st.repositoryId === repositoryId).map(st => (st.yn)).update(yn)
    db.run(updateAction)
  }

  private class SwitchTable(tag: Tag) extends Table[Switch](tag, "SWITCHES") {
    def userId = column[String]("USER_ID")
    def repositoryId = column[String]("REPOSITORY_ID")
    def yn = column[Boolean]("YN")

    def * = (userId, repositoryId, yn) <> (Switch.tupled, Switch.unapply)
  }
}
