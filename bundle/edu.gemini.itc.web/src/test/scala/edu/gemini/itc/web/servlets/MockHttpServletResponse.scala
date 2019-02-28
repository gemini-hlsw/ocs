package edu.gemini.itc.web.servlets

import java.io.{ PrintWriter, StringWriter }
import java.util.Locale
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Cookie
import scala.util.control.NoStackTrace

/**
 * A very stripped-down implementation of HttpServletResponse for testing.
 */
final class MockHttpServletResponse private extends HttpServletResponse {

  private var _status: Option[Int] = None
  def setStatus(a: Int): Unit = _status = Some(a)
  def getStatus(): Int = _status.getOrElse(sys.error("Status is not set."))

  private var _contentType: Option[String] = None
  def setContentType(a: String): Unit = _contentType = Some(a)
  def getContentType(): String = _contentType.getOrElse("ContentType is not set.")

  // We assume naively that the charset is specified on the content-type and it's the only
  // attribute. So it will only work for things of the form "text/json; charset=UTF-8"
  def getCharacterEncoding(): String = {
    val ct = getContentType
    ct.substring(ct.indexOf("=") + 1)
  }

  private var _writer: StringWriter = null
  def getWriter(): PrintWriter =
    if (_writer != null) {
      throw new IllegalStateException("Spec says getWriter() can only be called once")
    } else {
      _writer = new StringWriter
      new PrintWriter(_writer)
    }

  def body: String =
    if (_writer != null) _writer.toString
    else sys.error("getWriter was never called.")

  def sendError(statusCode: Int, body: String): Unit = {
    setContentType("text/html; charset=UTF-8")
    setStatus(statusCode)
    getWriter().println(body) // in reality this would be HTML formatted
  }

  // Remaining functionalty is not implemented

  private def notImplemented(method: String) =
    throw new Exception(s"Not implemented: MockHttpServletResponse.$method") with NoStackTrace

  def addCookie(a: Cookie): Unit = notImplemented("addCookie")
  def addDateHeader(a: String, b: Long): Unit = notImplemented("addDateHeader")
  def addHeader(a: String, b: String): Unit = notImplemented("addHeader")
  def addIntHeader(a: String, b: Int): Unit = notImplemented("addIntHeader")
  def containsHeader(a: String): Boolean = notImplemented("containsHeader")
  def encodeRedirectURL(a: String): String = notImplemented("encodeRedirectURL")
  def encodeRedirectUrl(a: String): String = notImplemented("encodeRedirectUrl")
  def encodeURL(a: String): String = notImplemented("encodeURL")
  def encodeUrl(a: String): String = notImplemented("encodeUrl")
  def flushBuffer(): Unit = notImplemented("flushBuffer")
  def getBufferSize(): Int = notImplemented("getBufferSize")
  def getLocale(): Locale = notImplemented("getLocale")
  def getOutputStream(): ServletOutputStream = notImplemented("getOutputStream")
  def isCommitted(): Boolean = notImplemented("isCommitted")
  def reset(): Unit = notImplemented("reset")
  def resetBuffer(): Unit = notImplemented("resetBuffer")
  def sendError(a: Int): Unit = notImplemented("sendError")
  def sendRedirect(a: String): Unit = notImplemented("sendRedirect")
  def setBufferSize(a: Int): Unit = notImplemented("setBufferSize")
  def setContentLength(a: Int): Unit = notImplemented("setContentLength")
  def setDateHeader(a: String, b: Long): Unit = notImplemented("setDateHeader")
  def setHeader(a: String, b: String): Unit = notImplemented("setHeader")
  def setIntHeader(a: String, b: Int): Unit = notImplemented("setIntHeader")
  def setLocale(a: Locale): Unit = notImplemented("setLocale")
  def setStatus(a: Int, b: String): Unit = notImplemented("setStatus")
}

object MockHttpServletResponse {
  def apply(): MockHttpServletResponse =
    new MockHttpServletResponse
}