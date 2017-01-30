package net.hvieira

import akka.actor.ActorSystem
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

  def handleWithTransactionMethod(actorSystem: ActorSystem): Route = {
    val requestFuture = TransactionOrchestrator.createActor(actorSystem) ? TransactionFlowRequest
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
