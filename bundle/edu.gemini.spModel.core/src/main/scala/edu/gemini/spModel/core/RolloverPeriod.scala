// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.core

import java.time.Instant

import scala.collection.JavaConverters._
import scala.collection.SortedSet

import scalaz._
import Scalaz._

/**
 * A semester range during which a given rollover period is active.
 *
 * @param startSemester starting semester of the rollover period
 * @param endSemester   ending semester (inclusive) of the rollover period (will
 *                      be the same or after startSemester)
 */
sealed abstract case class RolloverPeriod private (
  startSemester: Semester,
  endSemester:   Semester
) {

  assert(startSemester <= endSemester, "startSemester must be <= endSemester")

  def includes(s: Semester): Boolean =
    (startSemester <= s) && (s <= endSemester)

  def startInstant(s: Site): Instant =
    startSemester.getStartDate(s).toInstant

  def endInstant(s: Site): Instant =
    endSemester.getEndDate(s).toInstant

  def semesters: SortedSet[Semester] =
    SortedSet(
      Stream.iterate(startSemester)(_.next).takeWhile(_ <= endSemester).toList: _*
    )

  def semestersAsJava: java.util.Set[Semester] =
    semesters.asJava
}

object RolloverPeriod {

  val s2018A: Semester =
    new Semester(2018, Semester.Half.A)

  val s2018B: Semester =
    s2018A.next

  // 2018B ends two rollover periods (2017B -> 2018B) and (2018A -> 2018B)
  // because of the rule change.  we'll just pick the longest one

  def ending(s: Semester): RolloverPeriod =
    new RolloverPeriod(if (s <= s2018B) s.prev.prev else s.prev, s) {}

  def beginning(s: Semester): RolloverPeriod =
    new RolloverPeriod(s, if (s >= s2018A) s.next else s.next.next) {}

  implicit val EqRolloverPeriod: Equal[RolloverPeriod] =
    Equal.equalBy(s => (s.startSemester, s.endSemester))

}
