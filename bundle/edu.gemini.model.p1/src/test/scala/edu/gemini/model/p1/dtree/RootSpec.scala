package edu.gemini.model.p1.dtree

import edu.gemini.model.p1.immutable.{Semester, Instrument}
import edu.gemini.model.p1.immutable.SemesterOption._
import org.specs2.mutable.Specification

class RootSpec extends Specification {

  "The Root Spec" should {
    "include Gsaoi" in {
      val root = new Root(Semester(2016, A))
      root.choices.contains(Instrument.Gsaoi) must beTrue
    }
    "not includes Texes" in {
      val root = new Root(Semester(2020, A))
      root.choices.contains(Instrument.Texes) must beFalse
    }
    "DSSI has been removed in 2019B" in {
      val root = new Root(Semester(2019, B))
      root.choices.contains(Instrument.Dssi) must beFalse
    }
    "include Visitor" in {
      val root = new Root(Semester(2016, B))
      root.choices.contains(Instrument.Visitor) must beTrue
    }
    "GPI is not available in 2020B" in {
      val root = new Root(Semester(2016, B))
      root.choices.contains(Instrument.Gpi) must beFalse
    }
    "include Graces" in {
      val root = new Root(Semester(2016, A))
      root.choices.contains(Instrument.Graces) must beTrue
    }
    "not include Phoenix" in {
      val root = new Root(Semester(2020, A))
      root.choices.contains(Instrument.Phoenix) must beFalse
    }
    "Michelle has been removed in 2016A" in {
      val root = new Root(Semester(2016, A))
      root.choices.contains(Instrument.Michelle) must beFalse
    }
    "T-ReCS has been removed in 2016A" in {
      val root = new Root(Semester(2016, A))
      root.choices.contains(Instrument.Trecs) must beFalse
    }
    "include Alopeke" in {
      val root = new Root(Semester(2018, B))
      root.choices.contains(Instrument.Alopeke) must beTrue
    }
    "include Zorro" in {
      val root = new Root(Semester(2019, B))
      root.choices.contains(Instrument.Zorro) must beTrue
    }
    "include IGRINS" in {
      val root = new Root(Semester(2020, A))
      root.choices.contains(Instrument.Igrins) must beTrue
    }
  }
}
