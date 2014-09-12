package edu.gemini.util.trpc

import common._
import scalaz._
import Scalaz._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.{lang => jl}
import java.lang.reflect.Method

package object server {

  implicit def pimpReq(req: HttpServletRequest) = new RichHttpServletRequest(req)

  implicit class pimpClass[A](c: Class[A]) {

    // Unboxed -> Boxed
    private def boxed: Map[Class[_], Class[_]] = Map(
      jl.Boolean.TYPE   -> classOf[jl.Boolean],
      jl.Byte.TYPE      -> classOf[jl.Byte],
      jl.Character.TYPE -> classOf[jl.Character],
      jl.Double.TYPE    -> classOf[jl.Double],
      jl.Float.TYPE     -> classOf[jl.Float],
      jl.Integer.TYPE   -> classOf[jl.Integer],
      jl.Long.TYPE      -> classOf[jl.Long],
      jl.Short.TYPE     -> classOf[jl.Short])

    // True if param (which may be primitive) is assignable from arg (which is not primitive but may be boxed)
    private def isCompatible(param: Class[_], arg: Class[_]) =
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
