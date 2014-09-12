package edu.gemini.util.trpc.server

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import edu.gemini.util.trpc.common._
import java.lang.reflect.InvocationTargetException
import edu.gemini.util.security.auth.keychain._
import edu.gemini.util.security.auth.keychain.Action._
import java.security.{PrivilegedActionException, PrivilegedAction, Principal}
import javax.security.auth.Subject
import scalaz._
import Scalaz._
import edu.gemini.spModel.core.{VersionException, Version}
import java.util.logging.{Level, Logger}
import scala.util.DynamicVariable

abstract class TrpcServlet(auth: KeyService) extends HttpServlet {
  val Log = Logger.getLogger(this.getClass.getName)

  // TODO: we can replace the try/catch stuff with Validation.fromTryCatchThrowable in Scalaz 7.1

  // The idea is that you pass class, method, args and get back a result or a throwable.
  // POST goes to http://server:host/trpc/class/method, where local path /class/method
  // Request payload is an Array[AnyRef] serialized and Base64-encoded
  override def service(req: HttpServletRequest, res: HttpServletResponse) {

    try {

      // Our result object is either an exception or a valid result
      val result:Try[AnyRef] = for {
        c <- req.path(0) // name of our service class
        r <- catching {  // capture any exceptions thrown within, and turn to Failure
          for {
            n  <- req.path(1) // the name of our method
            a  <- req.payload // our argument array
            ps <- subject(a._2)
            a <- withService(c, ps) { t => t.getClass.getCompatibleMethod(n, a._1).map { m =>
                try {
                  m.setAccessible(true) // public stuff isn't visible if the class isn't public
                  m.invoke(t, a._1: _*)
                } catch {
                  case ite:InvocationTargetException => throw ite.getCause // unwrap the exception
                }
              }
            }
          } yield a
        }
      } yield r

      // Either way, send it back.
      closing(res.getOutputStream)(_.writeBase64(result))

    } catch {
      case t: Exception =>
        Log.log(Level.INFO, s"Problem writing response to ${req.getRemoteAddr} for request: class=${req.path(0).getOrElse("")} method=${req.path(1).getOrElse("")}", t)
        throw t
    }

  }

  def subject(ps:Set[Key]): Try[Set[Principal]] = try {
    ps.collect { case a if auth.validateKey(a).isRight => a.get._1 : Principal } .toSet.right
  } catch {
    case e:Exception => e.left
  }

  /**
   * Invoke the specified method on a service of the specified type, returning the result.
   * Caller will handle exceptions.
   */
  protected def withService[B](clazz: String, ps: Set[Principal])(f: Any => B): B

}
