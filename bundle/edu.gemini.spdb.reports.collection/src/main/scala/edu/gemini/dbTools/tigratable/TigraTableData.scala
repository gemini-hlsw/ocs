package edu.gemini.dbTools.tigratable

import edu.gemini.pot.client.SPDB
import edu.gemini.sp.vcs2.VcsService
import edu.gemini.util.osgi.SecureServiceFactory

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference

import java.util.{List => JList, Set => JSet}
import java.security.Principal

import scala.collection.JavaConverters._

// Logically this utility is part of TigraTableCreator, but we would need to
// rewrite it in Scala to incorporate the service creation.

object TigraTableData {

  /** Obtains the TigraTable data from the database, assuming a VcsService is
    * available.
    *
    * @return Some(java.util.List[TigraTable]) if a secure VcsService has been
    *         registered; None otherwise
    */
  def get(ctx: BundleContext, user: JSet[Principal]): Option[JList[TigraTable]] =
    SecureServiceFactory.withSecureService(ctx, classOf[VcsService], user.asScala.toSet, None) { vcs =>
      TigraTableFunctor.getTigraTables(SPDB.get(), vcs, user)
    }

  /** Obtains the TigraTable data from the database, assuming a VcsService is
    * available.  (This version is intended to simplify access from Java.)
    *
    * @return a java.util.List[TigraTable] if a secure VcsService has been
    *         registered; null otherwise
    */
  def getOrNull(ctx: BundleContext, user: JSet[Principal]): JList[TigraTable] =
    get(ctx, user).orNull
}
