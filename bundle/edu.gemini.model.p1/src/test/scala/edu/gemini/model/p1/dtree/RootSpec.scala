package edu.gemini.model.p1.dtree

import edu.gemini.model.p1.immutable.{Semester, Instrument}
import edu.gemini.model.p1.immutable.SemesterOption._
import org.specs2.mutable.Specification

class RootSpec extends Specification {

  "The Root Spec" should {
    "include Gsaoi" in {
      val root = new Root(Semester(2016, A))
      root.choices must contain(Instrument.Gsaoi)
    }
    "not includes Texes" in {
      val root = new Root(Semester(2020, A))
      root.choices must not contain Instrument.Texes
    }
    "DSSI has been removed in 2019B" in {
      val root = new Root(Semester(2019, B))
      root.choices must not contain Instrument.Dssi
    }
    "include Visitor" in {
      val root = new Root(Semester(2016, B))
      root.choices must contain(Instrument.Visitor)
    }
    "GPI is not available in 2020B" in {
      val root = new Root(Semester(2016, B))
      root.choices must not contain Instrument.Gpi
    }
    "include Graces" in {
      val root = new Root(Semester(2016, A))
      root.choices must contain(Instrument.Graces)
    }
    "not include Phoenix" in {
      val root = new Root(Semester(2020, A))
      root.choices must not contain Instrument.Phoenix
    }
    "Michelle has been removed in 2016A" in {
      val root = new Root(Semester(2016, A))
      root.choices must not contain Instrument.Michelle
    }
    "T-ReCS has been removed in 2016A" in {
      val root = new Root(Semester(2016, A))
      root.choices must not contain Instrument.Trecs
    }
    "include Alopeke" in {
      val root = new Root(Semester(2018, B))
      root.choices must contain (Instrument.Alopeke)
    }
    "include Zorro" in {
      val root = new Root(Semester(2019, B))
      root.choices must contain (Instrument.Zorro)
    }
    "include IGRINS" in {
      val root = new Root(Semester(2020, A))
      root.choices must contain (Instrument.Igrins)
    }
  }
}
