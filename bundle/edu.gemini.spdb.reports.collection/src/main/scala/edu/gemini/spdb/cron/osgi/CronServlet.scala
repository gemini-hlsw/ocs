package edu.gemini.spdb.cron.osgi

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import java.util.logging._

import edu.gemini.spdb.cron.{Storage, CronJob}
import Storage.{Temp, Perm}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

import java.util.concurrent.atomic.AtomicInteger
import org.osgi.framework.BundleContext
import edu.gemini.spdb.cron.CronJob
import scala.util.Success
import scala.util.Failure
import java.security.Principal

/**
 * A servlet that maps requests to batch jobs, allowing them to be scheduled by an external
 * entity like `cron`.
 */
class CronServlet(ctx: BundleContext, services: Map[String, Job], tempDir: Temp, permDir: Perm, user: java.util.Set[Principal]) extends HttpServlet {

  // This is all kinds of bad, sorry.
  val newLogger: String => (Logger, Int) = {
    val Pid = new AtomicInteger()
    (key: String) => {
      val pid = Pid.incrementAndGet
      val log = Logger.getLogger("edu.gemini.spdb.cron." + key + "/" + pid)
      log.setFilter(new Filter {
        def isLoggable(p1: LogRecord): Boolean = {
          p1.setMessage(s"$key/$pid: ${p1.getMessage}")
          log.isLoggable(p1.getLevel)
        }
      })
      (log, pid)
    }
  }

  def contextService(alias: String): Option[Job] =
    ctx.getServiceReferences(classOf[CronJob], null)
      .asScala
      .find(_.getProperty(CronJob.ALIAS) == alias)
      .map(ctx.getService[CronJob])
      .map(s => (t: Temp, p: Perm, l: Logger, e: java.util.Map[String, String], user: java.util.Set[Principal]) => s.run(t, p, l, e, user))

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {

    val key = req.getPathInfo.drop(1)

    val sysEnv = System.getProperties.asScala.toMap

    def prefixed(p: String): Map[String, String] =
      sysEnv.collect { case (k, v) if k.startsWith(p) => (k.drop(p.length), v)}

    val env: java.util.Map[String, String] =
      (prefixed("cron.*.") ++ prefixed(s"cron.$key.")).asJava

    //    val env = req.getParameterMap
    //                 .asInstanceOf[java.util.Map[String, Array[String]]]
    //                 .asScala
    //                 .mapValues(_.headOption.orNull)
    //                 .asJava

    // We look at the local service table first, then OSGi-shared services, and finally
    // filter to make sure we're NOT using HTTPS, which is an easy way to guarantee
    // that this feature is only available inside the firewall. We will respond with 404 so
    // there's no indication that the service exists.
    val service: Option[Job] =
      services.get(key)
        .orElse(contextService(key))
        .filterNot(_ => req.isSecure)

    service.fold(resp.sendError(404)) { j =>

    // Each job gets its own logger that prepends the job name and number
      val (log, pid) = newLogger(key)
      log.info(f"scheduled with environment:\n${env.asScala.mkString("  ", "\n  ", "")}")
      Future {
        log.info(f"starting...")
        val start = System.currentTimeMillis
        j(tempDir, permDir, log, env, user)
        System.currentTimeMillis - start
      } onComplete {
        case Success(n) => log.info(f"completed in $n%d ms.")
        case Failure(e) => log.log(Level.SEVERE, f"failed:", e)
      }

      resp.setContentType("text/plain")
      resp.addHeader("X-Gemini-spdb-batch-key", key)
      resp.addHeader("X-Gemini-spdb-batch-pid", pid.toString)
      resp.getWriter.write(s"queued job $key/$pid for execution\n")

    }

  }

}

