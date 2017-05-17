package jsky.app.ot.gemini.editor.sitequality

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{BorderLayout, Component, Dimension, FlowLayout, Frame}
import java.io.File
import java.text.ParsePosition
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.logging.Logger
import javax.swing._

import edu.gemini.shared.gui.Chooser
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import jsky.app.ot.gemini.editor.sitequality.TimingWindowParser.TimingWindowParseFailure

import scala.collection.JavaConverters._
import scala.util.parsing.combinator.RegexParsers
import scalaz._
import Scalaz._


// Prompt the user for a file and then parse it and return a list of TimingWindows.
// Since the calling class is Java and uses Java components, to get the modality right, we have to use Java Swing / AWT.
// If we try to wrap the AWT component in a Scala Swing component, we are globally modal, which is not what we want.
class TimingWindowImporter(owner: Component) {
  import TimingWindowParser._

  private val log = Logger.getLogger(classOf[TimingWindowImporter].getName)

  // Open the import dialog and perform the import.
  // Errors can be displayed in the TimingWindowParseFailureDialog.
  def openImport(): TimingWindowParseResults =
    new Chooser[TimingWindowImporter]("importer", owner).chooseOpen("Timing Windows (.tw)", "tw").
      fold(TimingWindowParseResults.empty)(parseTimingWindowsFromFile)

  private def parseTimingWindowsFromFile(twf: File): TimingWindowParseResults =
    parseTimingWindows(scala.io.Source.fromFile(twf).getLines.toList, log.some)
}


// Irritating again: must use Java Swing for correct dialog modality from calling Java code.
class TimingWindowParseFailureDialog(owner: Frame, failures: List[TimingWindowParseFailure]) extends JDialog(owner, "Timing Window Parsing Errors", true) {
  dlg =>
  // Error label.
  add(new JLabel("The following timing window entries could not be parsed:") {
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
  }, BorderLayout.NORTH)

  // Table of lines that could not be parsed.
  add(new JPanel(new FlowLayout()) {
    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10))

    val parseFailureTable = {
      val rows = failures.map {
        // Add 1 to start labeling with row 1 instead of row 0.
        case TimingWindowParseFailure(idx, input) => Array[AnyRef](new Integer(idx + 1), input)
      }.toArray
      val cols = Array[AnyRef]("Line#", "Input")

      new JTable(rows, cols) {
        setRowSelectionAllowed(false)
        setColumnSelectionAllowed(false)

        val tc = getColumnModel.getColumn(0)
        tc.setMinWidth(50)
        tc.setMaxWidth(50)
        tc.setPreferredWidth(50)
      }
    }

    val scrollPane = new JScrollPane(parseFailureTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scrollPane.setPreferredSize(new Dimension(500, 116))
    add(scrollPane)
  }, BorderLayout.CENTER)

  // OK button to dismiss dialog.
  add(new JPanel(new FlowLayout()) {
    val okButton = new JButton("OK")
    add(okButton)

    okButton.addActionListener(new ActionListener() {
      override def actionPerformed(evt: ActionEvent): Unit =
        dlg.setVisible(false)
    })
  }, BorderLayout.SOUTH)

  setLocationRelativeTo(getOwner)
  setResizable(false)
  pack()
}


// Parser for TimingWindows.
object TimingWindowParser extends RegexParsers {
  // Whitespace is important, so do not skip it!
  override val skipWhitespace = false

  // Simple parsers.
  // timeDigits must be a valid mm or ss field containing two digits.
  private def whitespace: Parser[String] = """\s+""".r
  private def arbitraryDigits: Parser[Int] = """\d+""".r ^^ { _.toInt }
  private def timeDigits: Parser[Int] = """[0-5]\d""".r ^^ { _.toInt }

  // A general parser to parse a temporal accessor from a DateTimeFormatter.
  // NOTE: DateTimeFormatter spacing is RIGID and must be adhered to exactly.
  private def temporalParser(df: DateTimeFormatter, failMsg: String): Parser[TemporalAccessor] = new Parser[TemporalAccessor] {
    override def apply(in: Input): ParseResult[TemporalAccessor] = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)
      val pos = new ParsePosition(start)
      \/.fromTryCatchNonFatal {
        val temporalAccessor = df.parse(source.toString, pos)
        Success(temporalAccessor, in.drop(pos.getIndex - offset))
      }.getOrElse(Failure(failMsg, in.drop(start - offset)))
    }
  }

  // Parse a date and time in UTC into a Long.
  // Formerly, in SimpleDateFormat, parsing would allow for arbitrary whitespace when a whitespace character was in the format.
  // This is not the case with DateTimeFormatter parsing, so we must pre-process strings to change all whitespace sequences
  // into single spaces.
  private[sitequality] val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"))
  private def dateParser: Parser[Long] =
    temporalParser(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")), "Failed to parse date") ^^ { case ta => Instant.from(ta).toEpochMilli }

  // Convert hhhh:mm:ss as a duration to a Long in ms.
  private def hhmmssToLong(hh: Long, mm: Long, ss: Long = 0): Long =
    1000 * (60 * (60 * hh + mm) + ss)

  // Convenience method for spec testing.
  private[sitequality] def hhmmssStringToLong(hhmmss: String): Long =
    hhmmss.split(':').toList.map(_.toInt) match {
      case hh :: mm :: Nil       => hhmmssToLong(hh, mm)
      case hh :: mm :: ss :: Nil => hhmmssToLong(hh, mm, ss)
      case _                     => -1L
    }

  // Parse an hhhh:mm:ss duration / period and return the result in ms.
  private def hhmmssParser: Parser[Long] = (arbitraryDigits <~ ":") ~ (timeDigits <~ ":") ~ timeDigits ^^ {
    case hh ~ mm ~ ss => hhmmssToLong(hh, mm, ss)
  }

  private def hhmmParser: Parser[Long] = (arbitraryDigits <~ ":") ~ timeDigits ^^ {
    case hh ~ mm => hhmmssToLong(hh, mm)
  }

  // Parse number of repetitions for a timing window:
  // -1  = infinite repetitions.
  //  0  = no repetitions.
  // x>0 = x repetitions.
  private def repetitionsParser: Parser[Int] = """(-1)|0|([1-9]\d*)""".r ^^ { _.toInt }

  // Format:
  // Begin (UTC)       Duration  Repetitions  Period
  // yyyy-MM-dd hh:mm  hh:mm     ####         hhh:mm:ss
  //
  // NOTES:
  // 1. Duration:    00:00 indicates remaining open forever.
  // 2. Repetitions: Optional. Default is -1, i.e. repeat forever, and 0 indicates never repeat.
  //                 Ignored if Duration is forever.
  // 3. Period:      Optional. Default is 00:00:00.
  //                 Ignored if Duration is forever and Repetitions is not present, ignored, or set to never repeat (0).
  def timingWindowParser: Parser[TimingWindow] = dateParser ~ (whitespace ~> hhmmParser) ~ ((whitespace ~> repetitionsParser)?) ~ ((whitespace ~> hhmmssParser)?) <~ (whitespace?) <~ "$".r ^^ {
    case b ~ d ~ r ~ p => new TimingWindow(b, (d == 0) ? -1L | d, r.getOrElse(0), p.getOrElse(0))
  }

  // Timing window parsing results.
  final case class TimingWindowParseFailure(idx: Int, input: String)
  final case class TimingWindowParseResults(successes: List[TimingWindow], failures: List[TimingWindowParseFailure]) {
    def successesAsJava: java.util.List[TimingWindow] = successes.asJava
    def failuresAsJava:  java.util.List[TimingWindowParseFailure] = failures.asJava
  }
  object TimingWindowParseResults {
    lazy val empty = TimingWindowParseResults(Nil, Nil)
  }

  // Method to actually parse the timing windows in a list.
  // Placed here to facilitate access in test cases.
  def parseTimingWindows(twfLines: List[String], logger: Option[Logger] = None): TimingWindowParseResults = {
    // Read in the file, trim lines, convert all whitespace to single characters (this is necessary as per the comment
    // in the temporalParser method in the companion object), determine line numbers, filter out comments (#) and empty
    // lines, and then attempt to parse the rest.
    val results = for {
      (line, idx) <- twfLines.zipWithIndex
      input = """\s+""".r.replaceAllIn(line.trim, " ")
      if !input.isEmpty && !input.startsWith("#")
    } yield {
      val result = parse(timingWindowParser, input)

      logger.foreach { log => result match {
        case Success(_, _) => log.info   (s"TimingWindow parse success: '$input'")
        case _             => log.warning(s"TimingWindow parse fail:    '$input'")
      }}

      (idx, line, result)
    }

    // Partition the results into successes and failures.
    // successes: we want just the list of timing windows.
    // failures:  we want the line numbers and input strings that failed.
    val (successes, failures) = results.partition {
      case (_, _, Success(_, _)) => true
      case _ => false
    }.bimap(
      _.map { case (_, _, Success(tw, _)) => tw },
      _.map { case (idx, input, _) => TimingWindowParseFailure(idx, input) }
    )

    TimingWindowParseResults(successes, failures)
  }
}
