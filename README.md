# Building a REST API in Scala with Play Framework using MySQL and Slick

For building simple, CRUD-style REST APIs in Scala, the Play Framework is a good solution.
It has an uncomplicated API that doesn’t require us to write too much code.

In this project we will build a CRUD application to Create, Read, Update and Delete. We make use of MySQL database to store the data,
The application will provide several endpoints, to perform CRUD operations

<h2>To implement a REST API using the Play Framework with Scala, you can follow these steps: </h2>

* Create a new Play project using the Scala version of the Play Framework. You can do this using the sbt new command and specifying the Play Scala seed template in terminal.
    <pre>sbt new playframework/play-scala-seed.g8</pre>

* Once after the template is generated import that project in your IDE.

* Add Slick and MySQL dependency in your build.sbt file and update the project 
    <pre>libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.1"
  libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.30"</pre>

* Add few configuration lines in application.conf file under conf directory 
    <pre>slick.dbs.default.driver = "slick.driver.MySQLDriver$" 
  slick.dbs.default.db.driver = "com.mysql.cj.jdbc.Driver"
  slick.dbs.default.db.url = "jdbc:mysql://localhost/playdb"
  slick.dbs.default.db.user = "root"
  slick.dbs.default.db.password = "wisdom@2022"</pre>

* Next we will be adding 3 files 
  * **routes** : Which holds the end-points for our application [*saved under conf directory as routes*]
  * **UserController** : It is a controller layer, where each public, static, method is an action. 
                        An action is a Scala entry point invoked when a HTTP Request is received.
                        [*saved under controllers directory as UserController*]
  * **UserDao** : Whenever model objects need to be saved into a persistent storage, 
                  they may contain some glue artifacts like JPA annotations or SQL statements.
                  [*create new directory called models and add a file as UserDao*]

<h3>routes</h3>
<pre># To add new user to Database
POST        /users                 controllers.UserController.create

# To get a user by id from database
GET         /users/:id             controllers.UserController.get(id: Long)

# To get all users from database
GET         /users                 controllers.UserController.getAll

# To update existing users in database
PUT         /users/:id             controllers.UserController.update(id: Long)

# To delete user from Database
DELETE      /users/:id             controllers.UserController.delete(id: Long)</pre>

<h3>UserController</h3>
<pre>package controllers

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

}</pre>

<h3>UserDao</h3>
<pre>package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class User(id: Long, name: String, email: String)

class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def * = (id, name, email) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[UserTable]

  def create(name: String, email: String): Future[User] = {
    val insertQuery = users returning users.map(_.id) into ((user, id) => user.copy(id = id))
    val action = insertQuery += User(0, name, email)
    db.run(action)
  }

  def get(id: Long): Future[Option[User]] = {
    db.run(users.filter(_.id === id).result.headOption)
  }

  def getAll: Future[Seq[User]] = {
    db.run(users.result)
  }

  def update(id: Long, name: String, email: String): Future[Int] = {
    db.run(users.filter(_.id === id).map(u => (u.name, u.email)).update((name, email)))
  }

  def delete(id: Long): Future[Int] = {
    db.run(users.filter(_.id === id).delete)
  }

}</pre>

* Start the server and test your end points 
  <pre>sbt run</pre>

------------------------------------------------------

Slick (or in full Scala Language-integrated Connection Kit) is a Functional Relational Mapping library for Scala that allows us to query and access a database like other Scala collections. 
We can write database queries in Scala instead of SQL, thus providing typesafe queries.

MySQL is an open-source relational database management system. As with other relational databases, MySQL stores data in tables made up of rows and columns. 
Users can define, manipulate, control, and query data using Structured Query Language, more commonly known as SQL.



>>	Requirements
*	[Java]
*	[IDE] IntelliJ IDEA
*	[SBT] can install using https://www.scala-sbt.org/download.html

    SBT is an open-source build tool for Scala and Java projects, similar to Apache's Maven and Gradle.
    
    Its main features are:
    * Native support for compiling Scala code and integrating with many Scala test frameworks
    * Continuous compilation, testing, and deployment
    * Incremental testing and compilation, meaning only changed sources are re-compiled, only affected tests are re-run
    * Build descriptions written in Scala using a DSL
    * Dependency management using Coursier, which supports Maven-format repositories
    * Integration with the Scala REPL for rapid iteration and debugging
    * Support for mixed Scala/Java projects


*   [Giter8]


    Giter8 is a command line tool to generate files and directories from templates published on GitHub or any other git repository. 
    It’s implemented in Scala and runs through the sbt launcher, but it can produce output for any purpose.
    Choose from community-maintained Giter8 templates to jump start your project:
    $ sbt new scala/scala-seed.g8
    $ sbt new playframework/play-scala-seed.g8
    $ sbt new akka/akka-http-quickstart-scala.g8
    $ sbt new http4s/http4s.g8
    $ sbt new holdenk/sparkProjectTemplate.g8	

>	Once the project template is downloaded we can start the project server by going into the project directory
*	sbt update
*	sbt run
>   Our project server is live in localhost:9000 port
		