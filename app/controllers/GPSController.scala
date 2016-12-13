package controllers

import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import javax.inject.Singleton
import play.api.mvc._
import play.api.libs.json._

/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 */
@Singleton
class GPSController extends Controller with MongoController {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[GPSController])

  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("users")

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models._
  import models.JsonFormats._

  def store = Action.async(parse.json) {
    request =>
      request.body.validate[GPS].map {
        coordinates =>
        // `user` is an instance of the case class `models.User`
          collection.insert(coordinates).map {
            lastError =>
              logger.debug(s"Successfully inserted with LastError: $lastError")
              Created(s"")
          }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def updateUser(firstName: String, lastName: String) = Action.async(parse.json) {
    request =>
      request.body.validate[GPS].map {
        user =>
          // find our user by first name and last name
          val nameSelector = Json.obj("firstName" -> firstName, "lastName" -> lastName)
          collection.update(nameSelector, user).map {
            lastError =>
              logger.debug(s"Successfully updated with LastError: $lastError")
              Created(s"User Updated")
          }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findUsers = Action.async {
    // let's do our query
    val cursor: Cursor[GPS] = collection.
      // find all
      find(Json.obj("active" -> true)).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[GPS]

    // gather all the JsObjects in a list
    val futureUsersList: Future[List[GPS]] = cursor.collect[List]()

    // transform the list into a JsArray
    val futurePersonsJsonArray: Future[JsArray] = futureUsersList.map { users =>
      Json.arr(users)
    }
    // everything's ok! Let's reply with the array
    futurePersonsJsonArray.map {
      users =>
        Ok(users(0))
    }
  }

}
