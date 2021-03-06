package net.hvieira.orchestrator.service

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.{StatusCodes, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.hvieira.orchestrator.fork.ForkOrchestrator
import net.hvieira.orchestrator.fork.ForkOrchestrator.{ForkFlowRequest, ForkFlowResponse}
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

  private val transactionTimeoutDuration = 3 seconds
  private val forkTimeoutDuration = 3 seconds

  val route =
    pathPrefix("orchestrate") {
      path("transaction") {
        withRequestTimeout(transactionTimeoutDuration) {
          get {
            handleWithTransactionMethod()
          }
        }
      } ~
        path("parallel") {
          withRequestTimeout(forkTimeoutDuration) {
            get {
              handleWithForkMethod()
            }
          }
        }
    }

  def handleWithTransactionMethod(): Route = {

    implicit val timeout = Timeout(transactionTimeoutDuration)

    val actorPerRequest: ActorRef = TransactionOrchestrator.createActor(system)
    val requestFuture = actorPerRequest ? TransactionFlowRequest

    // on completion terminate actor per request and children
    requestFuture.onComplete(result => actorPerRequest ! PoisonPill)(system.dispatcher)

    val requestTimestamp = System.currentTimeMillis()
    onComplete(requestFuture) {

      case Success(TransactionFlowResponse(data)) => {
        log.info(s"Transaction flow took ${System.currentTimeMillis() - requestTimestamp}")
        complete(
          HttpResponse(
            entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), data)))
      }

      case Success(TransactionFlowError) => {
        log.error("Encountered an error during transaction!")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }

      case Failure(e) => {
        log.error(e, "Failure fulfilling request")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }

      case _ => {
        log.warning("Received unexpected result. Ignoring...")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }
    }
  }

  def handleWithForkMethod(): Route = {
    implicit val timeout = Timeout(forkTimeoutDuration)

    val actorPerRequest: ActorRef = ForkOrchestrator.createActor(system)
    val requestFuture = actorPerRequest ? ForkFlowRequest

    val requestTimestamp = System.currentTimeMillis()

    // on completion terminate actor per request and children
    requestFuture.onComplete(result => actorPerRequest ! PoisonPill)(system.dispatcher)

    onComplete(requestFuture) {

      case Success(ForkFlowResponse(result)) => {
        log.info(s"Parallel flow took ${System.currentTimeMillis() - requestTimestamp}")
        complete(
          HttpResponse(
            entity = HttpEntity(ContentType(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`), result)))
      }

      case Failure(e) => {
        log.error(e, "Failure fulfilling request")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }

      case _ => {
        log.warning("Received unexpected result. Ignoring...")
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }
    }
  }


}
