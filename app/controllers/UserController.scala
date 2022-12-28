package controllers

import models.{User, UserDao}
import play.api.libs.json.{JsValue, Json}

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(userDao: UserDao, cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def create = Action.async { request =>
    // parse the request body and validate the data
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody match {
      case Some(json) =>
        val name = (json \ "name").as[String]
        val email = (json \ "email").as[String]

        // save the data to the database using the UserDao
        userDao.create(name, email).map { user =>
          Ok("User created")
        }
      case None =>
        Future.failed(new Exception("Invalid request body"))
    }
  }



  implicit val userWrites: Writes[User] = Json.writes[User]

  def get(id: Long) = Action.async {
    // retrieve the user from the database using the UserDao
    userDao.get(id).map { userOpt =>
      userOpt match {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound("User not found")
      }
    }
  }

 // implicit val userWrites: Writes[User] = Json.writes[User]

  def getAll = Action.async {
    // retrieve all users from the database using the UserDao
    userDao.getAll.map { users =>
      Ok(Json.toJson(users))
    }
  }

  def update(id: Long) = Action.async { request =>
    // parse the request body and validate the data
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody match {
      case Some(json) =>
        val name = (json \ "name").as[String]
        val email = (json \ "email").as[String]

        // update the user in the database using the UserDao
        userDao.update(id, name, email).map { result =>
          if (result > 0) {
            Ok("User updated")
          } else {
            NotFound("User not found")
          }
        }
      case None =>
        Future.failed(new Exception("Invalid request body"))
    }
  }

  def delete(id: Long) = Action.async {
    // delete the user from the database using the UserDao
    userDao.delete(id).map { result =>
      if (result > 0) {
        Ok("User deleted")
      } else {
        NotFound("User not found")
      }
    }
  }

}