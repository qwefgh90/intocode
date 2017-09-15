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

  def select(userId: Long, repositoryId: Long) = {
    db.run(Switches.filter(tb => tb.userId === userId && tb.repositoryId === repositoryId).result.headOption)
  }

  def all(): Future[Seq[Switch]] = db.run(Switches.result)

  def insert(switch: Switch): Future[Unit] = db.run(Switches += switch).map { _ => () }

  def updateSwitch(userId: Long, repositoryId: Long, yn: Boolean): Future[Int] = {
    val updateAction = Switches.filter(st => st.userId === userId && st.repositoryId === repositoryId).map(st => (st.yn)).update(yn)
    db.run(updateAction)
  }

  def insertOrUpdate(switch: Switch): Future[Int] = {
    db.run(Switches.insertOrUpdate(switch))
  }

  private class SwitchTable(tag: Tag) extends Table[Switch](tag, "SWITCHES") {
    def userId = column[Long]("USER_ID")
    def repositoryId = column[Long]("REPOSITORY_ID")
    def yn = column[Boolean]("YN")
    def pk = primaryKey("pk_switch_table", (userId, repositoryId))
    def * = (userId, repositoryId, yn) <> (Switch.tupled, Switch.unapply)
  }
}
