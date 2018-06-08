package edu.gemini.dbTools.mailer

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp.SPProgram
import javax.mail.internet.InternetAddress

import scalaz._
import Scalaz._

case class ProgramAddresses(
  pi:  List[InternetAddress],
  ngo: List[InternetAddress],
  cs:  List[InternetAddress]
)

object ProgramAddresses {
  val Separators: Array[Char] =
    Array(' ', ',', ';', '\t')

  def fromDataObject(sp: SPProgram): ValidationNel[String, ProgramAddresses] = {

    def toInternetAddress(s: String): ValidationNel[String, InternetAddress] =
      Validation.fromTryCatchNonFatal {
        new InternetAddress(s) <| (_.validate)
      }.leftMap { _ => s"Invalid email address $s".wrapNel }

    def addresses(s: String): ValidationNel[String, List[InternetAddress]] =
      s.split(Separators).map(_.trim).filter(_.nonEmpty).toList.traverseU(toInternetAddress)

    def nullableAddresses(f: SPProgram => String): ValidationNel[String, List[InternetAddress]] =
      Option(f(sp)).fold(List.empty[InternetAddress].successNel[String])(addresses)

    val pi  = nullableAddresses(_.getPIInfo.getEmail)
    val ngo = nullableAddresses(_.getPrimaryContactEmail)
    val cs  = nullableAddresses(_.getContactPerson)

    (pi |@| ngo |@| cs)(ProgramAddresses.apply)

  }

  def fromProgram(sp: ISPProgram): ValidationNel[String, ProgramAddresses] =
    fromDataObject(sp.getDataObject.asInstanceOf[SPProgram])

  def fromProgramId(odb: IDBDatabaseService, pid: SPProgramID): Option[ValidationNel[String, ProgramAddresses]] =
    Option(odb.lookupProgramByID(pid)).map(fromProgram)

}