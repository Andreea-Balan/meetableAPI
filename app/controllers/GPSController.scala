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


}