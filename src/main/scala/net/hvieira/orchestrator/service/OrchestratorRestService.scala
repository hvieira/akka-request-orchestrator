package net.hvieira.orchestrator.service

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.{StatusCodes, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import net.hvieira.orchestrator.transactional.TransactionOrchestrator
import net.hvieira.orchestrator.transactional.TransactionOrchestrator.{TransactionFlowError, TransactionFlowRequest, TransactionFlowResponse}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.util.{Failure, Success}

// TODO would this make more sense as an object?
class OrchestratorRestService(implicit val system: ActorSystem,
                              implicit val materializer: ActorMaterializer) {

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

  // TODO check if using an inbox would offer better performance and no blocking
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

      case Success(TransactionFlowError) => complete(HttpResponse(status = StatusCodes.InternalServerError))

      case Failure(e) => {
        // TODO use logs instead
        e.printStackTrace()
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }

      case _ => {
        // TODO at least log something
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }
    }
  }

  def handleWithForkMethod(): Route = ???


}
