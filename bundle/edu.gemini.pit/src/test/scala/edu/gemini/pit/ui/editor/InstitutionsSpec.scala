package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.InstitutionAddress
import org.specs2.mutable._

class InstitutionsSpec extends Specification {

  "The Institutions object" should {
    "include Universidad Andres Bello, REL-550" in {
      val UniversidadAndresBello = "Universidad Andres Bello"
      Institutions.all must contain((i: Institution) => i.name must beEqualTo(UniversidadAndresBello))
      val uab = Institutions.all.find(_.name == UniversidadAndresBello).head
      uab.country must beEqualTo("Chile")
      uab.contact.phone must containMatch("\\+56-2-8370134")
      uab.affiliate must beNone
    }

    "include Subaru Telescope, NAOJ, with a country of USA but an affiliate of Japan" in {
      val SubaruNAOJ = "Subaru Telescope, NAOJ"
      Institutions.all must contain((i: Institution) => i.name must beEqualTo(SubaruNAOJ))
      val subaru = Institutions.all.find(_.name == SubaruNAOJ).head
      subaru.country must beEqualTo("USA")
      subaru.affiliate must beSome(Institutions.country2Ngo("Japan"))
    }

    "include the Canada-France-Hawaii Telescope Corporation, with a country of US but an NGO partner of Canada" in {
      val CFT = "Canada-France-Hawaii Telescope Corporation"
      Institutions.all must contain((i: Institution) => i.name must beEqualTo(CFT))
      val cft = Institutions.all.find(_.name == CFT).head
      cft.country must beEqualTo("USA")
      val cftAddr = InstitutionAddress(cft.name, cft.addr.map(_.trim).mkString("\n"), cft.country)
      Institutions.institution2Ngo(cftAddr) must beEqualTo(Institutions.country2Ngo("Canada"))
    }
  }

}