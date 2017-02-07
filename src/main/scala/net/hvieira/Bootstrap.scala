package net.hvieira

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.hvieira.http.HttpServer
import net.hvieira.orchestrator.service.OrchestratorRestService

object Bootstrap extends App {

  implicit val actorSystem = ActorSystem("orchestrator-service-actor-system")
  implicit val actorMaterializer = ActorMaterializer()

  new HttpServer()
    .start(
      "localhost",
      actorSystem.settings.config.getInt("server.port"),
      new OrchestratorRestService().route)
}
