package edu.gemini.auxfile.server.file

import edu.gemini.auxfile.server.AuxFileServer
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.shared.util.immutable.{ Option => GemOption }
import edu.gemini.shared.util.immutable.ScalaConverters._

import scalaz._
import Scalaz._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import java.time.Instant
import java.util.Collection

import scala.collection.JavaConverters._

object FileSystemSpec extends Specification with SpecBase {

  // The auxfile "FileManager" keeps a root directory in a static variable but
  // we want each test case to be isolated from all the others and to clean up
  // after itself.
  sequential

  def roundTrip[A](
    env:   TestEnv,
    value: TestEnv => A,
    set:   (AuxFileServer, SPProgramID, Collection[String], A) => Unit,
    get:   MetaData => A
  ): MatchResult[Any] = {
    val a = value(env)
    val s = new BackendFileSystemImpl
    set(s, env.pid, List(env.fileName).asJava, a)
    get(s.getMetaData(env.pid, env.fileName)) shouldEqual a
  }

  "BackendFileSystemImpl" should {
    "round-trip checked" ! {
      forAllMetaData { (env) =>
        roundTrip[Boolean](env, _.checked, _.setChecked(_, _, _), _.isChecked)
      }
    }

    "round-trip description" ! {
      forAllMetaData { (env) =>
        roundTrip[String](env, _.description, _.setDescription(_, _, _), _.getDescription)
      }
    }

    "round-trip lastEmailed" ! {
      forAllMetaData { (env) =>
        roundTrip[GemOption[Instant]](env, _.lastEmailed.asGeminiOpt, _.setLastEmailed(_, _, _), _.getLastEmailed)
      }
    }

  }

}
