package net.hvieira.actor

import akka.actor.{Actor, ReceiveTimeout}

import scala.concurrent.duration._

trait TimeoutableState extends Actor {

  type State = Actor.Receive

  def assumeStateWithTimeout(timeout: FiniteDuration, successBehavior: State, timeoutFunction: () => Unit) = {
    // this can also be achieved by registering a tick with the actor scheduler to send a timeout message or run a timeout function
    // see commit logs for the previous version for this
    registerReceiveTimeout(timeout)
    context.become(
      successBehavior
        .orElse[Any, Unit]({
          case ReceiveTimeout => timeoutFunction()
        }).andThen((Unit) => cancelReceiveTimeout)
    )
  }

  private def registerReceiveTimeout(timeout: FiniteDuration): Unit = context.setReceiveTimeout(timeout)

  private def cancelReceiveTimeout: Unit = context.setReceiveTimeout(Duration.Undefined)

}
