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
    "includes Texes" in {
      val root = new Root(Semester(2019, B))
      root.choices must contain(Instrument.Texes)
    }
    "DSSI has been removed in 2019B" in {
      val root = new Root(Semester(2019, B))
      root.choices must not contain Instrument.Dssi
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
    "include Alopeke" in {
      val root = new Root(Semester(2018, B))
      root.choices must contain (Instrument.Alopeke)
    }
    "include Zorro" in {
      val root = new Root(Semester(2019, B))
      root.choices must contain (Instrument.Zorro)
    }
  }

}
