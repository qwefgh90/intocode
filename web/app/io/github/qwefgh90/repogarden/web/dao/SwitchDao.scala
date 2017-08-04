package io.github.qwefgh90.repogarden.web.dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import io.github.qwefgh90.repogarden.web.model.Switch
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.dbio._

class SwitchDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Switches = TableQuery[SwitchTable]

  def create(): Future[Any] = {
    db.run(DBIOAction.seq(Switches.schema.create))
  }

  def drop(): Future[Any] = {
    db.run(DBIOAction.seq(Switches.schema.drop))
  }

  def select(userId: String, repositoryId: String) = {
    db.run(Switches.filter(tb => tb.userId === userId && tb.repositoryId === repositoryId).result.head)
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
