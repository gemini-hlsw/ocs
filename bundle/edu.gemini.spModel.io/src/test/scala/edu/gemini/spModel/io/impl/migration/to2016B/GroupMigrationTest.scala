package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.pot.sp.{SPComponentType, ISPObservation, ISPProgram}
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.target.env.{ManualGroup, GuideGroup, AutomaticGroup}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.junit.{Assert, Test}

import scala.collection.JavaConverters._


class GroupMigrationTest extends MigrationTest {

  @Test def testGroupMigration(): Unit =
    withTestProgram("groupMigration.xml", { (_, p) => validateProgram(p)})

  private def validateProgram(p: ISPProgram): Unit = {
    val obsList = p.getAllObservations.asScala

    Assert.assertTrue(obsList.size == 3)

    List(validateNoGroups(_), validateOneGroup(_), validateTwoGroups(_)).zip(obsList).foreach { case (f, o) => f(o) }
  }

  private def groups(o: ISPObservation): (Int, List[GuideGroup]) = {
    val n = o.getObsComponents.asScala.find(_.getType == SPComponentType.TELESCOPE_TARGETENV).getOrElse(sys.error("Missing target environment"))
    val e = n.getDataObject.asInstanceOf[TargetObsComp].getTargetEnvironment
    (e.getGuideEnvironment.getPrimaryIndex.intValue, e.getGroups.toList.asScala.toList)
  }

  private def assertAuto(g: GuideGroup): Unit =
    Assert.assertTrue(g.grp.isInstanceOf[AutomaticGroup.Disabled.type])

  private def assertManual(g: GuideGroup): Unit =
    Assert.assertTrue(g.grp.isInstanceOf[ManualGroup])

  private def validateNoGroups(o: ISPObservation): Unit =
    groups(o) match {
      case (0, a :: Nil) => assertAuto(a)
      case _ => Assert.fail("Expected a single disabled automatic group.")
    }

  private def validateOneGroup(o: ISPObservation): Unit =
    groups(o) match {
      case (1, a :: m :: Nil) =>
        assertAuto(a)
        assertManual(m)
      case _                  =>
        Assert.fail("Expected an automatic disabled group and a primary manual group")
    }

  private def validateTwoGroups(o: ISPObservation): Unit =
    groups(o) match {
      case (2, a :: m0 :: m1 :: Nil) =>
        assertAuto(a)
        assertManual(m0)
        assertManual(m1)
      case _                  =>
        Assert.fail("Expected an automatic disabled group and two manual groups")
    }
}
