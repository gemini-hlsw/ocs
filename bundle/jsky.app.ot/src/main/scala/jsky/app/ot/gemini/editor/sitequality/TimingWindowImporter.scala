package jsky.app.ot.gemini.editor.sitequality

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{BorderLayout, Component, Dialog, FlowLayout}
import java.io.File
import java.text.ParsePosition
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Collections
import java.util.logging.Logger
import javax.swing._

import edu.gemini.shared.gui.Chooser
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow

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

  def promptImport(): java.util.List[TimingWindow] = {
    val fileChooser = new Chooser[TimingWindowImporter]("importer", owner)
    fileChooser.chooseOpen("Timing Windows (.tw)", "tw").
      fold(Collections.emptyList[TimingWindow]())(f => parseTimingWindowsFromFile(f).asJava)
  }

  private def parseTimingWindowsFromFile(twf: File): List[TimingWindow] = {
    val results = parseTimingWindows(scala.io.Source.fromFile(twf).getLines.toList, log.some)

    val failures = results.failures
    if (failures.nonEmpty)
      new ParseFailureDialog(failures).setVisible(true)
    results.successes
  }

  // Irritating again: must use JDialog.
  private class ParseFailureDialog(failures: List[TimingWindowParseFailure])
    extends JDialog(JOptionPane.getFrameForComponent(owner), "Timing Window Parsing Errors", Dialog.ModalityType.APPLICATION_MODAL) {
    dlg =>
    add(new JLabel("The following timing window entries could not be parsed:"), BorderLayout.NORTH)

    val parseFailureTable = {
      val rows = failures.map {
        case TimingWindowParseFailure(idx, input) => Array[AnyRef](new Integer(idx), input)
      }.toArray
      val cols = Array[AnyRef]("Row #", "Input")
      new JTable(rows, cols) {
        setRowSelectionAllowed(false)
        setColumnSelectionAllowed(false)
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        getColumnModel.getColumn(0).setPreferredWidth(50)
        getColumnModel.getColumn(1).setPreferredWidth(400)
      }
    }
    add(new JScrollPane(parseFailureTable), BorderLayout.CENTER)

    val okButton = new JButton("OK")
    val buttonPanel = new JPanel(new FlowLayout)
    buttonPanel.add(okButton)
    add(buttonPanel, BorderLayout.SOUTH)

    okButton.addActionListener(new ActionListener() {
      override def actionPerformed(evt: ActionEvent): Unit = {
        dlg.setVisible(false)
      }
    })

    pack()
  }
}

object TimingWindowParser extends RegexParsers {
  // Whitespace is important, so do not skip it!
  override val skipWhitespace = false

  // Simple parsers.
  private def whitespace: Parser[String] = """\s+""".r
  private def arbitraryDigits: Parser[Int] = """\d+""".r ^^ { _.toInt }
  private def twoDigits: Parser[Int] = """\d{2}""".r ^^ { _.toInt }

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
  private def dateParser: Parser[Long] =
    temporalParser(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")), "Failed to parse date") ^^ { case ta => Instant.from(ta).toEpochMilli }

  // Convert hhhh:mm:ss as a duration to a Long in ms.
  private def hhmmssToLong(hh: Int, mm: Int, ss: Int = 0): Long =
    1000 * (60 * (60 * hh + mm) + ss)

  // Parse an hhhh:mm:ss duration / period and return the result in ms.
  private def hhmmssParser: Parser[Long] = (arbitraryDigits <~ ":") ~ (twoDigits <~ ":") ~ twoDigits ^^ {
    case hh ~ mm ~ ss => hhmmssToLong(hh, mm, ss)
  }

  private def hhmmParser: Parser[Long] = (arbitraryDigits <~ ":") ~ twoDigits ^^ {
    case hh ~ mm => hhmmssToLong(hh, mm)
  }

  // Parse number of repetitions for a timing window:
  // -1  = infinite repetitions.
  //  0  = no repetitions.
  // x>0 = x repetitions.
  private def repetitionsParser: Parser[Int] = """(-1)|0|([1-9]\d*)""".r ^^ { _.toInt }

  def timingWindowParser: Parser[TimingWindow] = dateParser ~ ((whitespace ~> hhmmParser)?) ~ ((whitespace ~> repetitionsParser)?) ~ ((whitespace ~> hhmmssParser)?) <~ (whitespace?) <~ "$".r ^^ {
    case b ~ d ~ r ~ p => new TimingWindow(b, d.getOrElse(-1), r.getOrElse(0), p.getOrElse(0))
  }

  // Timing window parsing results.
  final case class TimingWindowParseFailure(idx: Int, input: String)
  final case class TimingWindowResults(successes: List[TimingWindow], failures: List[TimingWindowParseFailure])

  // Method to actually parse the timing windows in a list.
  // Placed here to facilitate access in test cases.
  def parseTimingWindows(twfLines: List[String], logger: Option[Logger] = None): TimingWindowResults = {
    // Read in the file, trim lines, convert all whitespace to single characters (this is necessary as per the commment
    // in the temporalParser method in the companion object), determine line numbers, filter out comments (#) and empty
    // lines, and then attempt to parse the rest.
    val results = for {
      l <- twfLines.map(s => """\s+""".r.replaceAllIn(s.trim, " ")).zipWithIndex
      (input, idx) = l
      if !input.isEmpty && !input.startsWith("#")
    } yield {
      val result = parse(timingWindowParser, input)

      logger.foreach { log => result match {
        case Success(_, _) => log.info   (s"TimingWindow parse success: '$input'")
        case _             => log.warning(s"TimingWindow parse fail:    '$input'")
      }}

      (idx, input, result)
    }

    // Partition the results into successes and failures.
    // successes: we want just the list of timing windows.
    // failures:  we want the line numbers and input strings.
    val (successes, failures) = results.partition {
      case (_, _, Success(_, _)) => true
      case _ => false
    }.bimap(
      _.map { case (_, _, Success(tw, _)) => tw },
      _.map { case (idx, input, _) => TimingWindowParseFailure(idx, input) }
    )

    TimingWindowResults(successes, failures)
  }
}
