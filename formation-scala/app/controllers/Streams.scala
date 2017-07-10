package controllers

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Success

/**
  * Created by mathieuancelin on 10/07/2017.
  */
object Streams extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  import system.dispatcher

  private val source: Source[Int, NotUsed] = Source(0 to 1000)

  private val strings: Source[Seq[String], NotUsed] =
    source.via(flowToString).grouped(10)

  private val response: Future[immutable.Seq[Seq[String]]] =
    strings.runWith(Sink.seq)

  response.onComplete {
    case Success(l) => println(l.mkString("\n"))
    case _ => println("Err")
  }


  val flowToString = Flow[Int].mapAsync(4)(toStringAsync)

  private def toStringAsync(intValue: Int): Future[String] = {
    Future {
      s"String : ${intValue.toString}"
    }
  }

}
