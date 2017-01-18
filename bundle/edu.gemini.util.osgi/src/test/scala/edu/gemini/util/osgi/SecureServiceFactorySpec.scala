package edu.gemini.util.osgi

import edu.gemini.util.osgi.SecureServiceFactory._
import edu.gemini.util.osgi.TestFramework._
import java.security.Principal
import org.osgi.framework._
import org.osgi.framework.launch.Framework
import org.specs2.mutable.Specification

class SecureServiceFactorySpec extends Specification {

  // Our trivial secure service
  type Service = (String, Set[Principal])
  val factory = new SecureServiceFactory[Service] {
    def getService(ps: Set[Principal]): Service =
      ("foo", ps.toSet)
  }

  case class PImpl(getName: String) extends Principal

  // Some Principals
  val ps  = Set[Principal](PImpl("Bob"), PImpl("Steve"))
  val ps2 = Set[Principal](PImpl("Sam"), PImpl("Harry"))

  // Helpers
  def withSecureServiceRegistration[A](props: Map[String, String] = Map())(f: BundleContext => A): A =
    withFramework { bc =>
      bc.registerSecureService(factory, props)
      f(bc)
    }

  "registerSecureService" should {

    "propagate service properties (positive)" in {
      withSecureServiceRegistration(Map("woozle" -> "fnord")) { bc =>
        val filter = SecureServiceFactory.filter("woozle", "fnord")
        bc.withSecureService[Service](ps, filter)(identity) must_== Some(("foo", ps))
      }
    }

    "propagate service properties (negative)" in {
      withSecureServiceRegistration(Map("woozle" -> "fnord")) { bc =>
        val filter = SecureServiceFactory.filter("woozle", "qux")
        bc.withSecureService[Service](ps, filter)(identity) must_== None
      }
    }

    "propagate service properties (wildcard, positive)" in {
      withSecureServiceRegistration(Map("woozle" -> "fnord")) { bc =>
        val filter = SecureServiceFactory.filter("woozle", "*")
        bc.withSecureService[Service](ps, filter)(identity) must_== Some(("foo", ps))
      }
    }

    "propagate service properties (wildcard, negative)" in {
      withSecureServiceRegistration(Map("woozle" -> "fnord")) { bc =>
        val filter = SecureServiceFactory.filter("snoozle", "*")
        bc.withSecureService[Service](ps, filter)(identity) must_== None
      }
    }

  }

  "withSecureService" should {

    "fail on missing service" in {
      withSecureServiceRegistration() { bc =>
        bc.withSecureService[ClassLoader](ps)(identity) must_== None
      }
    }

    "pass principals correctly" in {
      withSecureServiceRegistration() { bc =>
        bc.withSecureService[Service](ps)(identity) must_== Some(("foo", ps))
      }
    }

    "pass principals correctly when called concurrently" in {
      withSecureServiceRegistration() { bc =>
        val Some(Some((a, b))) =
          bc.withSecureService[Service](ps) { case (_, ps) =>
            bc.withSecureService[Service](ps2) { case (_, ps2) => (ps, ps2) }
          }
        (a, b) must_== ((ps, ps2))
      }

    }

  }

  "withSecureServiceByName" should {

    "fail on missing service" in {
      withSecureServiceRegistration() { bc =>
        bc.withSecureServiceByName(classOf[ClassLoader].getName, ps)(identity) must_== None
      }
    }

    "pass principals correctly" in {
      withSecureServiceRegistration() { bc =>
        bc.withSecureServiceByName(classOf[Service].getName, ps)(identity) must_== Some(("foo", ps))
      }
    }

    "pass principals correctly when called concurrently" in {
      withSecureServiceRegistration() { bc =>
        val Some(Some((a, b))) =
          bc.withSecureServiceByName(classOf[Service].getName, ps) { case (_, ps) =>
            bc.withSecureServiceByName(classOf[Service].getName, ps2) { case (_, ps2) => (ps, ps2) }
          }
        (a, b) must_== ((ps, ps2))
      }

    }

  }



}
