package net.hvieira.searchprovider

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import net.hvieira.actor.TimeoutableState
import net.hvieira.searchprovider.SearchEngineMainPageProvider._
import net.hvieira.searchprovider.SearchEngineMainPageProvider.SearchProvider.SearchProvider

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object SearchEngineMainPageProvider {

  private val props = Props[SearchEngineMainPageProvider]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)
  def createChildActor(context: ActorContext) = context.actorOf(props)

  private val LOCATION_HEADER = "Location"

  // TODO perhaps make SearchProvider package protected and move it to another file
  object SearchProvider extends Enumeration {
    type SearchProvider = Value
    val GOOGLE = Value(1)
    val DUCKDUCKGO = Value(2)
    val YAHOO = Value(3)

    def fromInt(param: Int) = {
      SearchProvider.apply(param)
    }

    def url(param: SearchProvider) = param match {
      case GOOGLE => "https://google.com"
      case DUCKDUCKGO => "https://duckduckgo.com"
      case YAHOO => "https://yahoo.com"
    }
  }

  case class SearchEngineMainPageRequest(val providerId: Int)
  case class SearchEngineMainPageResponse(val html: Try[String])
  case class SearchEngineMainPageError()


}

class SearchEngineMainPageProvider extends Actor with TimeoutableState {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  def handleRedirection(resp: HttpResponse, headers: Seq[HttpHeader]): Unit = {
    val headerMap = headers.map(header => (header.name(), header.value())).toMap

    resp.discardEntityBytes()

    headerMap.get(LOCATION_HEADER) match {
      case Some(newLocation) => {
        http
          .singleRequest(HttpRequest(method = HttpMethods.GET, uri = newLocation))
          .pipeTo(self)
      }
        // TODO this is not great. I dont want exceptions in code. Better use Try, Either or something like that or reply to sender from here to communicate error
      case None => throw new RuntimeException("Redirection failed because there is no location header in resp")
    }
  }

  def handleHtmlResponse(originalSender: ActorRef): State = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) => {
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        originalSender ! SearchEngineMainPageResponse(Success(body.utf8String.trim))
      }
    }

    case resp@HttpResponse(StatusCodes.MovedPermanently, headers, _, _) => handleRedirection(resp, headers)
    case resp@HttpResponse(StatusCodes.Found, headers, _, _) => handleRedirection(resp, headers)

    case resp@HttpResponse(code, _, _, _) => {
      originalSender ! SearchEngineMainPageResponse(Failure(null))
      resp.discardEntityBytes()
    }
  }

  def processRequest(value: SearchProvider): Unit = {
    val finalUri: Uri = Uri(SearchProvider.url(value))

    http
      .singleRequest(HttpRequest(method = HttpMethods.GET, uri = finalUri))
      .pipeTo(self)

    assumeStateWithTimeout(1 seconds,
      handleHtmlResponse(sender),
      () => {
        sender ! SearchEngineMainPageError
      })
  }

  override def aroundPostStop(): Unit = {
    // TODO use logs
    println(s"Stopping Actor $self")
    postStop()
  }

  override def receive: Receive = {
    case SearchEngineMainPageRequest(id) => processRequest(SearchProvider.fromInt(id))
  }
}