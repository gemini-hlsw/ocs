package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import java.io.InputStreamReader

class SemesterSpec extends Specification {
  "The Semester class" should {
    "have a default" in {
      Semester.current must beEqualTo(Semester(2016, SemesterOption.B))
    }
    "deserialize any semester" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_old_semester.xml")))

      proposal.semester must beEqualTo(Semester(2011, SemesterOption.B))
    }
    "may be parsed out of a string" in {
      Semester.parse("2013A") must beSome(Semester(2013, SemesterOption.A))
      Semester.parse("2014B") must beSome(Semester(2014, SemesterOption.B))
      Semester.parse("abc") must beNone
    }
  }
}