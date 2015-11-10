package edu.gemini.dataman.app

import scalaz.{-\/, \/-}

object PidFunctorSpec extends TestSupport {
  "PidFunctor" should {
    "find all pids" ! forAllPrograms { (odb, progs) =>
      val expected = progs.map(_.getProgramID).toSet
      PidFunctor.exec(odb, User) match {
        case \/-(actual) => expected == actual.toSet
        case -\/(f)      => println(f.explain)
                            false
      }
    }
  }

}
