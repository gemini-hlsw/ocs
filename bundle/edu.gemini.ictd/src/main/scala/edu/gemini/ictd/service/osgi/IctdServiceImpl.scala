package edu.gemini.ictd.service.osgi

import java.security.{AccessControlException, Permission, Principal}

import edu.gemini.ictd.IctdDatabase
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.ictd.{IctdService, IctdSummary}
import edu.gemini.util.security.permission.StaffPermission
import edu.gemini.util.security.policy.ImplicitPolicy

import scalaz.effect.IO

/**
 * A service that runs in the ODB and conducts ICTD queries on behalf of ODB
 * clients.  The service is configured with user/password information from the
 * ODB OSGI activator so that this information isn't distributed to clients like
 * the OT and QPT.
 */
object IctdServiceImpl {

  /**
   * The service method creates the service via a registered OSGi secure service
   * factory (see the Activator start method for more information).
   */
  def service(
    db: IDBDatabaseService,
    gn: IctdDatabase.Configuration,
    gs: IctdDatabase.Configuration,
    ps: Set[Principal]
  ): IctdService =

    new IctdService {

      def config(site: Site): IctdDatabase.Configuration =
        site match {
          case Site.GN => gn
          case Site.GS => gs
        }

      def withPermission[A](p: Permission)(a: => IO[A]): IO[A] =
        ImplicitPolicy.hasPermission(db, ps, p).flatMap { b =>
          if (b) a else IO.throwIO[A](new AccessControlException("permission needed to access ICTD"))
        }

      def action(site: Site): IO[IctdSummary] =
        withPermission(StaffPermission(None)) {
          for {
            fa <- IctdDatabase.feature.select(config(site))
            ms <- IctdDatabase.mask.select(config(site), site)
          } yield IctdSummary(fa.availabilityMap(site).enumMap, ms)
        }

      override def summary(site: Site): IctdSummary =
        action(site).unsafePerformIO
    }
}
