package net.hvieira.orchestrator.transactional

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import net.hvieira.random.{RandomIntegerProvider, RandomIntegerRequest, RandomIntegerResponse}

import scala.concurrent.duration._
import scala.util.Success

object TransactionOrchestrator {

  private val props = Props[TransactionOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

}

class TransactionOrchestrator extends Actor with TimeoutBehavior {

  import context.become

  def handleRandomNumberResponse(originalSender: ActorRef) = {

    def waitingForRandomIntegerResp(originalSender: ActorRef) : Receive = {
      case RandomIntegerResponse(Success(value)) => {
       // TODO at this point we need to change state/behavior because otherwise we will receive the timeout message which will cause a dead letter
        originalSender ! new TransactionFlowResponse(value)
      }
      case BehaviorTimeout => originalSender ! TransactionFlowError
      case _ => println("An Error occurred while waiting for random integer!!")
    }

    // TODO extract this to the trait so that you only specify the behavior function and duration
    sendTimeoutTick(3 seconds)
    become(waitingForRandomIntegerResp(originalSender))
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