package edu.gemini.util.trpc

import edu.gemini.util.trpc.common._
import scalaz._
import Scalaz._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.{lang => jl}
import java.lang.reflect.Method
import java.io.{InvalidClassException, ByteArrayOutputStream, ByteArrayInputStream, ObjectInputStream}
import edu.gemini.spModel.core.{VersionException, Version}
import edu.gemini.util.security.auth.keychain._

package object server {

  implicit class RichHttpServletRequest(req: HttpServletRequest) {

    lazy val pathElems = req.getPathInfo.split("/").drop(1)

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


  implicit class ClassOps[A](c: Class[A]) {

    // Unboxed -> Boxed
    def boxed: Map[Class[_], Class[_]] = Map(
      jl.Boolean.TYPE   -> classOf[jl.Boolean],
      jl.Byte.TYPE      -> classOf[jl.Byte],
      jl.Character.TYPE -> classOf[jl.Character],
      jl.Double.TYPE    -> classOf[jl.Double],
      jl.Float.TYPE     -> classOf[jl.Float],
      jl.Integer.TYPE   -> classOf[jl.Integer],
      jl.Long.TYPE      -> classOf[jl.Long],
      jl.Short.TYPE     -> classOf[jl.Short])

    // True if param (which may be primitive) is assignable from arg (which is not primitive but may be boxed)
    def isCompatible(param: Class[_], arg: Class[_]) =
      arg == null || (param.isAssignableFrom(arg) || param.isPrimitive && boxed(param) == arg)

    /**
     * Returns a method of the given name that can be invoked with the specified arguments (handling unboxing properly),
     * or throws an exception if no such method exists. The intent is to mimic other such methods on Class.
     */
    @throws(classOf[NoSuchMethodException])
    def getCompatibleMethod(name: String, args: Seq[AnyRef]): Try[Method] = {

      val argTypes: List[Class[_ <: AnyRef]] =
        for {
          a <- ~Option(args).map(_.toList)
        } yield Option(a).map(_.getClass).orNull

      val om = getCompatibleMethod0(c, name, argTypes)
      om.\/>(new NoSuchMethodException("%s.%s(%s)".format(c.getName, name, argTypes.mkString(", "))))
    }

    // Walk up the inheritance tree to find the specified method
    def getCompatibleMethod0(c: Class[_], name: String, argTypes: List[Class[_]]): Option[Method] =
      Option(c).flatMap(_.getDeclaredMethods.filter(_.getName == name).find(_.getParameterTypes.corresponds(argTypes)(isCompatible))
        .orElse(getCompatibleMethod0(c.getSuperclass, name, argTypes)))

  }

}
