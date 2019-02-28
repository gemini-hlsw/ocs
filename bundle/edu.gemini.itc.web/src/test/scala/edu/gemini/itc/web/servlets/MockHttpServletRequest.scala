package edu.gemini.itc.web.servlets

import java.io.{ BufferedReader, ByteArrayInputStream }
import java.util.{ Enumeration, Map => JMap, Locale }
import java.security.Principal
import javax.servlet.{ RequestDispatcher, ServletInputStream }
import javax.servlet.http.{ HttpServletRequest, HttpSession }
import javax.servlet.http.Cookie
import scala.util.control.NoStackTrace

/**
 * A very stripped-down implementation of HttpServletRequest for testing.
 * @param body the request body
 */
final class MockHttpServletRequest private (body: String) extends HttpServletRequest {

  private var _characterEncoding: String = null
  def getCharacterEncoding() = _characterEncoding
  def setCharacterEncoding(s: String) = _characterEncoding = s

  private var _getInputStreamCalled: Boolean = false
  def getInputStream(): ServletInputStream =
    if (_getInputStreamCalled)  {
      throw new IllegalStateException("Spec says getInputStream() can only be called once")
    } else {
      new ServletInputStream {
        val delegate = new ByteArrayInputStream(body.getBytes(Option(_characterEncoding).getOrElse("ISO-8859-1")))
        def read(): Int = delegate.read()
      }
    }

  // Remaining functionalty is not implemented

  private def notImplemented(method: String) =
    throw new Exception(s"Not implemented: MockHttpServletRequest.$method") with NoStackTrace

  def getAttribute(a: String): Object = notImplemented("getAttribute")
  def getAttributeNames(): Enumeration[_] = notImplemented("getAttributeNames")
  def getAuthType(): String = notImplemented("getAuthType")
  def getContentLength(): Int = notImplemented("getContentLength")
  def getContentType(): String = notImplemented("getContentType")
  def getContextPath(): String = notImplemented("getContextPath")
  def getCookies(): Array[Cookie] = notImplemented("getCookies")
  def getDateHeader(a: String): Long = notImplemented("getDateHeader")
  def getHeader(a: String): String = notImplemented("getHeader")
  def getHeaderNames(): Enumeration[_] = notImplemented("getHeaderNames")
  def getHeaders(a: String): Enumeration[_] = notImplemented("getHeaders")
  def getIntHeader(a: String): Int = notImplemented("getIntHeader")
  def getLocale(): Locale = notImplemented("getLocale")
  def getLocales(): Enumeration[_] = notImplemented("getLocales")
  def getMethod(): String = notImplemented("getMethod")
  def getParameter(a: String): String = notImplemented("getParameter")
  def getParameterMap(): JMap[_, _] = notImplemented("getParameterMap")
  def getParameterNames(): Enumeration[_] = notImplemented("getParameterNames")
  def getParameterValues(a: String): Array[String] = notImplemented("getParameterValues")
  def getPathInfo(): String = notImplemented("getPathInfo")
  def getPathTranslated(): String = notImplemented("getPathTranslated")
  def getProtocol(): String = notImplemented("getProtocol")
  def getQueryString(): String = notImplemented("getQueryString")
  def getReader(): BufferedReader = notImplemented("getReader")
  def getRealPath(a: String): String = notImplemented("getRealPath")
  def getRemoteAddr(): String = notImplemented("getRemoteAddr")
  def getRemoteHost(): String = notImplemented("getRemoteHost")
  def getRemoteUser(): String = notImplemented("getRemoteUser")
  def getRequestDispatcher(a: String): RequestDispatcher = notImplemented("getRequestDispatcher")
  def getRequestedSessionId(): String = notImplemented("getRequestedSessionId")
  def getRequestURI(): String = notImplemented("getRequestURI")
  def getRequestURL(): StringBuffer = notImplemented("getRequestURL")
  def getScheme(): String = notImplemented("getScheme")
  def getServerName(): String = notImplemented("getServerName")
  def getServerPort(): Int = notImplemented("getServerPort")
  def getServletPath(): String = notImplemented("getServletPath")
  def getSession(): HttpSession = notImplemented("getSession")
  def getSession(a: Boolean): HttpSession = notImplemented("getSession")
  def getUserPrincipal(): Principal = notImplemented("getUserPrincipal")
  def isRequestedSessionIdFromCookie(): Boolean = notImplemented("isRequestedSessionIdFromCookie")
  def isRequestedSessionIdFromUrl(): Boolean = notImplemented("isRequestedSessionIdFromUrl")
  def isRequestedSessionIdFromURL(): Boolean = notImplemented("isRequestedSessionIdFromURL")
  def isRequestedSessionIdValid(): Boolean = notImplemented("isRequestedSessionIdValid")
  def isSecure(): Boolean = notImplemented("isSecure")
  def isUserInRole(a: String): Boolean = notImplemented("isUserInRole")
  def removeAttribute(a: String): Unit = notImplemented("removeAttribute")
  def setAttribute(a: String, b: Any): Unit = notImplemented("setAttribute")
}

object MockHttpServletRequest {
  def apply(body: String): HttpServletRequest =
    new MockHttpServletRequest(body)
}