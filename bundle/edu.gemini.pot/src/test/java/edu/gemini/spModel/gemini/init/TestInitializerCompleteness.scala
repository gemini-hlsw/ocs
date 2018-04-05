package edu.gemini.spModel.gemini.init

import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentBroadType._;
import edu.gemini.pot.sp.SPComponentType;

import org.junit.Assert._
import org.junit.Test

/**
 * Tests that NodeInitializer observation and sequence component initialization
 * maps are complete.
 */
final class TestInitializerCompleteness {

  import TestInitializerCompleteness._

  @Test
  def testObsCompCompleteness(): Unit = {
    ObsComponentTypes.foreach { t =>
      assertTrue(
        s"NodeInitializers is missing obs component $t",
        Option(NodeInitializers.instance.obsComp.get(t)).isDefined
      )
    }
  }

  @Test
  def testSeqCompCompleteness(): Unit = {
    SeqComponentTypes.foreach { t =>
      assertTrue(
        s"NodeInitializers is missing seq component $t",
        Option(NodeInitializers.instance.seqComp.get(t)).isDefined
      )
    }
  }

}


object TestInitializerCompleteness {
  val ObsComponentBroadTypes: Set[SPComponentBroadType] =
    Set(AO, DATA, ENGINEERING, INFO, INSTRUMENT, SCHEDULING, TELESCOPE)

  val ObsComponentTypes: Set[SPComponentType] =
    SPComponentType.values.toSet.filter(t => ObsComponentBroadTypes(t.broadType)) -
      SPComponentType.QPT_CANOPUS -
      SPComponentType.QPT_PWFS

  val SeqComponentBroadTypes: Set[SPComponentBroadType] =
    Set(ITERATOR, OBSERVER)

  val SeqComponentTypes: Set[SPComponentType] =
    SPComponentType.values.toSet.filter(t => SeqComponentBroadTypes(t.broadType))

}
