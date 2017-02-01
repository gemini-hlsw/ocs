package edu.gemini.qv.plugin.chart

import edu.gemini.qpt.shared.util.ObsBuilder
import edu.gemini.qv.plugin.data.CategorizedXYValues
import edu.gemini.qv.plugin.filter.core.Filter.{RA, Priorities}
import edu.gemini.spModel.core.{Angle, RightAscension}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{InstGmosSouth, InstGmosNorth}
import edu.gemini.spModel.gemini.nifs.InstNIFS
import edu.gemini.spModel.obs.SPObservation.Priority._
import org.junit.Assert._
import org.junit.Test


/**
 * Test cases for categorized data.
 */
class CategorizedDataTest {

  val ra1hrs: RightAscension = RightAscension.fromHours(1.0)
  val ra3hrs: RightAscension = RightAscension.fromHours(3.0)

  @Test def cat1() {

    val builder = new ObsBuilder
    val observations = Set(
      builder.setRa(ra1hrs).setPriority(LOW).apply,
      builder.setRa(ra1hrs).setPriority(LOW).apply,
      builder.setRa(ra1hrs).setPriority(LOW).apply,
      builder.setRa(ra1hrs).setPriority(HIGH).apply,
      builder.setRa(ra3hrs).setPriority(LOW).apply,
      builder.setRa(ra3hrs).setPriority(HIGH).apply,
      builder.setRa(ra3hrs).setPriority(LOW).apply,
      builder.setRa(ra3hrs).setPriority(MEDIUM).apply
    )

    val catData = CategorizedXYValues(Axis.RA1.groups, Axis.Priorities.groups, observations, Chart.ObservationCount.value)
    assertEquals(3, catData.activeYGroups.size)
    assertEquals(3, catData.value(RA(1, 2), Priorities(Set(LOW))), 0.01)
    assertEquals(2, catData.value(RA(3, 4), Priorities(Set(LOW))), 0.01)

  }

  @Test def cat2() {

    val builder = new ObsBuilder
    val observations = Set(
      builder.setRa(ra1hrs).setInstrument(InstGmosNorth.SP_TYPE).apply,
      builder.setRa(ra1hrs).setInstrument(InstGmosSouth.SP_TYPE).apply,
      builder.setRa(ra1hrs).setInstrument(Flamingos2.SP_TYPE).apply,
      builder.setRa(ra1hrs).setInstrument(InstGmosNorth.SP_TYPE).apply,
      builder.setRa(ra3hrs).setInstrument(InstGmosNorth.SP_TYPE).apply,
      builder.setRa(ra3hrs).setInstrument(InstGmosNorth.SP_TYPE).apply,
      builder.setRa(ra3hrs).setInstrument(InstNIFS.SP_TYPE).apply,
      builder.setRa(ra3hrs).setInstrument(InstGmosNorth.SP_TYPE).apply
    )

    val catData = CategorizedXYValues(Axis.RA1.groups, Axis.Instruments.groups, observations, Chart.ObservationCount.value)
    assertEquals(4, catData.activeYGroups.size)

  }

}
