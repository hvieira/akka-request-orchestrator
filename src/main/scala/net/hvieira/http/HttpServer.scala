package net.hvieira.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

class HttpServer(
                 implicit val system: ActorSystem,
                 implicit val materializer: ActorMaterializer) {

  def startServer(address: String, port: Int, route: Route) = {

    implicit val executor = system.dispatcher

    val bindingFuture = Http().bindAndHandle(route, address, port)

    bindingFuture.onComplete {
      case Success(Http.ServerBinding(_)) => println(s"Server online at http://localhost:$port")
      case Failure(t) => {
        sys.error(s"Fatal Error! Exiting")
        t.printStackTrace()
        sys.exit()
      }
    }
  }
}
