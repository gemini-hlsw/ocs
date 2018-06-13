package edu.gemini.auxfile.server.file

import edu.gemini.shared.util.immutable.{ Option => GemOption }
import edu.gemini.shared.util.immutable.ScalaConverters._

import scalaz._
import Scalaz._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import java.time.Instant


object MetaDataSpec extends Specification with SpecBase {

  // The auxfile "FileManager" keeps a root directory in a static variable but
  // we want each test case to be isolated from all the others and to clean up
  // after itself.
  sequential

  def roundTrip[A](
    env:   TestEnv,
    value: TestEnv => A,
    set:   (MetaData, A) => Unit,
    get:   MetaData => A
  ): MatchResult[Any] = {
    val a = value(env)
    set(MetaData.forFile(env.pid, env.fileName), a)
    get(MetaData.forFile(env.pid, env.fileName)) shouldEqual a
  }

  "MetaData" should {
    "round-trip checked" ! {
      forAllMetaData { (env) =>
        roundTrip[Boolean](env, _.checked, _.setChecked(_), _.isChecked)
      }
    }

    "round-trip description" ! {
      forAllMetaData { (env) =>
        roundTrip[String](env, _.description, _.setDescription(_), _.getDescription)
      }
    }

    "round-trip lastEmailed" ! {
      forAllMetaData { (env) =>
        roundTrip[GemOption[Instant]](env, _.lastEmailed.asGeminiOpt, _.setLastEmailed(_), _.getLastEmailed)
      }
    }
  }

}
