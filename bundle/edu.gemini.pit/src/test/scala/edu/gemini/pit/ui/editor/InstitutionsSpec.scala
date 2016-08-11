package edu.gemini.pit.ui.editor

import org.specs2.mutable._

class InstitutionsSpec extends Specification {

  "The Institutions object" should {
    "include Universidad Andres Bello, REL-550" in {
      Institutions.all must contain((i: Institution) => i.name must beEqualTo("Universidad Andres Bello"))
      val uab = Institutions.all.find(_.name == "Universidad Andres Bello").head
      uab.country must beEqualTo("Chile")
      uab.contact.phone must containMatch("\\+56-2-8370134")
      uab.affiliate must beNone
    }

    "include Subaru Telescope, NAOJ, with a country of USA but an affiliate of Japan" in {
      Institutions.all must contain((i: Institution) => i.name must beEqualTo("Subaru Telescope, NAOJ"))
      val subaru = Institutions.all.find(_.name == "Subaru Telescope, NAOJ").head
      subaru.country must beEqualTo("USA")
      subaru.affiliate must beSome("Japan")
    }
  }

}