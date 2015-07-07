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
      val qc = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(noMagnitudeConstraint))
      // Filtering on search radius should give all back
      unfiltered.filter(qc).targets.rows should be size 11
    }
    "be able to filter targets to a specific ring" in {
      val qc = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), Some(noMagnitudeConstraint))
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 7
    }
    "be able to filter by band" in {
      val qc = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.fromDegrees(0.05), coneSearch), Some(noMagnitudeConstraint))
      val filtered = unfiltered.filter(qc)

      // Filtering on search radius filters out 4 targets
      filtered.targets.rows should be size 7
    }
    "be able to filter by faintness on band j" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(m))
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude discards 3 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter by faintness and saturation on band j" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), Some(SaturationConstraint(14.0)))
      val qc = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(m))
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude leaves only 4 targets
      filtered.targets.rows should be size 4
    }
    "be able to filter ignoring the band" in {
      val m = MagnitudeRange(FaintnessConstraint(15.0), Some(SaturationConstraint(14.0)))
      val qc  = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.zero, coneSearch), None)
      val qc2 = CatalogQuery.catalogQueryWithoutBand(c, RadiusConstraint.between(Angle.zero, coneSearch), Some(m))
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering on magnitude leaves 12 targets
      filtered.targets.rows should be size 12
    }
    "be able to filter with a band and range" in {
      val m = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(15.0), None)
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), Some(m))

      val qc2 = CatalogQuery.catalogQueryRangeOnBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), Some(mr))
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering only on J-magnitude leaves 9 targets
      filtered.targets.rows should be size 9
    }
    "be able to filter with a band range with identity adjustments" in {
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)
      val qc = CatalogQuery.catalogQueryRangeOnBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), Some(mr))

      def nilAdjustment(mr: Option[MagnitudeRange], mag: Magnitude) = mr

      val qc2 = CatalogQuery.catalogQueryWithAdjustedRange(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), nilAdjustment, Some(mr))
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
      val qc = CatalogQuery.catalogQueryRangeOnBand(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), Some(mr))

      // Nominal conditions don't change the MagnitudeRange
      val conditions = SPSiteQuality.Conditions.NOMINAL
      def adjustment(mr: Option[MagnitudeRange], mag: Magnitude) =
        mr.map { m =>
          m.adjust(k => conditions.magAdjustOp().apply(mag.copy(value = k).toOldModel).toNewModel.value)
        }

      val qc2 = CatalogQuery.catalogQueryWithAdjustedRange(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), adjustment, Some(mr))
      val filtered = unfiltered.filter(qc)
      val filtered2 = unfiltered.filter(qc2)
      filtered should beEqualTo(filtered2)

      // Filtering on magnitude with nominal conditions doesnt change the output
      filtered.targets.rows should be size 9
    }
    "be able to filter with a band range with nominal conditions" in {
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)

      // Large adjustment leaving all targets out
      def adjustment(mr: Option[MagnitudeRange], mag: Magnitude) =
        mr.map { m =>
          m.adjust(k => k - 20)
        }

      val qc = CatalogQuery.catalogQueryWithAdjustedRange(c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), adjustment, Some(mr))
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude and with the adjustemn takes all the targets out
      filtered.targets.rows should beEmpty
    }
    "be able to filter for Gems" in {
      val mr = MagnitudeRange(FaintnessConstraint(15.0), None)

      // No adjustment
      def adjustment(mr: Option[MagnitudeRange], mag: Magnitude) =
        mr.map { m =>
          m.adjust(k => k )
        }

      val qc = CatalogQuery.catalogQueryForGems(0, c, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(90)), (t: SiderealTarget) => t.magnitudeIn(MagnitudeBand.J), adjustment, Some(mr))
      val filtered = unfiltered.filter(qc)

      // Filtering on magnitude leaves 9 targets
      filtered.targets.rows should be size 9
    }
  }

}
