package net.hvieira.random

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import net.hvieira.actor.TimeoutableState
import net.hvieira.random.RandomIntegerProvider.{RandomIntegerError, RandomIntegerRequest, RandomIntegerResponse, SERVICE_BASE_ENDPOINT}

import scala.concurrent.duration._

object RandomIntegerProvider {
  private val SERVICE_BASE_ENDPOINT = "https://www.random.org/integers/"

  private val props = Props[RandomIntegerProvider]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

  def createChildActor(context: ActorContext) = context.actorOf(props)

  case class RandomIntegerRequest(val min: Int, val max: Int)

  case class RandomIntegerResponse(val number: Int)

  case object RandomIntegerError

}

class RandomIntegerProvider
  extends Actor
    with TimeoutableState
    with ActorLogging {


  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  def waitingForHttpResponse(requestTimestamp: Long, originalSender: ActorRef): State = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) => {
      log.info(s"Got random number! Took ${System.currentTimeMillis()-requestTimestamp}")
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        originalSender ! RandomIntegerResponse(Integer.parseInt(body.utf8String.trim))
      }
    }
    case resp@HttpResponse(code, _, _, _) => {
      log.warning(s"Got a response with unexpected status code $code")
      originalSender ! RandomIntegerError
      resp.discardEntityBytes()
    }
  }

  def requestRandomNumber(min: Int, max: Int): Unit = {

    val finalUri: Uri = Uri(SERVICE_BASE_ENDPOINT)
      .withQuery(
        Uri.Query(
          Map(
            "num" -> "1",
            "min" -> s"$min",
            "max" -> s"$max",
            "col" -> "1",
            "base" -> "10",
            "format" -> "plain",
            "rnd" -> "new")))

    http
      .singleRequest(HttpRequest(method = HttpMethods.GET, uri = finalUri))
      .pipeTo(self)

    val originalSender = sender()
    assumeStateWithTimeout(2000 millis,
      waitingForHttpResponse(System.currentTimeMillis(), originalSender),
      () => {
        log.warning("Timeout while getting a random number")
        originalSender ! RandomIntegerError
      })
  }

  override def aroundPostStop(): Unit = {
    log.info("Stopping...")
    postStop()
  }

  override def receive: Receive = {
    case RandomIntegerRequest(min, max) => {
      requestRandomNumber(min, max)
    }
  }
}