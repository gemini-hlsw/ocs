package edu.gemini.util.osgi

import org.osgi.framework._
import java.security.Principal
import scala.util.DynamicVariable
import collection.JavaConverters._

object SecureServiceFactory {

  private val principals: DynamicVariable[Set[Principal]] = 
    new DynamicVariable(Set())

  /**  Equivalent to getServiceReferences followed by getService. */
  def getSecureServices(context: BundleContext, clazz: String, filter: String, ps: Set[Principal]): List[(ServiceReference[_], Any)] = 
    principals.withValue(ps) {
      Option(context.getServiceReferences(clazz, filter)).map(_.toList.map { ref => 
        (ref, context.getService(ref))
      }).getOrElse(Nil)
    }

  /**  Equivalent to getServiceReference followed by getService. */
  def getSecureService(context: BundleContext, clazz: String, filter: String, ps: Set[Principal]): Option[(ServiceReference[_], Any)] = 
    principals.withValue(ps) {
      Option(context.getServiceReferences(clazz, filter)).flatMap(_.headOption).map { ref => 
        (ref, context.getService(ref))
      }
    }

  implicit class BundleContextOps(val bc: BundleContext) extends AnyVal {

    def getSecureServices(clazz: String, filter: String, ps: Set[Principal]): List[(ServiceReference[_], Any)] = 
      SecureServiceFactory.getSecureServices(bc, clazz, filter, ps)

    def getSecureService(clazz: String, filter: String, ps: Set[Principal]): Option[(ServiceReference[_], Any)] = 
      SecureServiceFactory.getSecureService(bc, clazz, filter, ps)

  }

}

abstract class SecureServiceFactory[A <: AnyRef] extends ServiceFactory[A] {

  final def getService(b: Bundle, reg: ServiceRegistration[A]): A = 
    getService(b, reg, SecureServiceFactory.principals.value.asJava)

  def getService(b: Bundle, reg: ServiceRegistration[A], ps: java.util.Set[Principal]): A

  def ungetService(b: Bundle, reg: ServiceRegistration[A], a: A): Unit =
    ()

}

