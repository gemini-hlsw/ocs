package edu.gemini.gsa.client.impl

import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}

import java.io.{InputStream, File}
import scala.collection.JavaConverters._
import java.net.URL

/**
 * Extracts the data from a GSA dataset query results page.
 */
object GsaPageScraper {
  val emptyMap: GsaMap = GsaColumn.ALL.map(_.name -> Nil).toMap

  def scrape(f: File): Either[String, GsaMap]                  = scrape(Jsoup.parse(f, "UTF-8"))
  def scrape(url: URL, timeoutms: Int): Either[String, GsaMap] = scrape(Jsoup.parse(url, timeoutms))
  def scrape(is: InputStream): Either[String, GsaMap]          = scrape(Jsoup.parse(is, "UTF-8", ""))

  private def scrape(doc: Document): Either[String, GsaMap] =
    if (hasNoData(doc)) Right(emptyMap) else scrapeDoc(doc)

  // When it legitimately has no data, the service returns a message saying
  // "No data returned !"
  private def hasNoData(doc: Document): Boolean =
    !doc.select(":contains(No data returned)").isEmpty

  // If it doesn't return "No data returned" then we expect it to have a table
  // with a "Mark" column.  We scrape everything from that table.
  private def scrapeDoc(doc: Document): Either[String, GsaMap] =
    (for {
      headerElem <- headerRow(doc)
      tableElem  <- Option(headerElem.parent)
    } yield table(tableElem, headerElem)).toRight("Sorry, the GSA dataset query service returned an unexpected result.")

  // The header row is the table row containing a "th" element with text "Mark",
  // if any.
  private def headerRow(doc: Document): Option[Element] = {
    val elems = doc.getElementsByTag("th").asScala.toList
    elems.find(!_.select(":contains(Mark)").isEmpty).flatMap(e => Option(e.parent))
  }

  private def rowToList(row: Element): List[String] =
    row.children().asScala.toList.map(_.text.trim)

  private def table(tableElem: Element, headerElem: Element): GsaMap = {
    val columnNames  = rowToList(headerElem)
    val dataRowElems = tableElem.children().asScala.toList.tail
    val data = dataRowElems map { rowToList }
    columnNames.zip(data.transpose).toMap
  }
}