package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.dbTools.mailer.MailerType
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.osgi.SiteProperty

import org.osgi.framework.BundleContext

import scalaz._
import Scalaz._
import scalaz.effect.IO


final case class TimingWindowCheckEnv(odb: IDBDatabaseService, mailer: TimingWindowCheckMailer, site: Site)

object TimingWindowCheckEnv {

  val SmtpProp       = "cron.odbMail.SITE_SMTP_SERVER"

  // "development", "production", or "test".
  val MailerTypeProp = "cron.odbMail.mailer.type"

  private def fromNullableOp[A](failMessage: => String)(a: => A): IO[A] =
    fromOption(failMessage)(Option(a))

  private def fromOption[A](failMessage: => String)(oa: => Option[A]): IO[A] =
    oa.fold(IO.throwIO[A](new RuntimeException(failMessage)))(_.point[IO])

  private def service[C](ctx: BundleContext, c: Class[C]): IO[C] =
    for {
      r <- fromNullableOp(s"No ${c.getName} service reference")(ctx.getServiceReference(c))
      s <- fromNullableOp(s"Mo ${c.getName} service"          )(ctx.getService(r))
    } yield s

  private def prop(ctx: BundleContext, n: String): IO[String] =
    fromNullableOp(s"Missing $n property")(ctx.getProperty(n))

  private def parsedProp[A](ctx: BundleContext, n: String)(f: String => Option[A]): IO[A] =
    prop(ctx, n).flatMap { s =>
      fromOption(s"Could not parse $n property value '$s'")(f(s))
    }

  private def mailer(s: Site, ctx: BundleContext): IO[TimingWindowCheckMailer] =
    for {
      m <- prop(ctx, SmtpProp)
      t <- parsedProp(ctx, MailerTypeProp)(MailerType.fromString)
    } yield TimingWindowCheckMailer(t, s, m)

  def fromBundleContext(ctx: BundleContext): IO[TimingWindowCheckEnv] =
    for {
      o <- service(ctx, classOf[IDBDatabaseService])
      s <- parsedProp(ctx, SiteProperty.NAME)(s => Option(Site.tryParse(s)))
      m <- mailer(s, ctx)
    } yield TimingWindowCheckEnv(o, m, s)
}
