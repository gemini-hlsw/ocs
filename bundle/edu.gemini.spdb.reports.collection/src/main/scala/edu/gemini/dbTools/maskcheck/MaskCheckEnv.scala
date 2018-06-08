package edu.gemini.dbTools.maskcheck

import edu.gemini.auxfile.server.AuxFileServer
import edu.gemini.dbTools.mailer.MailerType
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.osgi.SiteProperty;

import org.osgi.framework.BundleContext

import scalaz._
import Scalaz._

final case class MaskCheckEnv(
  auxFileServer: AuxFileServer,
  odb:           IDBDatabaseService,
  mailer:        MaskCheckMailer
)

object MaskCheckEnv {

  val SmtpProp       = "cron.odbMail.SITE_SMTP_SERVER"
  val MailerTypeProp = "cron.odbMail.mailer.type"

  private def service[C](ctx: BundleContext, c: Class[C]): MC[C] =
    for {
      r <- MC.fromNullableOp(s"No ${c.getName} service reference")(ctx.getServiceReference(c))
      s <- MC.fromNullableOp(s"Mo ${c.getName} service"          )(ctx.getService(r))
    } yield s

  private def mailer(ctx: BundleContext): MC[MaskCheckMailer] =
    for {
      s  <- MC.fromNullableOp(s"Missing ${SiteProperty.NAME} property")(SiteProperty.get(ctx))
      sm <- MC.fromNullableOp(s"Missing $SmtpProp property"           )(ctx.getProperty(SmtpProp))
      ts <- MC.fromNullableOp(s"Missing $MailerTypeProp property"     )(ctx.getProperty(MailerTypeProp))
      ty <- MC.fromOption(s"Could not parse MailerType $ts")(MailerType.fromString(ts))
    } yield {
      ty match {
        case MailerType.Production => MaskCheckMailer(s, sm)
        case MailerType.Test       => MaskCheckMailer.forTesting(s)
      }
    }

  def fromBundleContext(ctx: BundleContext): MC[MaskCheckEnv] =
    for {
      a <- service(ctx, classOf[AuxFileServer     ])
      o <- service(ctx, classOf[IDBDatabaseService])
      m <- mailer(ctx)
    } yield MaskCheckEnv(a, o, m)
}
