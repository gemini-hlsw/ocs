package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.immutable.Site
import org.specs2.mutable.Specification

class TexesSpec extends Specification {
  "The Texes decision tree" should {
      "include a site choice" in {
        val texes = Texes()
        texes.title must beEqualTo("Site")
        texes.choices must have size 1
      }
      "include a disperser choice" in {
        val texes = Texes()
        val disperserNode = texes.apply(VisitorSite.fromSite(Site.GN)).a
        disperserNode.title must beEqualTo("Disperser")
        disperserNode.choices must have size 4
      }
  }

}
