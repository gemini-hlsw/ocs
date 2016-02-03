package edu.gemini.seqexec.web.server

import edu.gemini.seqexec.web.common.Comment
import org.http4s.HttpService
import org.http4s.dsl._

import upickle.default._

object RestRoutes {

  val service = HttpService {
    case req @ GET -> Root / "comments" => println("heret");try {
      Ok(write("Carlos"))
    } catch {
      case e => e.printStackTrace();Ok()
    }
  }
}
