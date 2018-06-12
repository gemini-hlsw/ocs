package edu.gemini.dbTools.maskcheck

import edu.gemini.auxfile.server.AuxFileServer
import edu.gemini.dbTools.mailer.MailerType
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.osgi.SiteProperty;

import org.osgi.framework.BundleContext

import java.time.Duration

import scalaz._
import Scalaz._


/**
 * Environment/configuration for mask check nagging emails when an ODF file has
 * been updated but hasn't been checked.
 *
 * @param auxFileServer aux file server used to determine which files to nag
 *                      about
 * @param odb           observing database for looking up active program pids
 *                      and email addresses
 * @param mailer        mailer for sending nagging emails
 * @param nagDelay      how long to wait after an ODF file update before nagging
 */
final case class MaskCheckEnv(
  auxFileServer: AuxFileServer,
  odb:           IDBDatabaseService,
  mailer:        MaskCheckMailer,
  nagDelay:      Duration
)

object MaskCheckEnv {

  val SmtpProp              = "cron.odbMail.SITE_SMTP_SERVER"

  // "development", "production", or "test".
  val MailerTypeProp        = "cron.odbMail.mailer.type"

  // Property that specifies how long to wait before sending a nagging email
  // about an ODF file that needs to be checked.  Should specify a value that
  // can be parsed by `java.time.Duration.parse`.  E.g. "P7D" for 7 days or one
  // week..
  val MaskCheckNagDelayProp = "edu.gemini.dbTools.maskcheck.nagdelay"

  private def service[C](ctx: BundleContext, c: Class[C]): MC[C] =
    for {
      r <- MC.fromNullableOp(s"No ${c.getName} service reference")(ctx.getServiceReference(c))
      s <- MC.fromNullableOp(s"Mo ${c.getName} service"          )(ctx.getService(r))
    } yield s

  private def prop(ctx: BundleContext, n: String): MC[String] =
    MC.fromNullableOp(s"Missing $n property")(ctx.getProperty(n))

  private def parsedProp[A](ctx: BundleContext, n: String)(f: String => Option[A]): MC[A] =
    prop(ctx, n).flatMap { s =>
      MC.fromOption(s"Could not parse $n property value '$s'")(f(s))
    }

  private def mailer(ctx: BundleContext): MC[MaskCheckMailer] =
    for {
      s <- parsedProp(ctx, SiteProperty.NAME)(s => Option(Site.tryParse(s)))
      m <- prop(ctx, SmtpProp)
      t <- parsedProp(ctx, MailerTypeProp)(MailerType.fromString)
    } yield MaskCheckMailer(t, s, m)

  private def nagDelay(ctx: BundleContext): MC[Duration] =
    parsedProp(ctx, MaskCheckNagDelayProp) { s =>
      \/.fromTryCatchNonFatal(Duration.parse(s)).toOption
    }

  def fromBundleContext(ctx: BundleContext): MC[MaskCheckEnv] =
    for {
      a <- service(ctx, classOf[AuxFileServer     ])
      o <- service(ctx, classOf[IDBDatabaseService])
      m <- mailer(ctx)
      d <- nagDelay(ctx)
    } yield MaskCheckEnv(a, o, m, d)
}
