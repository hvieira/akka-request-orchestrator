package net.hvieira

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import net.hvieira.orchestrator.transactional.{TransactionFlowError, TransactionFlowRequest, TransactionFlowResponse, TransactionOrchestrator}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ServiceRoute {

  private implicit val timeout = Timeout(5 seconds)

  // TODO improve this
  def handleWithTransactionMethod(actorSystem: ActorSystem): Route = {
    val requestFuture = TransactionOrchestrator.createActor(actorSystem) ? TransactionFlowRequest
    onComplete(requestFuture) {
      case Success(TransactionFlowResponse(data)) => complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`text/html`,  HttpCharsets.`UTF-8`), data)))
      case Success(TransactionFlowError) => complete("Transaction Flow failed with an error!")
      case Failure(e) => {
        e.printStackTrace()
        complete(e.toString)
      }
    }
  }

  def handleWithForkMethod(actorSystem: ActorSystem): Route = ???

  def createRoute(actorSystem: ActorSystem): Route = {

    pathPrefix("orchestrate") {
      path("transaction") {
        get {
          handleWithTransactionMethod(actorSystem)
        }
      }~
      path("fork") {
        get {
          handleWithForkMethod(actorSystem)
        }
      }
    }

  }

}
