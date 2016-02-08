package edu.gemini.util.security.auth.keychain

import doobie.imports._
import org.specs2.mutable.Specification
import scalaz._, Scalaz._, effect.IO
import edu.gemini.util.security.principal._
import edu.gemini.spModel.core._
import java.io.File

object KeySchema2Spec extends Specification {
  import KeySchema2._

  val serialId = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis)

  val principals: List[GeminiPrincipal] = 
    List(
      AffiliatePrincipal(Affiliate.CHILE),
      ProgramPrincipal(SPProgramID.toProgramID("GS-2010A-Q-11")),
      StaffPrincipal.Gemini,
      UserPrincipal("bob@dobbs.com"),
      VisitorPrincipal(SPProgramID.toProgramID("GS-2014B-C-2"))
    )

  def go[A](f: ConnectionIO[A]): A = {
    val xa = DriverManagerTransactor[IO]("org.h2.Driver", s"jdbc:h2:mem:ks${serialId.getAndIncrement};DB_CLOSE_DELAY=-1")
    f.transact(xa).ensuring(sql"SHUTDOWN IMMEDIATELY".update.run.transact(xa).attempt).unsafePerformIO
  }

  "compatibility" should {

    "read old database without error" in go {

      val testData: List[(GeminiPrincipal, String, Int)] = 
        List(
          (UserPrincipal("bob@dobbs.com"), "foodle", 1),
          (ProgramPrincipal(SPProgramID.toProgramID("GS-2010A-Q-3")), "blippy", 1),
          (StaffPrincipal.Gemini, "hoox", 2),
          (AffiliatePrincipal(Affiliate.CHILE), "huevon", 123),
          (VisitorPrincipal(SPProgramID.toProgramID("GN-2010-C-3")), "doop", 1)
        )

      for {
        _  <- sql"runscript from 'classpath:/testdb.sql' charset 'utf-8'".update.run
        vs <- testData.traverse { case (p, pass, v) => checkPass(p, pass) } 
      } yield vs must_== testData.map(t => Some(t._3))

    }

  }

  "checkSchema" should {

    "initialize with new database" in go {
      checkSchema("«in memory»") >| true
    }

    "initialize with existing database" in go {
      checkSchema("«in memory»").replicateM(2) >| true
    }
  
  }

  "checkPass" should {

    "return None for unkown principals" in go {
      for {
        _   <- checkSchema("«in memory»")
        kvs <- principals.traverse(checkPass(_, "blah"))
      } yield kvs.forall(_ must_== None)
    }

    "return Some(1) for newly created passwords" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        kvs <- principals.traverse(checkPass(_, "blah"))
      } yield kvs.forall(_ must_== Some(1))
    }

    "return Some(n) for updated passwords" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah")).replicateM(3)
        kvs <- principals.traverse(checkPass(_, "blah"))
      } yield kvs.forall(_ must_== Some(3))
    }

    "return None for revoked keys" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        _   <- principals.traverse(revokeKey)
        kvs <- principals.traverse(checkPass(_, "blah"))
      } yield kvs.forall(_ must_== None)
    }

  }

  "setPass" should {

    "return 1 for newly created passwords" in go {
      for {
        _   <- checkSchema("«in memory»")
        kvs <- principals.traverse(setPass(_, "blah"))
      } yield kvs.forall(_ must_== 1)
    }

    "return n for updated passwords" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        kvs <- principals.traverse(setPass(_, "blah"))
      } yield kvs.forall(_ must_== 2)
    }

    "correctly set initial password" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(p => setPass(p, p.hashCode.toString))
        kvs <- principals.traverse(p => checkPass(p, p.hashCode.toString))
      } yield kvs.forall(_ must_== Some(1))
    }

    "correctly update new password" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(p => setPass(p, p.hashCode.toString))
        _   <- principals.traverse(p => setPass(p, p.hashCode.toString + "-new"))
        kvs <- principals.traverse(p => checkPass(p, p.hashCode.toString + "-new"))
      } yield kvs.forall(_ must_== Some(2))
    }

  }

  "getVersion" should {

     "return None for unkown principals" in go {
      for {
        _   <- checkSchema("«in memory»")
        kvs <- principals.traverse(getVersion)
      } yield kvs.forall(_ must_== None)
    }

    "return Some(1) for newly created passwords" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        kvs <- principals.traverse(getVersion)
      } yield kvs.forall(_ must_== Some(1))
    }

    "return Some(n) for updated passwords" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah")).replicateM(3)
        kvs <- principals.traverse(getVersion)
      } yield kvs.forall(_ must_== Some(3))
    }

    "return None for revoked keys" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        _   <- principals.traverse(revokeKey)
        kvs <- principals.traverse(getVersion)
      } yield kvs.forall(_ must_== None)
    }

  }

  "revokeKey" should {

    "result in None with checkPass" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        _   <- principals.traverse(revokeKey)
        kvs <- principals.traverse(checkPass(_, "blah"))
      } yield kvs.forall(_ must_== None)
    }

    "return None for revoked keys" in go {
      for {
        _   <- checkSchema("«in memory»")
        _   <- principals.traverse(setPass(_, "blah"))
        _   <- principals.traverse(revokeKey)
        kvs <- principals.traverse(getVersion)
      } yield kvs.forall(_ must_== None)
    }

  }

}