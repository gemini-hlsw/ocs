package edu.gemini.model.p1.immutable.transform

import java.io.{StringReader, File}
import xml.XML

import scalaz._
import Scalaz._
import edu.gemini.model.p1.immutable.{ProposalIo, Proposal}

object BatchConverter extends App {
  val usage = """
     Usage: p1converter -version XX.YY -year YYYY -semester [A|B] dir
              """
  if (args.length == 0) {
    println(usage)
  } else {
    val arglist = args.toList
    type OptionMap = Map[Symbol, String]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case year :: value :: tail if year == "-year" => nextOption(map ++ Map('year -> value), tail)
        case year :: value :: tail if year == "-version" => nextOption(map ++ Map('version -> value), tail)
        case semester :: value :: tail if semester == "-semester" => nextOption(map ++ Map('semester -> value.toString), tail)
        case string :: Nil => nextOption(map ++ Map('dir -> string), list.tail)
        case option :: tail => sys.error(s"Unknown option: ${option}")
      }
    }

    val options = nextOption(Map(), arglist)
    println(options)
    System.setProperty("edu.gemini.model.p1.year", options.getOrElse('year, "0"))
    System.setProperty("edu.gemini.model.p1.semester", options.getOrElse('semester, "C"))
    System.setProperty("edu.gemini.model.p1.schemaVersion", options.getOrElse('version, "0"))

    val converted:List[(File, UpConverter.UpConversionResult)] = options.get('dir).map(d => new File(d.toString).listFiles().map(f => f -> UpConverter.upConvert(XML.loadFile(f))).toList).getOrElse(List.empty[(File, UpConverter.UpConversionResult)])
    converted.foreach {p => p._2 match {
      case Failure(_) => Console.println("Error converting " + p._1)
      case Success(s) => XML.save(p._1.getAbsolutePath, XML.loadString(ProposalIo.writeToString(ProposalIo.read(new StringReader(s.root.toString())))), "UTF-8");Console.println("Converted " + p._1)
    }}
  }
}
