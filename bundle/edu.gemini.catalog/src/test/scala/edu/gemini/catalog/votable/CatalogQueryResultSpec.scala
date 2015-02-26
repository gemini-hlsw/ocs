package edu.gemini.catalog.votable

import edu.gemini.catalog.api._
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

  val noMagnitudeConstraint = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(100), None)

  "CatalogQueryResultSpec" should {
    "be able to filter targets inside the requested range limit" in {
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(noMagnitudeConstraint))
      // Filtering on search radius should give all back
      unfiltered.filter(qc).targets.rows should be size 12
    }
    "be able to filter targets to a specific ring" in {
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), Some(noMagnitudeConstraint))
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 8
    }
    "be able to filter by band" in {
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), Some(noMagnitudeConstraint))
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 8
    }
    "be able to filter by faintness on band j" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), None)
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(m))
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude discards 3 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter by faintness and saturation on band j" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), Some(SaturationConstraint(14.0)))
      val qc = CatalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(m))
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude leaves only 4 targets
      filtered.targets.rows should be size 4
    }
  }

}
