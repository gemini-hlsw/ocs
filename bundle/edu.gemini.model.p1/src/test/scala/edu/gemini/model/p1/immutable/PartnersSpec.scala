package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML

class PartnersSpec extends SpecificationWithJUnit {

  "The Partners object" should {
    "not include the UK, REL-622" in {
      NgoPartner.values must have size 7
      NgoPartner.forName("UK") must throwA[Exception]
    }
  }
}