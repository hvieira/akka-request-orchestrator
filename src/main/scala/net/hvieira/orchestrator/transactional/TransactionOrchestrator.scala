package net.hvieira.orchestrator.transactional

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import net.hvieira.random.{RandomIntegerProvider, RandomIntegerRequest, RandomIntegerResponse}

import scala.concurrent.duration._
import scala.util.Success

object TransactionOrchestrator {

  private val props = Props[TransactionOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

}

class TransactionOrchestrator extends Actor with TimeoutableState {

  import context.unbecome

  def handleRandomNumberResponse(originalSender: ActorRef) = {

    def waitingForRandomIntegerResp(originalSender: ActorRef) : Receive = {
      case RandomIntegerResponse(Success(value)) => {
        originalSender ! new TransactionFlowResponse(value)
        // TODO at this point it needs to change state/behavior because otherwise the system will receive the timeout message which will cause a dead letter
        // temporary fix
        unbecome()
      }
      case _ => println("An Error occurred while waiting for random integer!!")
    }

    assumeStateWithTimeout(3 seconds,
      waitingForRandomIntegerResp(originalSender),
      () => originalSender ! TransactionFlowError)
  }

  def handleRequestWithTransaction(): Unit = {
    val randomNumberHandler = RandomIntegerProvider.createActor(this.context.system)
    randomNumberHandler ! RandomIntegerRequest(1, 3)

    handleRandomNumberResponse(sender)
  }

  override def receive: Receive = {
    case TransactionFlowRequest => {
      handleRequestWithTransaction()
    }
  }
}

case class TransactionFlowRequest()

case class TransactionFlowResponse(val generatedNum: Int)
case class TransactionFlowError()