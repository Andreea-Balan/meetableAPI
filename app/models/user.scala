package models

case class GPS(id: String,
               longitude: Float,
               latitude: Float)

object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val gpsFormat = Json.format[GPS]
}