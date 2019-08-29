package edu.gemini.catalog.votable

import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.api.CatalogName.UCAC4
import edu.gemini.spModel.core.SiderealTarget

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.NonEmptyList

/**
 * Backend for tests, reads votable xml files from the classpath.  Applies a
 * provided modification function to each parsed target.
 */
case class TestVoTableBackend(
    file: String,
    mod:  SiderealTarget => SiderealTarget
) extends VoTableBackend {

  override val catalogUrls: NonEmptyList[URL] =
    NonEmptyList(new URL(s"file://$file"))

  override def doQuery(query: CatalogQuery, url: URL)(ec: ExecutionContext) = Future {
    VoTableParser.parse(UCAC4, this.getClass.getResourceAsStream(file))
                 .map(pvo => ParsedVoResource(pvo.tables.map(pt => ParsedTable(pt.rows.map(_.map(mod))))))
                 .fold(p => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(p))),
                       y => QueryResult(query, CatalogQueryResult(y).filter(query)))
  }

}

object TestVoTableBackend {
  def apply(file: String): TestVoTableBackend =
    TestVoTableBackend(file, identity)
}
