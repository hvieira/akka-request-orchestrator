package net.hvieira.orchestrator.service

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._

class OrchestratorRestServiceSpec
  extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest {

  val route = new OrchestratorRestService().route

  "The service" should {

    "Orchestrate requests" in {

      implicit val explicitTimeout = RouteTestTimeout(5 seconds)

      val request: HttpRequest = Get("/orchestrate/transaction")
      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        handled shouldBe true
        entityAs[String]  should include regex "Google|DuckDuckGo|Yahoo"
      }
    }
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools()
  }

}
