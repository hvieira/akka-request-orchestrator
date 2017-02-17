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

    "orchestrate requests in as a transaction" in {

      implicit val explicitTimeout = RouteTestTimeout(3 seconds)

      val request: HttpRequest = Get("/orchestrate/transaction")
      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        handled shouldBe true
        entityAs[String]  should include regex "Google|DuckDuckGo|Yahoo"
      }
    }

    "orchestrate requests in parallel" in {

      implicit val explicitTimeout = RouteTestTimeout(3 seconds)

      val request: HttpRequest = Get("/orchestrate/parallel")
      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        handled shouldBe true
        entityAs[String]  should include regex "<title>Google<\\/title>|<title>DuckDuckGo<\\/title>|<title>Yahoo<\\/title>"
      }
    }
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools()
  }

}
