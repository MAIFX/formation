package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.stream.Materializer
import akka.util.Timeout
import controllers.PrintStats.Print
import controllers.StatsActor._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class Stats(creations: Long = 0, updates: Long = 0, deletes: Long = 0, reads: Long = 0)

object Stats {
  implicit val format = Json.format[Stats]
}

@Singleton
class StatsService @Inject() ()(implicit actorSystem: ActorSystem) {

  import akka.pattern._

  private implicit val timeout = Timeout(1.second)
  private val ref = actorSystem.actorOf(StatsActor.props())

  def getStats(): Future[Stats] = {
    (ref ? GetState).mapTo[Stats]
  }

  def sendEvent(msg: StatsMessages): Unit = {
    ref ! msg
  }

}

object StatsActor {

  sealed trait StatsMessages
  case object UserAdded extends StatsMessages
  case object UserDeleted extends StatsMessages
  case object UserUpdated extends StatsMessages
  case object UserRead extends StatsMessages
  case object GetState
  case object DoPrint

  def props() = Props(classOf[StatsActor])
}


class StatsActor extends Actor {

  import context.dispatcher

  var stats = Stats()
  var printer: Option[ActorRef] = None
  var scheduler: Option[Cancellable] = None

  override def receive: Receive = {
    case UserAdded =>
      stats = stats.copy(creations = stats.creations + 1)
      context.system.eventStream.publish(UserAdded)
    case UserUpdated =>
      stats = stats.copy(updates = stats.updates + 1)
      context.system.eventStream.publish(UserUpdated)
    case UserDeleted =>
      stats = stats.copy(deletes = stats.deletes + 1)
      context.system.eventStream.publish(UserDeleted)
    case UserRead =>
      stats = stats.copy(reads = stats.reads + 1)
      context.system.eventStream.publish(UserRead)
    case GetState =>
      sender() ! stats
    case DoPrint =>
      printer.foreach{_ ! Print(stats) }
      stats = Stats()
  }

  override def preStart(): Unit = {
    val printStats = context.actorOf(PrintStats.props())
    printer = Some(printStats)

    scheduler = Some(context.system.scheduler.schedule(
      3.second,
      3.second,
      self,
      DoPrint
    ))
  }

  override def postStop(): Unit = {
    scheduler.foreach { _.cancel() }
    printer.foreach { ref => ref ! PoisonPill }
  }
}

object PrintStats {
  case class Print(stats: Stats)

  def props() = Props(classOf[PrintStats])
}

class PrintStats extends Actor {

  override def receive: Receive = {
    case Print(stats) => Logger.info(s"New stats: $stats")
  }
}



@Singleton
class StatsController @Inject()(statsService: StatsService)(implicit materializer: Materializer, ec: ExecutionContext, actorSystem: ActorSystem) extends Controller {

  import Stats._

  def getStats() = Action.async {
    statsService.getStats().map{ stats => Ok(Json.toJson(stats))}
  }



}
