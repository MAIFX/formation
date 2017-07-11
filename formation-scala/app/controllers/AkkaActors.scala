package controllers

import akka.actor.{Actor, ActorSystem, Props}
import controllers.AkkaActors.MonActeur.SayHello

/**
  * Created by adelegue on 11/07/2017.
  */
object AkkaActors extends App {

  val system = ActorSystem()

  private val monActeur = system.actorOf(MonActeur.props, "MonActeur")

  monActeur ! SayHello


  object MonActeur {
    case object SayHello

    def props = Props(classOf[MonActeur])
  }

  class MonActeur2 extends Actor {
    override def receive: Receive = {
      case "Reply" => sender() ! "OK"
    }
  }

  class MonActeur extends Actor {


    override def preStart(): Unit = {

      val ref = context.actorOf(Props(classOf[MonActeur2]), "enfant")
      println(s"Moi : ${self.path}, child : ${ref.path}")
    }

    override def receive: Receive = {
      case SayHello =>
        val ref = context.actorOf(Props(classOf[MonActeur2]))
        ref ! "Reply"
        println("hello")
      case "OK" =>
        println("OK")
    }
  }



}
