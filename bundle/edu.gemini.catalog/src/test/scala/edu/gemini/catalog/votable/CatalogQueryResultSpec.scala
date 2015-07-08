package edu.gemini.catalog.votable

import edu.gemini.catalog.api._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
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
      val qc = CatalogQuery.catalogQueryOnBand(c, RadiusConstraint.between(Angle.zero, coneSearch), noMagnitudeConstraint)
      // Filtering on search radius should give all back
      unfiltered.filter(qc).targets.rows should be size 11
    }
    "be able to filter targets to a specific ring" in {
      val qc = CatalogQuery.catalogQueryOnBand(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), noMagnitudeConstraint)
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 7
    }
    "be able to filter by band" in {
      val qc = CatalogQuery.catalogQueryOnBand(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), noMagnitudeConstraint)
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 7
    }
    "be able to filter by faintness on band j" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQueryOnBand(c, RadiusConstraint.between(Angle.zero, coneSearch), m)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude discards 3 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter by faintness and saturation on band j" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), Some(SaturationConstraint(14.0)))
      val qc = CatalogQuery.catalogQueryOnBand(c, RadiusConstraint.between(Angle.zero, coneSearch), m)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude leaves only 4 targets
      filtered.targets.rows should be size 4
    }
    "be able to filter with a band and range" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), None)
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQueryOnBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), m)

      val qc2 = CatalogQuery.catalogQueryWithDynamicBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr)
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering only on J-magnitude leaves 9 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter with a band range with identity adjustments" in {
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQueryWithDynamicBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr)

      def nilAdjustment(mr: MagnitudeRange, mag: Magnitude) = mr

      val qc2 = CatalogQuery.catalogQueryWithAdjustedRange(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr, nilAdjustment)
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering on magnitude leaves just 9  targets
      filtered.targets.rows should be size 9
      filtered2.targets.rows should be size 9
    }
    "be able to filter with a band range with nominal conditions" in {
      import edu.gemini.pot.ModelConverters._

      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQueryWithDynamicBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr)

      // Nominal conditions don't change the MagnitudeRange
      val conditions = SPSiteQuality.Conditions.NOMINAL
      def adjustment(mr: MagnitudeRange, mag: Magnitude) =
        mr.adjust(k => conditions.magAdjustOp().apply(mag.copy(value = k).toOldModel).toNewModel.value)

      val qc2 = CatalogQuery.catalogQueryWithAdjustedRange(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr, adjustment)
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering on magnitude with nominal conditions doesnt change the output
      filtered.targets.rows should be size 9
    }
    "be able to filter with a band range with nominal conditions" in {
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)

      // Large adjustment leaving all targets out
      def adjustment(mr: MagnitudeRange, mag: Magnitude) =
        mr.adjust(k => k - 20)

      val qc = CatalogQuery.catalogQueryWithAdjustedRange(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr, adjustment)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude and with the adjustemn takes all the targets out
      filtered.targets.rows should beEmpty
    }
    "be able to filter for Gems" in {
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)

      // No adjustment
      def adjustment(mr: MagnitudeRange, mag: Magnitude) =
        mr.adjust(k => k)

      val qc = CatalogQuery.catalogQueryForGems(0, c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), mr, adjustment)
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude leaves 9 targets
      filtered.targets.rows should be size 9
    }
  }

}
