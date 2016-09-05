package edu.gemini.lchquery.servlet

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.`type`.DisplayableSpType
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scalaz._
import Scalaz._


sealed trait ValueMatcher[A,B] {
  protected def extractor(a: A): Option[B]
  def matcher(expression: String, x: A)
}

abstract class StringValueMatcher[A] extends ValueMatcher[A,String] {
  import ValueMatcher.ToRegex

  override def matcher(expression: String, x: A): Boolean = (for {
    r <- Option(expression).map(_.toRegex)
    m <- Option(x).flatMap(extractor)
  } yield r.findFirstMatchIn(m).isDefined).getOrElse(false)
}


abstract class BooleanValueMatcher[A, B <: DisplayableSpType] extends ValueMatcher[A,B] {
  import ValueMatcher.ToRegex

  protected def transform(s: String): String =
    if (s.equalsIgnoreCase("true")) YesNoType.YES.displayValue()
    else if (s.equalsIgnoreCase("false")) YesNoType.NO.displayValue()
    else s

  override def matcher(expression: String, x: A): Boolean = (for {
    r <- Option(expression).map(transform).map(_.toRegex)
    m <- Option(x).flatMap(extractor).map(_.displayValue)
  } yield r.findFirstMatchIn(m).isDefined).getOrElse(false)
}


abstract class StringListValueMatcher[A] extends ValueMatcher[A,List[String]] {
  import ValueMatcher.ToRegex

  override def matcher(expression: String, x: A): Boolean = (for {
    r <- Option(expression).map(_.toRegex)
    v <- Option(x)
    m <- extractor(v)
  } yield m.exists(s => r.findFirstMatchIn(s).isDefined)).getOrElse(false)
}

sealed case class LchQueryParam[A,B](name: String, v: ValueMatcher[A,B])


object LchQueryParams {
  import ValueMatcher.ToSPProgram

  private val ProgramSemester = LchQueryParam("programSemester", new StringValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[String] = {
      for {
        s <- Option(prog.getProgramID).map(_.stringValue)
        i = s.indexOf('-')
        if i != -1 && s.length > i+6 && s.charAt(i+1) == '2'
      } yield s.substring(i+1, i+6)
    }
  })

  private val ProgramTitle = LchQueryParam("programTitle", new StringValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[String] = prog.toSPProg.map(_.getTitle)
  })

  // TODO: No idea if this is how we want to do it. Maybe we want a comma separated list of names from servlet?
  private val ProgramInvestigatorNames = LchQueryParam("programInvestigatorNames", new StringListValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[List[String]] =
      (for {
        p <- Option(prog)
        gsap1 = p.getDataObject.asInstanceOf[SPProgram].getGsaPhase1Data
        i <- gsap1.getPi :: gsap1.getCois.asScala.toList
      } yield s"${i.getFirst} ${i.getLast}").toList match {
        case Nil => None
        case lst => lst.some
      }
  })

  private val ProgramPiEmail = LchQueryParam("programPiEmail", new StringValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[String] = prog.toSPProg.map(_.getGsaPhase1Data.getPi.getEmail)
  })

  private val ProgramCoiEmails = ???

  private val ProgramAbstract = LchQueryParam("programAbstract", new StringValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[String] = prog.toSPProg.map(_.getGsaPhase1Data.getAbstract.getValue)
  })

  // TODO: Is this right?
  private val ProgramBand = LchQueryParam("programBand", new StringValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[String] = prog.toSPProg.map(_.getQueueBand)
  })

  private val ProgramPartners = ???

  private val ProgramReference = LchQueryParams("programReference", new StringValueMatcher[ISPProgram] {
    override def extractor(prog: ISPProgram): Option[String] = Option(prog).map(_.getProgramID.stringValue)
  })

  private val ProgramActive = LchQueryParams("programActive", new BooleanValueMatcher[ISPProgram,SPProgram.Active] {
    override protected def extractor(a: ISPProgram): Option[Active] = a.toSPProg.flatMap(sp => Option(sp.getActive))
  })


}

object ValueMatcher {
  private[servlet] implicit class ToRegex(val expression: String) extends AnyVal {
    def toRegex: Regex = ("^" +
      (expression.contains("|") ? s"($expression)" | expression).
        replaceAllLiterally("*", ".*").
        replaceAllLiterally("%", ".*").
        replaceAllLiterally("?", ".")
        + "$").r
  }

  private[servlet] implicit class ToSPProgram(val prog: ISPProgram) extends AnyVal {
    def toSPProg: Option[SPProgram] = Option(prog).map(_.getDataObject.asInstanceOf[SPProgram])
  }
}
