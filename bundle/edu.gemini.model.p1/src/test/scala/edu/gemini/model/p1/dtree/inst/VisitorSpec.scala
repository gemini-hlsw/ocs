package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification
import edu.gemini.model.p1.immutable.Site

class VisitorSpec extends Specification {
  "The Visitor decision tree" should {
      "includes a site selector" in {
        val visitor = Visitor()
        visitor.title must beEqualTo("Site")
        visitor.choices must have size(2)
      }
  }

}
