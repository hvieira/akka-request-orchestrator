package net.hvieira.orchestrator.transactional

import akka.actor.Actor

import scala.concurrent.duration._

trait TimeoutBehavior { this: Actor =>

  import context.dispatcher
  def sendTimeoutTick(timeout: FiniteDuration) = context.system.scheduler.scheduleOnce(timeout, self, BehaviorTimeout)

}

case class BehaviorTimeout()
