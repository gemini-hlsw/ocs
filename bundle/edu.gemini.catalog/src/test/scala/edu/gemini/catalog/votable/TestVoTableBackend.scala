package edu.gemini.catalog.votable

import java.net.URL

import edu.gemini.catalog.api.{UCAC4, CatalogQuery}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.NonEmptyList

// Backend for tests, reads votable xml files from the classpath
case class TestVoTableBackend(file: String) extends VoTableBackend {
  override val catalogUrls = NonEmptyList(new URL(s"file://$file"))

  override def doQuery(query: CatalogQuery, url: URL)(implicit ec: ExecutionContext) = Future {
    VoTableParser.parse(UCAC4, this.getClass.getResourceAsStream(file)).fold(p => QueryResult(query, CatalogQueryResult(TargetsTable.Zero, List(p))), y => QueryResult(query, CatalogQueryResult(y).filter(query)))
  }
}
