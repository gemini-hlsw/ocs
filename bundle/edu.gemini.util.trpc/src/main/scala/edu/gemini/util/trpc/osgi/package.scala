package edu.gemini.util.trpc

import java.util.logging.Logger
import java.security.Principal
import org.osgi.framework._
import edu.gemini.util.osgi.SecureServiceFactory

package object osgi {

  val Attr = "trpc"
  val Filter = SecureServiceFactory.filter(Attr)
  val Alias = "/%s".format(Attr)
  val Log = Logger.getLogger("edu.gemini.util.trpc.osgi") // hack hack

}
