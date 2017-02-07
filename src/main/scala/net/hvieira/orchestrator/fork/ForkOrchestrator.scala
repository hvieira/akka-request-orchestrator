package net.hvieira.orchestrator.fork

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import net.hvieira.actor.TimeoutableState
import net.hvieira.orchestrator.fork.ForkOrchestrator.{ForkFlowError, ForkFlowRequest, ForkFlowResponse}
import net.hvieira.searchprovider.SearchEngineMainPageProvider
import net.hvieira.searchprovider.SearchEngineMainPageProvider.{SearchEngineMainPageRequest, SearchEngineMainPageResponse}

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
    case result: String => {
      log.info(s"FORK RESULT => [$result] to $originalSender")
      originalSender ! ForkFlowResponse(result)
    }
  }

  override def receive: Receive = {
    case ForkFlowRequest => {

      implicit val timeout = Timeout(2 seconds)
      import context.dispatcher

      val futureList = context.children
        .zipWithIndex
        .map {
          case (worker, i) => worker ? SearchEngineMainPageRequest(i + 1)
        }

      val reducedFuture = Future.sequence(futureList)

      // TODO might as well just pipe the list of Strings and then compose a proper response from the list
      reducedFuture.map(list => list.map {
        case SearchEngineMainPageResponse(Success(html)) => "Google|DuckDuckGo|Yahoo".r findFirstIn html
      }).map(list => list.map {
        case Some(value) => value
        case None => ""
      }).map(list => list.reduce((s1, s2) => s1 + "||" + s2))
        .pipeTo(self)

      assumeStateWithTimeout(5 seconds, handleResult(sender), () => sender ! ForkFlowError)
    }

  }
}
