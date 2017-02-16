package edu.gemini.ags.servlet

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.obs.context.ObsContext

import java.io.{BufferedOutputStream, IOException}
import java.net.URLDecoder
import java.util.concurrent.{ThreadFactory, LinkedBlockingDeque, TimeUnit, ThreadPoolExecutor}
import java.util.logging.{Level, Logger}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import javax.servlet.http.HttpServletResponse.{SC_BAD_GATEWAY, SC_BAD_REQUEST, SC_INTERNAL_SERVER_ERROR, SC_OK}

import scala.concurrent.{ExecutionContext, Await}
import scala.util.{Failure, Success, Try}

object AgsServlet {
  private val Log = Logger.getLogger(getClass.getName)

  // How many concurrent AGS estimations can be running.  Queries block on
  // catalog requests and then do a relatively quick calculation so more threads
  // means we hit the catalog server with more simultaneous requests and
  // throughput goes up. If we increase the thread count too much, the catalog
  // server begins failing and producing HTML 500 and 504 responses.
  val ThreadCount = 16

  type Response = (Int, String)

  def success(est: AgsStrategy.Estimate): Response = (SC_OK, "%.3f".format(est.probability))
  def failure(code: Int, ex: Throwable): Response  = (code, "ERROR: "+ ex.getMessage)

  val guaranteedSuccess: Response = success(AgsStrategy.Estimate.GuaranteedSuccess)
  val completeFailure: Response   = success(AgsStrategy.Estimate.CompleteFailure)

  def send(r: Response, req: HttpServletRequest, res: HttpServletResponse): Unit = {
    res.setStatus(r._1)
    res.setContentType("text/plain; charset=UTF-8")

    Try {
      Option(req.getQueryString).map(s => URLDecoder.decode(s, ToContext.enc(req))).getOrElse("")
    }.foreach { s => Log.log(Level.INFO, s"AGS Estimate: $s => $r") }

    val bos = new BufferedOutputStream(res.getOutputStream)
    try {
      bos.write(r._2.getBytes("UTF-8"))
    } catch {
      case ex: IOException => Log.log(Level.WARNING, "problem sending response", ex)
    } finally {
      bos.close()
    }
  }

  // An executor for use with AGS estimations.  For the most part, threads are
  // only kept alive while there are outstanding AGS estimation requests.
  private val executor = new ThreadPoolExecutor(
                       ThreadCount,
                       ThreadCount,
                       30, TimeUnit.SECONDS,                 // 30 sec timeout
                       new LinkedBlockingDeque[Runnable](),  // unbounded queue
                       new ThreadFactory() {
                         override def newThread(r: Runnable): Thread = {
                           val t = new Thread(r, "AgsServlet Worker")
                           t.setPriority(Thread.NORM_PRIORITY - 1)
                           t.setDaemon(true)
                           t
                         }
                       })

  // Except at the end of the proposal deadline, there will rarely be AGS
  // estimation requests so we might as well let these threads be removed.
  executor.allowCoreThreadTimeOut(true)

  private val executionContext = ExecutionContext.fromExecutor(executor)
}

import AgsServlet._

class AgsServlet(magTable: MagnitudeTable) extends HttpServlet {

  override def doPost(req: HttpServletRequest, res: HttpServletResponse): Unit = estimate(req, res)
  override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = estimate(req, res)

  private def estimate(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    def toContext: Either[Response, ObsContext] =
      Try { ToContext.instance.apply(req) } match {
        // Hack for Gpi.  Gpi uses the science target as a guide star but
        // doesn't actually add any guide stars.  It has no strategy impl.
        case Success(ctx) =>
          Either.cond(ctx.getInstrument.getType != SPComponentType.INSTRUMENT_GPI, ctx, guaranteedSuccess)
        case Failure(ex)  =>
          Left(failure(SC_BAD_REQUEST, ex))
      }

    def strategy(ctx: ObsContext): Either[Response, AgsStrategy] =
      AgsRegistrar.defaultStrategy(ctx).toRight(completeFailure)

    def estimate(ctx: ObsContext, s: AgsStrategy): Either[Response, AgsStrategy.Estimate] = {
      import scala.concurrent.duration._
      Try {
        println(s"*** AgsServlet: estimate ${s.key.displayName}")
        Await.result(s.estimate(ctx, magTable)(executionContext), 2.minutes)
      } match {
        case Success(e)               => Right(e)
        case Failure(io: IOException) => Left(failure(SC_BAD_GATEWAY, io))
        case Failure(t: Throwable)    => Left(failure(SC_INTERNAL_SERVER_ERROR, t))
      }
    }

    send((for {
      ctx <- toContext.right
      s   <- strategy(ctx).right
      e   <- estimate(ctx, s).right
    } yield success(e)).merge, req, res)
  }
}
