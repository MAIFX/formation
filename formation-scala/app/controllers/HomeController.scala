package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json._
import reactivemongo.play.json.compat._, json2bson.{ toDocumentReader, toDocumentWriter }

import scala.concurrent.{ExecutionContext, Future}

case class User(name: String, email: String)

case class StoreError(msgs: List[String])

object User {

  val namespace = "mathieu.ancelin"

  implicit val writes = Json.writes[User]
  implicit val reads = Json.reads[User]

  def users()(implicit mongo: ReactiveMongoApi, ec: ExecutionContext): Future[BSONCollection] =
    mongo.database.map(_.collection[BSONCollection]("users-mathieu.ancelin"))

  def addUser(user: User)(implicit mongoApi: ReactiveMongoApi, ec: ExecutionContext): Future[Either[StoreError, User]] = {
    users().flatMap { coll =>
      coll.insert.one(user).map {
        case wr if wr.writeErrors.isEmpty => Right(user)
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

}

@Singleton
class UserController @Inject()(components: ControllerComponents)(implicit mongo: ReactiveMongoApi, ec: ExecutionContext)  extends AbstractController(components) {

  import User._

  mongo.database.map(_.collection[BSONCollection]("users-mathieu.ancelin")).flatMap { usrs =>
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

































