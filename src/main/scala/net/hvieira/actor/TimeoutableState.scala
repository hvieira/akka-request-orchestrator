package net.hvieira.actor

import akka.actor.Actor

import scala.concurrent.duration._

trait TimeoutableState extends Actor {

  import context.{become, dispatcher}

  type State = Actor.Receive

  private def registerTimeoutTick(timeout: FiniteDuration) = context.system.scheduler.scheduleOnce(timeout, self, BehaviorTimeout)

  def assumeStateWithTimeout(timeout: FiniteDuration, successBehavior: State, timeoutFunction: () => Unit) = {
    registerTimeoutTick(timeout)
    become(
      successBehavior
        .orElse({
          case BehaviorTimeout => timeoutFunction
        })
    )
  }

}

private case class BehaviorTimeout()
