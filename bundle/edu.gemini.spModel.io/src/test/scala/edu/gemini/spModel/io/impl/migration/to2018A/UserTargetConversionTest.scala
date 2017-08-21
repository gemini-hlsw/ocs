package edu.gemini.spModel.io.impl.migration.to2018A

import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.UserTarget
import edu.gemini.shared.util.immutable.ScalaConverters._

import org.specs2.mutable.Specification

class UserTargetConversionTest extends Specification with MigrationTest {
  import UserTargetConversionTest._

  "2018A User Target Conversion" should {

    "Type user targets as 'other'" in withTestProgram2("userTargets.xml") {
      _.allObservations.flatMap(_.findTargetObsComp).exists { toc =>
        val userTargets = toc.getTargetEnvironment.getUserTargets.asScalaList
        (userTargets.size == 2) && userTargets.zip(List(user1, user2)).forall((matches _).tupled)
      }
    }
  }

}

object UserTargetConversionTest {
  val user1 = userTarget("User1", "18:37:11.000", "38:49:49.00")
  val user2 = userTarget("User2", "18:36:42.000", "38:44:21.00")

  private def userTarget(name: String, ra: String, dec: String): UserTarget = {
    val sp = new SPTarget()
    sp.setName(name)

    val rad  = Angle.parseHMS(ra).getOrElse(sys.error(s"Could not parse RA: $ra")).toDegrees
    sp.setRaDegrees(rad)

    val decd = Angle.parseDMS(dec).getOrElse(sys.error(s"Could not parse Dec: $dec")).toDegrees
    sp.setDecDegrees(decd)

    new UserTarget(UserTarget.Type.other, sp)
  }


  def matches(u0: UserTarget, u1: UserTarget): Boolean = {
    val when = new edu.gemini.shared.util.immutable.Some(new java.lang.Long(0))

    def same[A](f: UserTarget => A): Boolean = f(u0) == f(u1)

    same(_.`type`                             ) &&
    same(_.target.getName                     ) &&
    same(_.target.getRaDegrees(when).getValue ) &&
    same(_.target.getDecDegrees(when).getValue)
  }

}
