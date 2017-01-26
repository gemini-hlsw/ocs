package edu.gemini.util.osgi

import org.osgi.framework._
import org.osgi.framework.FrameworkUtil.createFilter
import java.security.Principal
import java.util.{ Set => JSet, Hashtable => JHashtable, Dictionary => JDictionary }
import scala.util.DynamicVariable
import collection.JavaConverters._
import scalaz._, Scalaz._
import scala.reflect.ClassTag


trait SecureServiceFactory[A] {

  def getService(ps: Set[Principal]): A

}

abstract class SecureServiceFactoryForJava[A] extends SecureServiceFactory[A] {

  final def getService(ps: Set[Principal]): A =
    getService(ps.asJava)

  def getService(ps: JSet[Principal]): A

}

object SecureServiceFactory {

  /**
   * When we register a `SecureService[A]` with the container, the registration will have
   * `ServiceProp` bound to the runtime classname of `A`, which is what we use for lookups.
   */
  val ServiceProp = "service"

  /** Filter is a semigroup under conjunction. */
  implicit val SemigroupFilter: Semigroup[Filter] =
    Semigroup.instance((a, b) => createFilter(s"(&$a$b)"))

  /** Construct an equality filter. */
  def filter(key: String, value: String = "*"): Filter =
    createFilter(s"($key=$value)")

  /** Construct an filter binding `ServiceProp` to the name of the given class. */
  def serviceFilter[A](clazz: Class[A]): Filter =
    filter(ServiceProp, clazz.getName)

  /**
   * Look up a service factory for the given clazz with any additional filter conditions, create an
   * instance with the given set of principals, evaluate the given function and return the result,
   * releasing references as needed; or return `None` if the requested service is unavailable.
   */
  def withSecureService[A, B](
    context: BundleContext,
    clazz:   Class[A],
    ps:      Set[Principal],
    ofilter: Option[Filter]
  )(f: A => B): Option[B] = {
    val fcName = classOf[SecureServiceFactory[A]].getName
    val filter = ofilter.foldLeft(serviceFilter(clazz))(_ |+| _).toString
    val orefs  = Option(context.getServiceReferences(fcName,filter))
    val oref   = orefs.flatMap(_.headOption)
    oref.map { ref =>
      val svc = context.getService(ref).asInstanceOf[SecureServiceFactory[A]]
      try f(svc.getService(ps)) finally context.ungetService(ref)
    }
  }


  def withSecureServiceByName[B](
    context: BundleContext,
    clazz:   String,
    ps:      Set[Principal],
    ofilter: Option[Filter]
  )(f: Any => B): Option[B] =
    withSecureService(context, Class.forName(clazz), ps, ofilter)(f)

  /**
   * Register the given `SecureServiceFactory` with the container, using any additional provided
   * service properties, such that instances of `A` can be constructed via `withSecureService`
   * above.
   */
  def registerSecureService[A](
    context: BundleContext,
    factory: SecureServiceFactory[A],
    clazz:   Class[A],
    props:   Map[String, String]
  ): ServiceRegistration[SecureServiceFactory[A]] = {
    val propsʹ = new JHashtable[String, Object]
    props.foreach { case (k, v) => propsʹ.put(k, v) }
    propsʹ.put(ServiceProp, clazz.getName)
    context.registerService(classOf[SecureServiceFactory[A]], factory, propsʹ)
  }

  /**
   * Register the given `SecureServiceFactory` with the container, using any additional provided
   * service properties, such that instances of `A` can be constructed via `withSecureService`
   * above.
   */
  def registerSecureServiceForJava[A](
    context: BundleContext,
    factory: SecureServiceFactory[A],
    clazz:   Class[A],
    props:   JDictionary[String, Object]
  ): ServiceRegistration[SecureServiceFactory[A]] = {
    props.put(ServiceProp, clazz.getName)
    context.registerService(classOf[SecureServiceFactory[A]], factory, props)
  }

  // Some convenience syntax for the methods above.
  implicit class BundleContextOps(val bc: BundleContext) {

    // Partial application of polymorphic method `withSecureService` below.
    // See http://tpolecat.github.io/2015/07/30/infer.html
    class WithSecureServicePartiallyApplied[A](ps: Set[Principal], filter: Option[Filter]) {
      def apply[B](f: A => B)(implicit ev: ClassTag[A]): Option[B] = {
        val clazz = ev.runtimeClass.asInstanceOf[Class[A]]
        SecureServiceFactory.withSecureService(bc, clazz, ps, filter)(f)
      }
    }

    def withSecureService[A](ps: Set[Principal]): WithSecureServicePartiallyApplied[A] =
      new WithSecureServicePartiallyApplied(ps, None)

    def withSecureService[A](ps: Set[Principal], filter: Filter): WithSecureServicePartiallyApplied[A] =
      new WithSecureServicePartiallyApplied(ps, Some(filter))

    def withSecureServiceByName[B](clazz: String, ps: Set[Principal])(f: Any => B): Option[B] =
      SecureServiceFactory.withSecureServiceByName(bc, clazz, ps, None)(f)

    def withSecureServiceByName[B](clazz: String, ps: Set[Principal], filter: Filter)(f: Any => B): Option[B] =
      SecureServiceFactory.withSecureServiceByName(bc, clazz, ps, Some(filter))(f)

    def registerSecureService[A](
      factory: SecureServiceFactory[A],
      props:   Map[String, String] = Map()
    )(implicit ev: ClassTag[A]): ServiceRegistration[SecureServiceFactory[A]] = {
      val clazz = ev.runtimeClass.asInstanceOf[Class[A]]
      SecureServiceFactory.registerSecureService(bc, factory, clazz, props)
    }

  }

}
