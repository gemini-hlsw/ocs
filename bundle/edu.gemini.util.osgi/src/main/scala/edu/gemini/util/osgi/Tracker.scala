package edu.gemini.util.osgi

import org.osgi.util.tracker.ServiceTracker
import org.osgi.framework.{BundleContext, ServiceReference}

/**
 * A utility for tracking services registered with the OSGi service registry.
 */
object Tracker {

  /**
   * Tracks a single service performing the provided <code>setup</code> function
   * when it becomes available and <code>cleanup</code> when it becomes
   * unavailable.
   *
   * @param ctx BundleContext
   * @param setup action to perform when the service is available, return
   *              value is passed to cleanup when the service becomes
   *              unavailable
   * @param cleanup action to perform when the service becomes unavailable
   * @tparam A type of service to track
   * @tparam Z return type of the setup function and argument to cleanup
   *
   * @return service tracker that tracks a service
   */
  def track[A:Manifest,Z](ctx: BundleContext)(setup: A => Z)(cleanup: Z => Unit): ServiceTracker[A, Z] = {
    new ServiceTracker[A,Z](ctx, manifest[A].runtimeClass.asInstanceOf[Class[A]], null) {
      override def addingService(ref: ServiceReference[A]): Z = setup(ctx.getService(ref))
      override def removedService(ref: ServiceReference[A], srv: Z) {
        cleanup(srv)
        ctx.ungetService(ref)
      }
    }
  }

  private class MultiTrack[A:Manifest](ctx: BundleContext, delegate: A => ServiceTracker[_,_]) extends ServiceTracker[A,ServiceTracker[_,_]](ctx, manifest[A].runtimeClass.asInstanceOf[Class[A]], null) {
    override def addingService(ref: ServiceReference[A]): ServiceTracker[_,_] = {
      val t = delegate(ctx.getService(ref))
      t.open()
      t
    }
    override def removedService(ref: ServiceReference[A], srv: ServiceTracker[_,_]) {
      srv.close()
      ctx.ungetService(ref)
    }
  }

  /**
   * Tracks two services performing the provided <code>setup</code> function
   * when both are available and <code>cleanup</code> when any becomes
   * unavailable.
   *
   * @param ctx BundleContext
   * @param setup action to perform when all services are available, return
   *              value is passed to cleanup when any service becomes
   *              unavailable
   * @param cleanup action to perform when any service becomes unavailable
   * @tparam A type of first service to track
   * @tparam B type of second service to track
   * @tparam Z return type of the setup function and argument to cleanup
   *
   * @return service tracker that tracks two services
   */
  def track[A:Manifest,B:Manifest,Z](ctx: BundleContext)(setup: (A,B) => Z)(cleanup: Z => Unit): ServiceTracker[A, _] =
    new MultiTrack[A](ctx, a => track[B,Z](ctx)(setup(a,_))(cleanup))

  /**
   * Tracks three services performing the provided <code>setup</code> function
   * when all are available and <code>cleanup</code> when any becomes
   * unavailable.
   *
   * @param ctx BundleContext
   * @param setup action to perform when all services are available, return
   *              value is passed to cleanup when any service becomes
   *              unavailable
   * @param cleanup action to perform when any service becomes unavailable
   * @tparam A type of first service to track
   * @tparam B type of second service to track
   * @tparam C type of third service to track
   * @tparam Z return type of the setup function and argument to cleanup
   *
   * @return service tracker that tracks three services
   */
  def track[A:Manifest,B:Manifest,C:Manifest,Z](ctx: BundleContext)(setup: (A,B,C) => Z)(cleanup: Z => Unit): ServiceTracker[A, _] =
    new MultiTrack[A](ctx, a => track[B,C,Z](ctx)(setup(a,_,_))(cleanup))

  /**
   * Tracks four services performing the provided <code>setup</code> function
   * when all are available and <code>cleanup</code> when any becomes
   * unavailable.
   *
   * @param ctx BundleContext
   * @param setup action to perform when all services are available, return
   *              value is passed to cleanup when any service becomes
   *              unavailable
   * @param cleanup action to perform when any service becomes unavailable
   * @tparam A type of first service to track
   * @tparam B type of second service to track
   * @tparam C type of third service to track
   * @tparam D type of fourth service to track
   * @tparam Z return type of the setup function and argument to cleanup
   *
   * @return service tracker that tracks four services
   */
  def track[A:Manifest,B:Manifest,C:Manifest,D:Manifest,Z](ctx: BundleContext)(setup: (A,B,C,D) => Z)(cleanup: Z => Unit): ServiceTracker[A, _] =
    new MultiTrack[A](ctx, a => track[B,C,D,Z](ctx)(setup(a,_,_,_))(cleanup))

  /**
   * Tracks five services performing the provided <code>setup</code> function
   * when all are available and <code>cleanup</code> when any becomes
   * unavailable.
   *
   * @param ctx BundleContext
   * @param setup action to perform when all services are available, return
   *              value is passed to cleanup when any service becomes
   *              unavailable
   * @param cleanup action to perform when any service becomes unavailable
   * @tparam A type of first service to track
   * @tparam B type of second service to track
   * @tparam C type of third service to track
   * @tparam D type of fourth service to track
   * @tparam E type of fifth service to track
   * @tparam Z return type of the setup function and argument to cleanup
   *
   * @return service tracker that tracks five services
   */
  def track[A:Manifest,B:Manifest,C:Manifest,D:Manifest,E:Manifest,Z](ctx: BundleContext)(setup: (A,B,C,D,E) => Z)(cleanup: Z => Unit): ServiceTracker[A, _] =
    new MultiTrack[A](ctx, a => track[B,C,D,E,Z](ctx)(setup(a,_,_,_,_))(cleanup))

}
