package net.hvieira

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import net.hvieira.orchestrator.transactional.TransactionOrchestrator
import net.hvieira.weather.{WeatherRequest, WeatherResponse}

import scala.concurrent.duration._

object ServiceRoute {

  private implicit val timeout = Timeout(2 seconds)

  def handleWithTransactionMethod(actorSystem: ActorSystem): Route = {
    val requestFuture = TransactionOrchestrator.createActor(actorSystem) ? WeatherRequest
    onSuccess(requestFuture) {
      case WeatherResponse(data) => complete(data.dummyValue)
    }
  }

  def handleWithForkMethod(actorSystem: ActorSystem): Route = ???

  def createRoute(actorSystem: ActorSystem): Route = {

    pathPrefix("weather") {
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
