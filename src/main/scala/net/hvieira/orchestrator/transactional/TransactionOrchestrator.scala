package net.hvieira.orchestrator.transactional

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import net.hvieira.random.{RandomIntegerProvider, RandomIntegerRequest, RandomIntegerResponse}

import scala.util.Success

object TransactionOrchestrator {

  private val props = Props[TransactionOrchestrator]

  def createActor(actorSystem: ActorSystem) = actorSystem.actorOf(props)

}

// TODO try using "become" funcionality to handle waiting for concrete responses instead of using the ask pattern
class TransactionOrchestrator extends Actor {

  import context.become

  // use a finite state machine sort of solution to handle the futures
  def handleRequestWithTransaction(): Unit = {
    val randomNumberHandler = RandomIntegerProvider.createActor(this.context.system)
    randomNumberHandler ! RandomIntegerRequest(1, 3)

    become(waitingForRandomIntegerResp(sender))
  }

  def waitingForRandomIntegerResp(originalSender: ActorRef) : Receive = {
    case RandomIntegerResponse(Success(value)) => {
      originalSender ! new TransactionFlowResponse(value)
    }
    case _ => println("An Error occurred while waiting for random integer!!")
  }

  override def receive: Receive = {
    case TransactionFlowRequest => {
      handleRequestWithTransaction()
    }
  }
}

case class TransactionFlowRequest()

case class TransactionFlowResponse(val generatedNum: Int)