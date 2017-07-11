package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by adelegue on 11/07/2017.
  */
object ForComprehension extends App {

  val option1 = Some("1")
  val option2 = Some(42)
  val option3 = Some(true)

  val res: Option[(String, Int, Boolean)] =
    option1.flatMap { v1 =>
    option2
      .filter(_ => v1 == "1")
      .flatMap { v2 =>
    option3.map { v3 =>
      (v1, v2, v3)
    }
    }
  }


  val res2: Option[(String, Int, Boolean)] =
    for {
      v1 <- option1
      v2 <- option2 if v1 == "1"
      v3 <- option3
    } yield (v1, v2, v3)

  val call1 = Future.successful(Some("1"))
  val call2 = Future.successful(1)
  val call3 = Future.successful(true)

  val callN: Future[(Option[String], Int, Boolean)] =
    for {
      mayBeV1 <- call1
      v2 <- call2 if mayBeV1 == "1"
      v3 <- call3
    } yield (mayBeV1, v2, v3)


}
