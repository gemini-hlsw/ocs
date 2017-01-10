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

  case class PImpl(getName: String) extends Principal

  // Some Principals
  val ps  = Set[Principal](PImpl("Bob"), PImpl("Steve"))
  val ps2 = Set[Principal](PImpl("Sam"), PImpl("Harry"))

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

    "return a new service instance each time requested" in {

      // So, this is probably the root cause of http://jira.gemini.edu:8080/browse/REL-2881
      // When you have a ServiceFactory the framework keeps a refcount for instances and doesn't
      // dispose of them until the refcount goes to zero. This count can be > 1 if there are`
      // simultaneous requests, a simulated below.

      withSecureServiceRegistration { bc =>
        val Some((ref1, svc1)) = getSecureService(bc, classOf[Service].getName, null, ps)

        // Normally we would bc.ungetService(ref1) here before the next request, but if we don't
        // do this the next getSecureService call will return the same instance and the passed
        // principals are ignored.

        val Some((ref2, svc2)) = getSecureService(bc, classOf[Service].getName, null, ps2)
        svc2 must_== (("foo", ps2))

      }
    }

  }


}
