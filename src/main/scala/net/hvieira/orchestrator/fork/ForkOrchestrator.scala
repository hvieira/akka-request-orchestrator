package net.hvieira.orchestrator.fork

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import net.hvieira.actor.TimeoutableState
import net.hvieira.orchestrator.fork.ForkOrchestrator.{ForkFlowError, ForkFlowRequest, ForkFlowResponse}
import net.hvieira.searchprovider.{SearchEngineMainPageProvider, SearchProvider}
import net.hvieira.searchprovider.SearchEngineMainPageProvider.{SearchEngineMainPageError, SearchEngineMainPageRequest, SearchEngineMainPageResponse}

import scala.concurrent.Future
import scala.concurrent.duration._

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
    SearchProvider.values.foreach(_ => SearchEngineMainPageProvider.createChildActor(context))
  }

  override def aroundPostStop(): Unit = {
    log.info("Stopping...")
    postStop()
  }

  def handleResult(originalSender: ActorRef): State = {

    case expectedResult: Iterable[Any] => {

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
