package edu.gemini.util.osgi

import edu.gemini.util.osgi.SecureServiceFactory._
import edu.gemini.util.osgi.TestFramework._
import java.security.Principal
import org.osgi.framework._
import org.osgi.framework.launch.Framework
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

class SecureServiceFactorySpec extends Specification {

  // Our trivial secure service
  type Service = (String, Set[Principal])
  val factory = new SecureServiceFactory[Service] {
    def getService(b: Bundle, reg: ServiceRegistration[Service], ps: java.util.Set[Principal]): Service =
      ("foo", ps.asScala.toSet)
  }

  // Some Principals
  val ps = Set[Principal](
    new Principal { def getName = "Bob" },
    new Principal { def getName = "Steve" }
  )

  // Helpers
  def withSecureServiceRegistration[A](f: BundleContext => A): A =
    withFramework { bc =>
      bc.registerService(classOf[Service].getName, factory, emptyDict)
      f(bc)
    }

  "Secure Service Factory getSecureServices (plural)" should {

    "work fine with normal service registrations" in {
      withFramework { bc =>
        bc.registerService(classOf[String].getName, "foo", emptyDict)
        val ref1 = bc.getServiceReferences(classOf[String], null).iterator.next
        val (ref2, svc) = getSecureServices(bc, classOf[String].getName, null, Set.empty)(0)
        (ref1, bc.getService(ref1)) must_== ((ref2, svc))
      }
    }

    "pass principals correctly via getSecureServices" in {
      withSecureServiceRegistration { bc =>
        val (ref, svc) = getSecureServices(bc, classOf[Service].getName, null, ps).head
        svc must_== (("foo", ps))
      }
    }

    "ignore principals when called normally" in {
      withSecureServiceRegistration { bc =>
        val ref = bc.getServiceReference(classOf[Service].getName)
        bc.getService(ref) must_== (("foo", Set.empty))
      }
    }

  }

  "Secure Service Factory getSecureService (singular)" should {

    "work fine with normal service registrations" in {
      withFramework { bc =>
        bc.registerService(classOf[String].getName, "foo", emptyDict)
        val ref1 = bc.getServiceReferences(classOf[String], null).iterator.next
        val Some((ref2, svc)) = getSecureService(bc, classOf[String].getName, null, Set.empty)
        (ref1, bc.getService(ref1)) must_== ((ref2, svc))
      }
    }

    "pass principals correctly via getSecureServices" in {
      withSecureServiceRegistration { bc =>
        val Some((ref, svc)) = getSecureService(bc, classOf[Service].getName, null, ps)
        svc must_== (("foo", ps))
      }
    }

    "ignore principals when called normally" in {
      withSecureServiceRegistration { bc =>
        val ref = bc.getServiceReference(classOf[Service].getName)
        bc.getService(ref) must_== (("foo", Set.empty))
      }
    }

  }


}

