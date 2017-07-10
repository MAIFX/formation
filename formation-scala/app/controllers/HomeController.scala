package controllers

import javax.inject._

import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

case class User(name: String, email: String)

case class StoreError(msgs: List[String])

object StoreError {
  def apply(msg: String*): StoreError = StoreError(msg:_*)
}

object User {

  val namespace = "mathieu.ancelin"

  implicit val writes = Json.writes[User]
  implicit val reads = Json.reads[User]

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
        case d: DatabaseException => Left(StoreError(List(d.getMessage())))
      }
    }
  }

  def findAll()(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Seq[User]] = {
    users().flatMap { coll =>
      coll.find(Json.obj())
        .cursor[User]()
        .collect[Seq](0, Cursor.FailOnError[Seq[User]]())
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
class UserController @Inject()()(implicit mongo: ReactiveMongoApi, ec: ExecutionContext) extends Controller {

  import User._

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

































