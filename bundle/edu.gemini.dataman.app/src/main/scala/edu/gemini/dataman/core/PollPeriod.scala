package edu.gemini.dataman.core

import java.time.Duration

import scalaz._

/** Gives a type to the various poll periods in the Data Manager.
 */
sealed trait PollPeriod {
  def time: Duration
  def millis: Long = time.toMillis
}

object PollPeriod {
  final case class Tonight(time: Duration)     extends PollPeriod
  final case class ThisWeek(time: Duration)    extends PollPeriod
  final case class AllPrograms(time: Duration) extends PollPeriod
  final case class ObsRefresh(time: Duration)  extends PollPeriod

  implicit val EqualPollPeriod: Equal[PollPeriod] = Equal.equalA
}
