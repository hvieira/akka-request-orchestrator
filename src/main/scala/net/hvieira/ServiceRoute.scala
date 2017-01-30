package net.hvieira

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import net.hvieira.orchestrator.transactional.TransactionOrchestrator
import net.hvieira.orchestrator.transactional.TransactionOrchestrator.{TransactionFlowError, TransactionFlowRequest, TransactionFlowResponse}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ServiceRoute {

  private implicit val timeout = Timeout(5 seconds)

  // TODO check if using an inbox would offer better performance and no blocking
  def handleWithTransactionMethod(actorSystem: ActorSystem): Route = {
    val actorPerRequest: ActorRef = TransactionOrchestrator.createActor(actorSystem)
    val requestFuture = actorPerRequest ? TransactionFlowRequest

    // on completion terminate actor per request and children
    requestFuture.onComplete(result => actorPerRequest ! PoisonPill)(actorSystem.dispatcher)

    onComplete(requestFuture) {
      case Success(TransactionFlowResponse(data)) => complete(
        HttpResponse(
          entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), data)))
      case Success(TransactionFlowError) => complete("Transaction Flow failed with an error!")
      case Failure(e) => {
        // TODO use logs instead
        e.printStackTrace()
        complete(HttpResponse(status = StatusCodes.InternalServerError))
      }
      case _ => complete(HttpResponse(status = StatusCodes.InternalServerError))
    }
  }

  def handleWithForkMethod(actorSystem: ActorSystem): Route = ???

  def createRoute(actorSystem: ActorSystem): Route = {

    pathPrefix("orchestrate") {
      path("transaction") {
        get {
          handleWithTransactionMethod(actorSystem)
        }
      } ~
        path("fork") {
          get {
            handleWithForkMethod(actorSystem)
          }
        }
    }

  }

}
