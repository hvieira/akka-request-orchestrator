import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

object HttpTest extends App {

  implicit val actorSystem = ActorSystem("my-system")
  // TODO what is this necessary for?
  implicit val actorMaterializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = actorSystem.dispatcher

  // YAHOO provider
  val query = "select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"london\")"
  val baseEndpoint = "https://query.yahooapis.com/v1/public/yql"

  val finalUri: Uri = Uri(baseEndpoint).withQuery(Uri.Query(Map("q" -> query, "format" -> "json")))
  println(finalUri)


  val respFuture = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = finalUri))

  respFuture.flatMap(resp => Unmarshal(resp.entity).to[String])
    .onSuccess {
      case contents: String =>  {
        println(contents.length)
        println(contents)
      }
    }

}
