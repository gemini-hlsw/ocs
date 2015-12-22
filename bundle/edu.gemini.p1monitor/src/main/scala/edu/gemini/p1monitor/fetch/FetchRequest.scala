package edu.gemini.p1monitor.fetch

import javax.servlet.http.HttpServletRequest
import edu.gemini.model.p1.immutable.SpecialProposalType

/**
 * A servlet request wrapper that makes it convenient to obtain the request
 * parameters in a format that the servlet can use.
 */
object FetchRequest {
  val DIR_PARAM = "dir"
  val TYPE_PARAM = "type"
  val PROP_PARAM = "proposal"
  val FORMAT_PARAM= "format"
}

class FetchRequest(req: HttpServletRequest) {
  val dir = getParam(req, FetchRequest.DIR_PARAM)
  val proposalType = getParam(req, FetchRequest.TYPE_PARAM)
  val proposal = getParam(req, FetchRequest.PROP_PARAM)
  val formatStr: String = getParam(req, FetchRequest.FORMAT_PARAM)
  val format = FetchFormat.valueOf(formatStr)

  def getParam(req: HttpServletRequest, paramName: String): String =
    Option(req.getParameter(paramName)) match {
      case Some(s:String) => s
      case _              => throw new FetchException("Missing param '" + paramName + "'")
    }

}