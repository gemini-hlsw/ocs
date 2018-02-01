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
    "Texes has been removed in 2018B" in {
      val root = new Root(Semester(2018, B))
      root.choices must not contain Instrument.Texes
    }
    "include Speckles" in {
      val root = new Root(Semester(2016, A))
      root.choices must contain (Instrument.Dssi)
    }
    "include Visitor" in {
      val root = new Root(Semester(2016, B))
      root.choices must contain(Instrument.Visitor)
    }
    "include Gpi" in {
      val root = new Root(Semester(2016, A))
      root.choices must contain(Instrument.Gpi)
    }
    "includes Graces" in {
      val root = new Root(Semester(2016, A))
      root.choices must contain(Instrument.Graces)
    }
    "includes Phoenix" in {
      val root = new Root(Semester(2018, B))
      root.choices must contain(Instrument.Phoenix)
    }
    "Michelle has been removed in 2016A" in {
      val root = new Root(Semester(2016, A))
      root.choices must not contain Instrument.Michelle
    }
    "T-ReCS has been removed in 2016A" in {
      val root = new Root(Semester(2016, A))
      root.choices must not contain Instrument.Trecs
    }
  }

}
