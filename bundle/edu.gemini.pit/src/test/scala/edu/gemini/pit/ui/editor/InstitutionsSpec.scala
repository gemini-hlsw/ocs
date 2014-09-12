package edu.gemini.pit.ui.editor

import org.specs2.mutable._

class InstitutionsSpec extends SpecificationWithJUnit {

  "The Institutions object" should {
    "include Universidad Andres Bello, REL-550" in {
      Institutions.all must have(_.name == "Universidad Andres Bello")
      val uab = Institutions.all.find(_.name == "Universidad Andres Bello").head
      uab.country must beEqualTo("Chile")
      uab.contact.phone must containMatch("\\+56-2-8370134")
    }
  }

}