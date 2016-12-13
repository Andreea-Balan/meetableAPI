package controllers

import javax.inject.Inject
import scala.concurrent.Future
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.api.Cursor
import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
MongoController,
ReactiveMongoApi,
ReactiveMongoComponents
}

// BSON-JSON conversions/collection
import reactivemongo.play.json._

class GPSController @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  def collection: reactivemongo.play.json.collection.JSONCollection = db.collection[reactivemongo.play.json.collection.JSONCollection]("gpscoord")

  import models.JsonFormats._

  def store = Action.async(parse.json) { request =>
    request.body.validate[models.GPS].map {
      coord =>
        val json = Json.obj(
          "id" -> coord.id,
          "latitude" -> coord.latitude,
          "longitude" -> coord.longitude)
        collection.insert(json).map { lastError =>
          Logger.debug(s"Successfully inserted with LastError: $lastError")
          Created(s"User Created")
        }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findGPS(lat: String, lon: String, dist: String) = Action.async {
    var latitude = lat.toFloat;
    var longitude = lon.toFloat;
    var distance = (dist.toFloat) * (1.0 / 110574.0);
    // let's do our query
    val cursor: Cursor[JsObject] = collection.
      // find all people with namByNamee `name`db.collection.find( { field: { $gt: value1, $lt: value2 } } );
      find(Json.obj("latitude" -> Json.obj("$gt" -> (latitude - distance), "$lt" -> (latitude + distance)),
      "longitude" -> Json.obj("$gt" -> (longitude - distance), "$lt" -> (longitude + distance)))).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[JsObject]

    // gather all the JsObjects in a list
    val futurePersonsList: Future[List[JsObject]] = cursor.collect[List]()

    // transform the list into a JsArray
    val futurePersonsJsonArray: Future[JsArray] =
    futurePersonsList.map { persons => Json.arr(persons) }

    // everything's ok! Let's reply with the array
    futurePersonsJsonArray.map { persons =>
      Ok(persons)
    }
  }
}