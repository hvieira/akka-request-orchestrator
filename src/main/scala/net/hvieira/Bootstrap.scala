package net.hvieira

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import net.hvieira.http.HttpServer
import net.hvieira.orchestrator.service.OrchestratorRestService

object Bootstrap extends App {

  implicit val actorSystem = ActorSystem("orchestrator-service-actor-system")
  // TODO what is this necessary for?
  implicit val actorMaterializer = ActorMaterializer()

  // TODO read port from args and have a default 9000
  new HttpServer().startServer("localhost", 9000, new OrchestratorRestService().route)
}
