package net.hvieira.orchestrator.fork

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import net.hvieira.actor.TimeoutableState
import net.hvieira.orchestrator.fork.ForkOrchestrator.{ForkFlowError, ForkFlowRequest, ForkFlowResponse}
import net.hvieira.searchprovider.SearchEngineMainPageProvider
import net.hvieira.searchprovider.SearchEngineMainPageProvider.{SearchEngineMainPageError, SearchEngineMainPageRequest, SearchEngineMainPageResponse}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

object ForkOrchestrator {

  private val props = Props[ForkOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

  case class ForkFlowRequest()

  case class ForkFlowResponse(titles: String)

  case class ForkFlowError()

}

class ForkOrchestrator
  extends Actor
    with ActorLogging
    with TimeoutableState {

  override def preStart(): Unit = {
    // start child actors
    1 to 3 foreach (_ => SearchEngineMainPageProvider.createChildActor(context))
  }

  override def aroundPostStop(): Unit = {
    log.info("Stopping...")
    postStop()
  }

  def handleResult(originalSender: ActorRef): State = {

    case expectedResult: Iterable[Any] => {

      // TODO make the return type of the main page actor to be a String and not a Try. Error response should be used when we can't get an successful http response
      val errorList = expectedResult.filter {
        case SearchEngineMainPageError => true
        case _ => false
      }

      if (!errorList.isEmpty)
        originalSender ! ForkFlowError
      else {

        val result = expectedResult.map {
          case e: SearchEngineMainPageResponse => e
        }.map(resp => resp.html)
          .map {
            case Success(html) => html
            case _ => ""
          }
          .map(html => "<title>.*<\\/title>".r findFirstIn html)
          .map {
            case Some(value) => value
            case None => ""
          }
          .reduce((s1, s2) => s1 + " | " + s2)

        originalSender ! ForkFlowResponse(result)
      }
    }
  }

  private val pageAskTimeout: FiniteDuration = 2 seconds

  override def receive: Receive = {
    case ForkFlowRequest => {

      import context.dispatcher

      implicit val timeout = Timeout(pageAskTimeout)

      val requestFutureList = context.children
        .zipWithIndex
        .map(tuple => tuple._1 ? SearchEngineMainPageRequest(tuple._2 + 1))

      Future.sequence(requestFutureList) pipeTo self

      assumeStateWithTimeout(pageAskTimeout, handleResult(sender), () => sender ! ForkFlowError)
    }

  }
}
