package edu.gemini.spdb.rapidtoo.www

import java.io.BufferedReader
import java.security.Principal
import java.util
import java.util.Locale
import javax.servlet.{RequestDispatcher, ServletInputStream}
import javax.servlet.http.{HttpSession, Cookie, HttpServletRequest}

/**
 * An HttpServletRequest for testing.
 */
case class MockRequest(params: Map[String, String]) extends HttpServletRequest {

  def modifiedWith(more: (String, String)*): MockRequest =
    copy(params = params ++ more.toMap)

  def without(k: String): MockRequest =
    copy(params = params - k)

  def getParameter(k: String): String =
    params.get(k).orNull

  def getAttribute(p1: String): AnyRef = ???
  def getAttributeNames: util.Enumeration[_] = ???
  def getAuthType: String = ???
  def getCharacterEncoding: String = ???
  def getContentLength: Int = ???
  def getContentType: String = ???
  def getContextPath: String = ???
  def getCookies: Array[Cookie] = ???
  def getDateHeader(p1: String): Long = ???
  def getHeader(p1: String): String = ???
  def getHeaderNames: util.Enumeration[_] = ???
  def getHeaders(p1: String): util.Enumeration[_] = ???
  def getInputStream: ServletInputStream = ???
  def getIntHeader(p1: String): Int = ???
  def getLocale: Locale = ???
  def getLocales: util.Enumeration[_] = ???
  def getMethod: String = ???
  def getParameterMap: util.Map[_, _] = ???
  def getParameterNames: util.Enumeration[_] = ???
  def getParameterValues(p1: String): Array[String] = ???
  def getPathInfo: String = ???
  def getPathTranslated: String = ???
  def getProtocol: String = ???
  def getQueryString: String = ???
  def getReader: BufferedReader = ???
  def getRealPath(p1: String): String = ???
  def getRemoteAddr: String = ???
  def getRemoteHost: String = ???
  def getRemoteUser: String = ???
  def getRequestDispatcher(p1: String): RequestDispatcher = ???
  def getRequestedSessionId: String = ???
  def getRequestURI: String = ???
  def getRequestURL: StringBuffer = ???
  def getScheme: String = ???
  def getServerName: String = ???
  def getServerPort: Int = ???
  def getServletPath: String = ???
  def getSession(p1: Boolean): HttpSession = ???
  def getSession: HttpSession = ???
  def getUserPrincipal: Principal = ???
  def isRequestedSessionIdFromCookie: Boolean = ???
  def isRequestedSessionIdFromURL: Boolean = ???
  def isRequestedSessionIdFromUrl: Boolean = ???
  def isRequestedSessionIdValid: Boolean = ???
  def isSecure: Boolean = ???
  def isUserInRole(p1: String): Boolean = ???
  def removeAttribute(p1: String): Unit = ???
  def setAttribute(p1: String, p2: scala.Any): Unit = ???
  def setCharacterEncoding(p1: String): Unit = ???

}

object MockRequest {
  def apply(kvs: (String, String)*): MockRequest =
    apply(kvs.toMap)
}