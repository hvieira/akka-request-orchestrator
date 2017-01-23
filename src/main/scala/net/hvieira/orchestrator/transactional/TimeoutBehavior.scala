package net.hvieira.orchestrator.transactional

import akka.actor.Actor

import scala.concurrent.duration._

trait TimeoutBehavior { this: Actor =>

  import context.dispatcher
  import context.become

  private def registerTimeoutTick(timeout: FiniteDuration) = context.system.scheduler.scheduleOnce(timeout, self, BehaviorTimeout)

  def assumeTimeoutableBehavior(timeout: FiniteDuration, successBehavior: Receive, timeoutFunction: () => Unit) = {
    registerTimeoutTick(timeout)
    become(successBehavior.orElse({
      case BehaviorTimeout => timeoutFunction
    }))
  }

}

private case class BehaviorTimeout()
