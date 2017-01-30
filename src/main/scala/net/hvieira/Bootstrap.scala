package net.hvieira

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object Bootstrap extends App {

  implicit val actorSystem = ActorSystem("service-actor-system")
  // TODO what is this necessary for?
  implicit val actorMaterializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = actorSystem.dispatcher

  val route = ServiceRoute.createRoute(actorSystem)
  val port = 9000
  val bindingFuture = Http().bindAndHandle(route, "localhost", port)
  bindingFuture.onSuccess {
    case Http.ServerBinding(_) => println(s"Server online at http://localhost:$port")
  }
  bindingFuture.onFailure {
    case t: Throwable => {
      sys.error(s"Fatal Error! Exiting")
      t.printStackTrace()
      sys.exit()
    }
    case _ => {
      sys.error(s"Fatal Error! Server failed for unknown reasons")
      sys.exit()
    }
  }


}
