package edu.gemini.util.trpc.client

import edu.gemini.spModel.core.{Version, Peer}
import edu.gemini.util.security.auth.keychain._
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.ssl.GemSslSocketFactory
import edu.gemini.util.trpc.common._

import java.io.IOException
import java.lang.reflect.{UndeclaredThrowableException, Proxy, Method, InvocationHandler}
import java.net.URL
import java.util.logging.{Level, Logger=>JLogger}
import javax.net.ssl.{SSLSession, HostnameVerifier, HttpsURLConnection}
import javax.servlet.http.HttpServletResponse

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._
import System.currentTimeMillis

object TrpcClient {
  private val Log  = JLogger.getLogger(classOf[TrpcClient].getName)
  private val Warn = 1000 // transactions > this many ms get a warning log

  val ConnectTimeout = 20 * 1000
  val ReadTimeout    = 0

  /*
  private lazy val socketFactory: SSLSocketFactory = {
    // trust store only contains the public key so the password protection seems
    // sort of irrelevant anyway
    def loadTrustStore: KeyStore = {
      val ks = KeyStore.getInstance(KeyStore.getDefaultType)
      ks.load(TrpcClient.getClass.getResourceAsStream("gemTrustStore"), "_Curry1!".toCharArray)
      ks
    }

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(loadTrustStore)

    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, tmf.getTrustManagers, null)
    ctx.getSocketFactory
  }
*/
  private val hostnameVerifier: HostnameVerifier = new HostnameVerifier {
     def verify(s: String, sslSession: SSLSession) = true
  }

  class ClientBuilder(host: String, port: Int, connectTimeout: Int, readTimeout: Int) {

    def withKeys(keys: Set[Key]): TrpcClient = 
      new TrpcClient(host, port, connectTimeout, readTimeout, keys)

    def withoutKeys: TrpcClient = 
      withKeys(Set())

    def withKeyChain(kc: KeyChain): TrpcClient =
      withKeys(kc.selection.unsafeRunAndThrow.map(_._2).toSet)

    def withOptionalKeyChain(okc: Option[KeyChain]): TrpcClient =
      okc.map(withKeyChain).getOrElse(withoutKeys)

  }

  def apply(host: String, port: Int): ClientBuilder = 
    apply(host, port, ConnectTimeout, ReadTimeout)

  def apply(peer:Peer): ClientBuilder = 
    apply(peer.host, peer.port, ConnectTimeout, ReadTimeout)

  def apply(peer:Peer, connectTimeout: Int, readTimeout: Int): ClientBuilder =
    apply(peer.host, peer.port, connectTimeout, readTimeout)

  def apply(host: String, port: Int, connectTimeout: Int, readTimeout: Int): ClientBuilder = 
    new ClientBuilder(host, port, connectTimeout, readTimeout)

}

class TrpcClient private (host: String, port: Int, connectTimeout: Int, readTimeout: Int, keys: Set[Key]) {
  import TrpcClient._

  /**
   * A remote proxy broker, which may be coerced into any <i>interface</i> type; this coercion triggers the creation of
   * a dynamic proxy whose method calls are forwarded to the remote host. See TrpcClient.apply() below.
   */
  trait Remote {
    def apply[A: Manifest]: A
  }

  /**
   * Similar to `apply` but returns a `Future[A]` rather than a `Try[A]`.
   * This mode of use is preferred as it's non-blocking.
   */
  def future[A](f: Remote => A)(implicit ec:ExecutionContext): Future[A] = 
    Future(apply(f).get)

  /**
   * Scala clients use this form, which firewalls any exceptions that escape the interaction. Undeclared throwables
   * are unwrapped. Invoke as
   * <code>
   * val result = TrpcClient(host, port) { remote => remote[IFoo].doSomething() ... }
   * </code>
   **/
  def apply[A](f: Remote => A): Try[A] = try {
    f(new Remote {
      def apply[A: Manifest]: A = proxy[A]
    }).right
  } catch {
    case ute: UndeclaredThrowableException => try {
      throw ute.getCause
    } catch {
      case e: Exception => e.left
    }
    case e: Exception => e.left
  }

  /**
   * Java clients use this form, which constructs a "naked" proxy that can throw UndeclaredThrowableException, which
   * must be trapped explicitly. Invoke as
   * <code>
   * try {
   * IFoo foo = new TrpcClient(host, port).proxy(IFoo.class);
   * foo.doSomething();
   * ...
   * } catch (UndeclaredThrowableException ute) {
   * // This will typically be an IOException
   * throw ute.getCause();
   * }
   * </code>
   */
  def proxy[A](c: Class[A]): A = proxy(Manifest.classType(c))

  private def proxy[A](implicit m: Manifest[A]): A = {
    val handler = new InvocationHandler {

      def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef = {
        val start = currentTimeMillis
        try {
          val url = "https://%s:%d/trpc/%s/%s".format(host, port, m.erasure.getName, method.getName)
          val conn = new URL(url).openConnection.asInstanceOf[HttpsURLConnection]
          conn.setSSLSocketFactory(GemSslSocketFactory.get)
          conn.setHostnameVerifier(hostnameVerifier)
          conn.setConnectTimeout(connectTimeout)
          conn.setChunkedStreamingMode(1024 * 16) // 16k blocks (?)
          conn.setDoOutput(true)
          conn.setDoInput(true)

          conn.setReadTimeout(readTimeout)

          // val ps: Set[Key] = auth.flatMap(_.selection.unsafeRunAndThrow.map(_._2)).toSet
          // //auth.map(_.signedPrincipals.fold(throw _, identity)).getOrElse(Set.empty)

          if (Log.isLoggable(Level.FINE))
            Log.fine("Sending %d principals:".format(keys.size) + keys.map(p => "\n\t" + p))

          closing(conn.getOutputStream)(_.writeBase64(Version.current, (args, keys))) // note that args may be null
          conn.getResponseCode match {
            case HttpServletResponse.SC_OK => closing(conn.getInputStream)(_.readBase64.next[Try[AnyRef]]) match {
              case \/-(a) => a
              case -\/(e) =>
                val localFrames = new Exception().getStackTrace.drop(2) // throw away the proxy frames (?)
                val markerFrame = new StackTraceElement("***** EXCEPTION THROW FROM SERVER", "", "<none>", 0)
                e.setStackTrace(localFrames ++ Array(markerFrame) ++ e.getStackTrace)
                throw e
            }
            case code => throw new IOException("%d %s: %s".format(code, conn.getResponseMessage, url)) // can we do better?
          }
        } finally {
          val elapsed = currentTimeMillis - start
          val level = if (elapsed > Warn) Level.WARNING else Level.FINE
          if (Log.isLoggable(level))
            Log.log(level, "%s on %s:%d took %d ms.".format(method, host, port, elapsed))
        }
      }

    }
    //    val contextClassLoader: ClassLoader = Thread.currentThread().getContextClassLoader()
    Proxy.newProxyInstance(getClass.getClassLoader, Array(m.erasure), handler).asInstanceOf[A]
  }

}
