package edu.gemini.util.trpc.server

import scalaz._
import Scalaz._
import edu.gemini.util.trpc.common._
import javax.servlet.http.HttpServletRequest
import sun.misc.BASE64Decoder
import java.io.{InvalidClassException, ByteArrayOutputStream, ByteArrayInputStream, ObjectInputStream}
import edu.gemini.util.security.auth.Signed
import java.security.Principal
import edu.gemini.spModel.core.{VersionException, Version}
import edu.gemini.util.security.auth.keychain._

private[server] class RichHttpServletRequest(req: HttpServletRequest) {

  private lazy val pathElems = req.getPathInfo.split("/").drop(1)

  def param(s: String): Try[String] =
    Option(req.getParameter(s)) \/> new IllegalArgumentException("Required request parameter %s was not found.".format(s))

  def payload: Try[(Array[AnyRef], Set[Key])] =
    lift {

      // Get our object stream
      val ios = req.getInputStream.readBase64

      // Check serial compatibility
      try {
        val actualVersion = ios.next[Version]
        if (!Version.current.isCompatible(actualVersion, Version.Compatibility.serial))
          throw new VersionException(Version.current, actualVersion, Version.Compatibility.serial);
      } catch {
        case ice: InvalidClassException =>
          // the version itself is incompatible!
          throw new VersionException(Version.current, Version.Compatibility.serial);
      }

      // Next hunk is our payload
      ios.next[(Array[AnyRef], Set[Key])]

    }

  def path(n: Int): Try[String] =
    pathElems.lift(n) \/> new IllegalArgumentException("Path element %d was not found.".format(n))

}
