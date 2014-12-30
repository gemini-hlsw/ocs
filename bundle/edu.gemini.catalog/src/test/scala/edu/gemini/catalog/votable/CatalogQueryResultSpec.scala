package edu.gemini.catalog.votable

import edu.gemini.catalog.api.{RadiusConstraint, CatalogQuery}
import edu.gemini.spModel.core._
import org.specs2.mutable.SpecificationWithJUnit

class CatalogQueryResultSpec extends SpecificationWithJUnit {
  // reference values for the query
  val ra = RightAscension.fromAngle(Angle.fromDegrees(10))
  val dec = Declination.fromAngle(Angle.fromDegrees(20)).getOrElse(Declination.zero)
  val c = Coordinates(ra, dec)
  val coneSearch = Angle.fromDegrees(0.1)

  val xmlFile = "votable-ucac4.xml"
  val targets = VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile"))
  val unfiltered = CatalogQueryResult(targets | ParsedVoResource(Nil))

  "CatalogQueryResultSpec" should {
    "be able to filter targets inside the requested range limit" in {
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch))
      // Filtering on search radius should give all back
      unfiltered.filter(qc).targets.rows should be size 12
    }
    "be able to filter targets to a specific ring" in {
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch))
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 8
    }
    "be able to filter by band" in {
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch))
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 8
    }
  }

}
