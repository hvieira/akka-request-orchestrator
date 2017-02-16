package net.hvieira.searchprovider

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import net.hvieira.actor.TimeoutableState
import net.hvieira.searchprovider.SearchEngineMainPageProvider._
import net.hvieira.searchprovider.SearchProvider.SearchProvider

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object SearchEngineMainPageProvider {

  private val LOCATION_HEADER = "Location"

  private val props = Props[SearchEngineMainPageProvider]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

  def createChildActor(context: ActorContext) = context.actorOf(props)

  case class SearchEngineMainPageRequest(val providerId: Int)

  case class SearchEngineMainPageResponse(val html: String)

  case object SearchEngineMainPageError

}

class SearchEngineMainPageProvider
  extends Actor
    with TimeoutableState
    with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  def handleRedirection(resp: HttpResponse, headers: Seq[HttpHeader]): Unit = {
    val headerMap = headers.map(header => (header.name(), header.value())).toMap

    resp.discardEntityBytes()

    headerMap.get(LOCATION_HEADER) match {
      case Some(newLocation) => http
        .singleRequest(HttpRequest(method = HttpMethods.GET, uri = newLocation))
        .pipeTo(self)

      case None => Future.successful(HttpResponse(StatusCodes.BadGateway)).pipeTo(self)
    }
  }

  def handleHtmlResponse(originalSender: ActorRef): State = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) => {
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        originalSender ! SearchEngineMainPageResponse(body.utf8String.trim)
      }
    }

    case resp@HttpResponse(StatusCodes.MovedPermanently, headers, _, _) => handleRedirection(resp, headers)
    case resp@HttpResponse(StatusCodes.Found, headers, _, _) => handleRedirection(resp, headers)

    case resp@HttpResponse(code, _, _, _) => {
      log.error("Did not get successful response from page request")
      originalSender ! SearchEngineMainPageError
      resp.discardEntityBytes()
    }
  }

  def processRequest(value: SearchProvider): Unit = {
    log.info(s"Retrieving page for provider $value")
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
    log.info("Stopping...")
    postStop()
  }

  override def receive: Receive = {
    case SearchEngineMainPageRequest(id) => processRequest(SearchProvider.fromInt(id))
  }
}