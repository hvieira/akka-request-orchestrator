package net.hvieira.orchestrator.transactional

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import net.hvieira.actor.TimeoutableState
import net.hvieira.orchestrator.transactional.TransactionOrchestrator.{TransactionFlowError, TransactionFlowRequest, TransactionFlowResponse}
import net.hvieira.random.RandomIntegerProvider
import net.hvieira.random.RandomIntegerProvider.{RandomIntegerRequest, RandomIntegerResponse}
import net.hvieira.searchprovider.SearchEngineMainPageProvider
import net.hvieira.searchprovider.SearchEngineMainPageProvider.{SearchEngineMainPageRequest, SearchEngineMainPageResponse}

import scala.concurrent.duration._
import scala.util.Success

object TransactionOrchestrator {

  private val props = Props[TransactionOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

  case class TransactionFlowRequest()
  case class TransactionFlowResponse(val html: String)
  case class TransactionFlowError()
}

class TransactionOrchestrator extends Actor with TimeoutableState {

  def handleSearchEngineResponse(originalSender: ActorRef): State = {
    case SearchEngineMainPageResponse(Success(html)) => originalSender ! TransactionFlowResponse(html)
      // TODO there are missing match patterns here specifically for error cases
  }

  def handleRandomNumberResponse(originalSender: ActorRef) = {

    def waitingForRandomIntegerResp(originalSender: ActorRef) : Receive = {
      case RandomIntegerResponse(Success(value)) => {
        println(s"Requesting provider for $value")

        SearchEngineMainPageProvider.createChildActor(context) ! SearchEngineMainPageRequest(value)

        assumeStateWithTimeout(2 seconds,
          handleSearchEngineResponse(originalSender),
          () => originalSender ! TransactionFlowError)
      }

      case _ => {
        // TODO use logs instead
        println("An Error occurred while waiting for random integer!!")
        originalSender ! TransactionFlowError
      }
    }

    assumeStateWithTimeout(3 seconds,
      waitingForRandomIntegerResp(originalSender),
      () => originalSender ! TransactionFlowError)
  }

  def handleRequestWithTransaction(): Unit = {
    val randomNumberHandler = RandomIntegerProvider.createChildActor(context)
    randomNumberHandler ! RandomIntegerRequest(1, 3)

    handleRandomNumberResponse(sender)
  }


  override def aroundPostStop(): Unit = {
    // TODO use logs
    println(s"Stopping Actor $self")
    postStop()
  }

  override def receive: Receive = {
    case TransactionFlowRequest => {
      handleRequestWithTransaction()
    }
  }
}