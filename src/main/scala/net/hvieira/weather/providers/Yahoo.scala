package net.hvieira.weather.providers

import akka.actor.Actor
import net.hvieira.weather.WeatherRequest

class Yahoo extends Actor {

  def handleRequest: Unit = {

    // https://developer.yahoo.com/weather/
    // TODO request data from API and then get relevant data accordingly and reply with them
  }

  override def receive: Receive = {
    case WeatherRequest => handleRequest
  }
}
