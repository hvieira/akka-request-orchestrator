package net.hvieira.orchestrator.transactional

import akka.actor.{Actor, ActorSystem, Props}
import net.hvieira.weather.{WeatherData, WeatherRequest, WeatherResponse}

object TransactionOrchestrator {

  private val props = Props[TransactionOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

}

class TransactionOrchestrator extends Actor {

  def handleWeatherRequest: Unit = {
    // TODO spawn new weather provider actors and get the data sequentially from them
    sender ! new WeatherResponse(new WeatherData("DUMMY -> TBD!!!"))
  }

  override def receive: Receive = {
    case WeatherRequest => {
      handleWeatherRequest
    }
  }
}
