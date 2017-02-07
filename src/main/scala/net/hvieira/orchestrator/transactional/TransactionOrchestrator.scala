package net.hvieira.orchestrator.transactional

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import net.hvieira.actor.TimeoutableState
import net.hvieira.orchestrator.transactional.TransactionOrchestrator.{TransactionFlowError, TransactionFlowRequest, TransactionFlowResponse}
import net.hvieira.random.RandomIntegerProvider
import net.hvieira.random.RandomIntegerProvider.{RandomIntegerRequest, RandomIntegerResponse}
import net.hvieira.searchprovider.SearchEngineMainPageProvider
import net.hvieira.searchprovider.SearchEngineMainPageProvider.{SearchEngineMainPageRequest, SearchEngineMainPageResponse}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TransactionOrchestrator {

  private val props = Props[TransactionOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

  case class TransactionFlowRequest()

  case class TransactionFlowResponse(val html: String)

  case class TransactionFlowError()

}

class TransactionOrchestrator
  extends Actor
    with TimeoutableState
    with ActorLogging {

  def handleSearchEngineResponse(originalSender: ActorRef): State = {
    case SearchEngineMainPageResponse(Success(html)) => originalSender ! TransactionFlowResponse(html)
    case SearchEngineMainPageResponse(Failure(_)) => originalSender ! TransactionFlowError()
    case _ => {
      log.error("Received unexpected message type")
      originalSender ! TransactionFlowError()
    }
  }

  def handleRandomIntegerResp(originalSender: ActorRef): Receive = {
    case RandomIntegerResponse(Success(value)) => {
      requestSearchProviderMainPage(originalSender, value)
    }

    case _ => {
      log.error("An error occurred while waiting for random integer!!")
      originalSender ! TransactionFlowError
    }
  }

  def requestSearchProviderMainPage(originalSender: ActorRef, value: Int): Unit = {
    log.info("Requesting provider for {}", value)

    SearchEngineMainPageProvider.createChildActor(context) ! SearchEngineMainPageRequest(value)

    assumeStateWithTimeout(2 seconds,
      handleSearchEngineResponse(originalSender),
      () => {
        log.error("Timed out requesting a search engine main page")
        originalSender ! TransactionFlowError
      })
  }

  def handleRandomNumberResponse(originalSender: ActorRef) = {

    assumeStateWithTimeout(3 seconds,
      handleRandomIntegerResp(originalSender),
      () => {
        log.error("Timed out requesting a random number")
        originalSender ! TransactionFlowError
      })
  }

  def handleRequestWithTransaction(): Unit = {
    val randomNumberHandler = RandomIntegerProvider.createChildActor(context)
    randomNumberHandler ! RandomIntegerRequest(1, 3)
    handleRandomNumberResponse(sender)
  }


  override def aroundPostStop(): Unit = {
    log.info("Stopping...")
    postStop()
  }

  override def receive: Receive = {
    case TransactionFlowRequest => {
      handleRequestWithTransaction()
    }
  }
}