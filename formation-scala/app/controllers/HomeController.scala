package controllers

import java.util.concurrent.TimeUnit
import javax.inject._

import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import reactivemongo.akkastream._
import play.api.libs.json._
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object Implicits {
  implicit class BetterString(str: String) {
    def toError[A]: Either[StoreError, A] = Left(StoreError(str))
  }
}

case class User(name: String, email: String)

case class StoreError(msgs: List[String])

object StoreError {
  def apply(msg: String*): StoreError = StoreError(msg:_*)
}

object User {

  import Implicits._

  val namespace = "mathieu.ancelin"

  implicit val writes = Json.writes[User]
  implicit val reads = Json.reads[User]

  val userToCSVByteString = Flow[User]
    .map(u => s"${u.email};${u.name}")
    .intersperse("user email;user name\n", "\n", "\n")
    .map(s => ByteString(s))

  def findAllStream()(
    implicit mongoApi: ReactiveMongoApi,
    materializer: Materializer,
    ec: ExecutionContext): Source[User, _] = {

    Source.fromFuture(users()).flatMapConcat { coll =>
      coll.find(Json.obj())
        .cursor[User]()
        .documentSource()
    }
  }

  def users()(implicit mongo: ReactiveMongoApi, ec: ExecutionContext): Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("users-mathieu.ancelin"))

  def addUser(user: User)(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Either[StoreError, User]] = {
    users().flatMap { coll =>
      coll.insert[User](user).map {
        case wr if wr.ok => Right(user)
        case wr =>
          val errors = wr.writeErrors.map(e => s"${e.code} => ${e.errmsg}").toList
          Left(StoreError(errors))
      } recover {
        case d: DatabaseException => d.getMessage().toError[User]
      }
    }
  }

  def findAll()(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Seq[User]] = {
    users().flatMap { coll =>
      coll.find(Json.obj())
        .cursor[User]()
        .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[User]]())
    }
  }

  def createStreamOld(stream: Source[User, _])(implicit mongoApi: ReactiveMongoApi, materializer: Materializer, ec: ExecutionContext): Source[JsObject, _] = {
    Source.fromFuture(users()).flatMapConcat { coll =>
      stream.mapAsyncUnordered(4) { user =>
        coll.insert(user).map(wr =>
          Json.obj(
            "ok" -> wr.ok,
            "email" -> user.email
          )
        )
      }
    }
  }

  def createStream(stream: Source[User, _])(implicit mongoApi: ReactiveMongoApi, materializer: Materializer, ec: ExecutionContext): Source[JsObject, _] = {
    Source.fromFuture(users()).flatMapConcat { coll =>
      stream
        .mapAsync(20) { user =>
          coll.insert[User](user)
            .map(wr => Json.obj(
              "email" -> user.email,
              "ok" -> wr.ok
            ))
        }
    }
  }

  def findByEmail(email: String)(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Option[User]] = {
    users().flatMap { coll =>
      coll
        .find(Json.obj("email" -> email))
        .one[User]
    }
  }

  def update(email: String, user: User)(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Either[StoreError, User]] = {
    findByEmail(email).flatMap {
      case None => Future.successful(Left(StoreError(s"User not found for email $email")))
      case Some(user) => {
        users().flatMap { coll =>
          coll
            .update[JsObject, User](Json.obj("email" -> email), user)
            .map {
              case wr if wr.ok => Right(user)
              case wr =>
                val errors = wr.writeErrors.map(e => s"${e.code} => ${e.errmsg}").toList
                Left(StoreError(errors))
            } recover {
              case d: DatabaseException => Left(StoreError(d.getMessage()))
            }
        }
      }
    }
  }

  def deleteByEmail(email: String)(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Either[StoreError, User]] = {
    findByEmail(email).flatMap {
      case None => Future.successful(Left(StoreError(s"User not found for email $email")))
      case Some(user) => {
        users().flatMap { coll =>
          coll
            .remove(Json.obj("email" -> email))
            .map {
              case wr if wr.ok => Right(user)
              case wr =>
                val errors = wr.writeErrors.map(e => s"${e.code} => ${e.errmsg}").toList
                Left(StoreError(errors))
            } recover {
              case d: DatabaseException => Left(StoreError(d.getMessage()))
            }
        }
      }
    }
  }
}

@Singleton
class UserController @Inject()()(implicit mongo: ReactiveMongoApi, materializer: Materializer, ec: ExecutionContext) extends Controller {

  import User._
  import Implicits._

  val sourceBodyParser = BodyParser("CSV BodyParser") { _ =>
    Accumulator.source[ByteString].map(Right.apply)
  }

  mongo.database.map(_.collection[JSONCollection]("users-mathieu.ancelin")).flatMap { usrs =>
    usrs.indexesManager.ensure(Index(Seq("email" -> IndexType.Ascending), unique = true))
  }

  def findByEmail(email: String) = Action.async { req =>
    User.findByEmail(email)
      .map { opt =>
        opt.map(u => Ok(Json.toJson(u))).getOrElse(NotFound(Json.obj()))
      }
  }

  def findAll() = Action.async { req =>
    User.findAll().map { users =>
      Ok(JsArray(users.map(u => Json.toJson(u))))
    }
  }

  def findAllStream() = Action {
    val users = User
      .findAllStream()
      .via(User.userToCSVByteString)
    Ok.chunked(users).as("text/csv")
  }

  def createStream() = Action(sourceBodyParser) { req =>
    val source: Source[User, _] = req.body
      .via(Framing.delimiter(ByteString("\n"), 10000, true))
      .map(_.utf8String)
      .drop(1)
      .map(line => line.split(";").toList)
      .collect {
        case email :: name :: ip :: Nil => User(name = name, email = email)
      }

    val finalSource: Source[ByteString, _] = User.createStream(source)
      .map(json => Json.stringify(json))
      .intersperse("[\n  ", ",\n  ", "\n]")
      .map(str => ByteString(str))

    Ok.chunked(finalSource).as("application/json")
  }

  def createUser() = Action.async(parse.json) { req =>
    req.body.validate[User] match {
      case JsSuccess(user, _) =>
        User.addUser(user).map {
          case Right(usr) => Ok(Json.toJson(usr))
          case Left(StoreError(errors)) => InternalServerError(Json.obj(
            "errors" -> JsArray(errors.map(JsString.apply)))
          )
        }
      case JsError(errors) => Future.successful(BadRequest(JsError.toJson(errors)))
    }
  }
}

































