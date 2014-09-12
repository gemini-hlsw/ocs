package edu.gemini.gsa.client.impl

import java.io.File

object PageScrapper {
  private def scrape(f: File): Either[String, GsaTable] =
    for {
      m <- GsaPageScraper.scrape(f).right
      t <- GsaTable.fromMap(m).right
    } yield t

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Specify the GSA result HTML page file to scape.")
      System.exit(-1)
    }

    scrape(new File(args(0))) match {
      case Left(msg)    => println("Couldn't scrape the file '%s': %s".format(args(0), msg))
      case Right(table) => println(table.datasets.mkString("\n"))
    }
  }
}