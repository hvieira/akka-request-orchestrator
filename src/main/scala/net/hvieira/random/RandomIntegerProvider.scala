package net.hvieira.random

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import net.hvieira.actor.TimeoutableState
import net.hvieira.random.RandomIntegerProvider.{RandomIntegerRequest, RandomIntegerResponse}

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object RandomIntegerProvider {
  private val props = Props[RandomIntegerProvider]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)
  def createChildActor(context: ActorContext) = context.actorOf(props)

  case class RandomIntegerRequest(val min: Int, val max: Int)
  case class RandomIntegerResponse(val number: Try[Int])
}

class RandomIntegerProvider
  extends Actor
    with TimeoutableState
    with ActorLogging {

  private val SERVICE_BASE_ENDPOINT = "https://www.random.org/integers/"

  import akka.pattern.pipe
  import context.dispatcher
  import context.become

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  // TODO the request takes a long time. Check if this is something to due with using akka-http
  def waitingForHttpResponse(originalSender: ActorRef): State = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) => {
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        originalSender ! RandomIntegerResponse(Success(Integer.parseInt(body.utf8String.trim)))
      }
    }
    case resp@HttpResponse(code, _, _, _) => {
      originalSender ! RandomIntegerResponse(Failure(null))
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

    become(waitingForHttpResponse(sender))
    assumeStateWithTimeout(2 seconds,
      waitingForHttpResponse(sender),
      () => sender ! RandomIntegerResponse
    )
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