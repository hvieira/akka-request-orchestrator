package net.hvieira.actor

import akka.actor.Actor

import scala.concurrent.duration._

trait TimeoutableState extends Actor {

  import context.{become, dispatcher}

  type State = Actor.Receive

  private def registerTimeoutTick(timeout: FiniteDuration) = context.system.scheduler.scheduleOnce(timeout, self, BehaviorTimeout)

  def assumeStateWithTimeout(timeout: FiniteDuration, successBehavior: State, timeoutFunction: () => Unit) = {
    val timeoutTick = registerTimeoutTick(timeout)
    become(
      successBehavior
        .orElse[Any, Unit]({
          case BehaviorTimeout => timeoutFunction()
        }).andThen((Unit) => timeoutTick.cancel())
    )
  }

}

private case class BehaviorTimeout()
