package edu.gemini.sp.vcs.diff

import edu.gemini.spModel.core.{Site, ProgramType, Semester, SPProgramID}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen._

import java.text.SimpleDateFormat
import java.util.Date


/**
 * ProgramId Generator.
 */
object ProgramIdGen {

  val genYear: Gen[Int] = choose(2000, 2020)

  val genSite: Gen[Site] = oneOf(Site.GN, Site.GS)

  val genSemester: Gen[Semester] =
    for {
      year <- genYear
      half <- oneOf(Semester.Half.values())
    } yield new Semester(year, half)

  val genScienceId: Gen[SPProgramID] =
    for {
      site <- genSite
      sem  <- genSemester
      pt   <- oneOf(ProgramType.All.filter(_.isScience).map(_.abbreviation))
      num  <- choose(1, 999)
    } yield SPProgramID.toProgramID(s"$site-$sem-$pt-$num")

  def genDate(site: Site): Gen[Date] =
    for {
      sem  <- genSemester
      time <- choose(sem.getStartDate(site).getTime, sem.getEndDate(site).getTime - 1)
    } yield new Date(time)

  val genDailyId: Gen[SPProgramID] = {
    def format(d: Date): String = new SimpleDateFormat("yyyyMMdd").format(d)

    for {
      site <- genSite
      pt   <- oneOf(ProgramType.All.filterNot(_.isScience).map(_.abbreviation))
      date <- genDate(site)
    } yield SPProgramID.toProgramID(s"$site-$pt${format(date)}")
  }

  val unstructuredId: Gen[SPProgramID] =
    for {
      i <- choose(1,10)
      s <- listOfN(i, oneOf(alphaNumChar, '-')).map(_.mkString)
    } yield SPProgramID.toProgramID(s)

  val genSomeId: Gen[SPProgramID] = frequency(
    (4, genScienceId),
    (5, genDailyId),
    (1, unstructuredId)
  )

  implicit val arbId = Arbitrary(genSomeId)
}
