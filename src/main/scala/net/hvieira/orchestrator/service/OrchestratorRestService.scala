package net.hvieira.orchestrator.service

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.{StatusCodes, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.hvieira.orchestrator.transactional.TransactionOrchestrator
import net.hvieira.orchestrator.transactional.TransactionOrchestrator.{TransactionFlowError, TransactionFlowRequest, TransactionFlowResponse}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object OrchestratorRestService {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class OrchestratorRestService(implicit val system: ActorSystem,
                              implicit val materializer: ActorMaterializer) {

  private val log = Logging(system, this)

  private val transactionTimeoutDuration = 5 seconds

  val route =
    pathPrefix("orchestrate") {
      path("transaction") {
        withRequestTimeout(5 seconds) {
          get {
            handleWithTransactionMethod()
          }
        }
      } ~
        path("fork") {
          get {
            handleWithForkMethod()
          }
        }
    }

  def handleWithTransactionMethod(): Route = {

    implicit val timeout = Timeout(transactionTimeoutDuration)

    val actorPerRequest: ActorRef = TransactionOrchestrator.createActor(system)
    val requestFuture = actorPerRequest ? TransactionFlowRequest

    // on completion terminate actor per request and children
    requestFuture.onComplete(result => actorPerRequest ! PoisonPill)(system.dispatcher)

    onComplete(requestFuture) {

      case Success(TransactionFlowResponse(data)) => complete(
        HttpResponse(
          entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), data)))

      case Success(TransactionFlowError) => {
        log.error("Encountered an error during transaction!")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }

      case Failure(e) => {
        log.error("Failure fulfilling request", e)
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }

      case _ => {
        log.error("Unexpected message type. Returning error to the client")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }
    }
  }

  def handleWithForkMethod(): Route = ???


}
