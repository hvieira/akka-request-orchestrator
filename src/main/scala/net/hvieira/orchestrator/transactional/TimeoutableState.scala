package net.hvieira.orchestrator.transactional

import akka.actor.Actor

import scala.concurrent.duration._

trait TimeoutableState extends Actor {

  import context.dispatcher
  import context.become

  type State = Actor.Receive

  private def registerTimeoutTick(timeout: FiniteDuration) = context.system.scheduler.scheduleOnce(timeout, self, BehaviorTimeout)

  def assumeStateWithTimeout(timeout: FiniteDuration, successBehavior: State, timeoutFunction: () => Unit) = {
    val timeout = registerTimeoutTick(timeout)
    become(
      successBehavior
        .orElse({
          case BehaviorTimeout => timeoutFunction
        })
    )
  }

}

private case class BehaviorTimeout()
