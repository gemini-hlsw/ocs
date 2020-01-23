package edu.gemini.catalog.votable

import edu.gemini.catalog.api._
import edu.gemini.catalog.api.CatalogName._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import org.specs2.mutable.Specification

class CatalogQueryResultSpec extends Specification {
  // reference values for the query
  val ra = RightAscension.fromAngle(Angle.fromDegrees(10))
  val dec = Declination.fromAngle(Angle.fromDegrees(20)).getOrElse(Declination.zero)
  val c = Coordinates(ra, dec)
  val coneSearch = Angle.fromDegrees(0.1)

  val xmlFile = "votable-ucac4.xml"
  val targets = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile"))
  val unfiltered = CatalogQueryResult(targets | ParsedVoResource(Nil))

  val noMagnitudeConstraint = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(100), None)

  "CatalogQueryResultSpec" should {
    "be able to filter targets inside the requested range limit" in {
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, coneSearch), noMagnitudeConstraint, CatalogName.UCAC4)
      // Filtering on search radius should give all back
      unfiltered.filter(qc).targets.rows should be size 11
    }
    "be able to filter targets to a specific ring" in {
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), noMagnitudeConstraint, CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 7
    }
    "be able to filter by band" in {
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), noMagnitudeConstraint, CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 7
    }
    "be able to filter by faintness on band j" in {
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, coneSearch), mc, CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude discards 3 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter by faintness and saturation on band j" in {
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), Some(SaturationConstraint(14.0)))
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, coneSearch), mc, CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude leaves only 4 targets
      filtered.targets.rows should be size 4
    }
    "be able to filter with a band and range" in {
      val mc1 = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val mc2 = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val qc1 = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), mc1, CatalogName.UCAC4)
      val qc2 = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), mc2, CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc1)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering only on J-magnitude leaves 9 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter with a band range with Nil) adjustments" in {
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), mc, CatalogName.UCAC4)

      def nilAdjustment(v: Double) = v

      val qc2 = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), mc.adjust(nilAdjustment), CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering on magnitude leaves just 9  targets
      filtered.targets.rows should be size 9
      filtered2.targets.rows should be size 9
    }
    "be able to filter with a band range with nominal conditions" in {
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), mc, CatalogName.UCAC4)

      // Nominal conditions don't change the MagnitudeRange
      val conditions = SPSiteQuality.Conditions.NOMINAL

      val qc2 = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), conditions.adjust(mc), CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering on magnitude with nominal conditions doesn't change the output
      filtered.targets.rows should be size 9
    }
    "be able to filter with a band range with extreme adjustments" in {
      val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(15.0), None)

      val qc = CatalogQuery.coneSearch(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), mc.adjust(k => k - 20), CatalogName.UCAC4)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude and with the given adjustment takes all the targets out
      filtered.targets.rows should beEmpty
    }
  }

}
