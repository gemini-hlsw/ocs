package edu.gemini.services.server.util

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.util.IOUtils

import java.io.{FileOutputStream, File}
import java.util.Collections
import java.util.logging.{Level, Logger}

/**
 * Helper functionality that provides access to Google services, most importantly authorization.
 */
object GoogleServices {

  val Log = Logger.getLogger(getClass.getName)

  val HttpTransport = new NetHttpTransport()
  val JsonFactory = new JacksonFactory()

  private val ServiceAccountEmail = "11605407579-d4vn2a7386u4qapn92jp51t7g49pfnb7@developer.gserviceaccount.com"
  private val PrivateKeyFileName = "24408334e14c4c6ca38b20b445384c25e9c4a8a9-privatekey.p12"
  private val Scopes = Collections.unmodifiableList(new java.util.ArrayList[String]() {{
    add("https://www.googleapis.com/auth/calendar")
  }} )

  /** Get credentials for accessing google services. */
  def authorize(): Credential = {
    try {

      new GoogleCredential.Builder().
      setTransport(HttpTransport).
      setJsonFactory(JsonFactory).
      setServiceAccountId(ServiceAccountEmail).
      setServiceAccountScopes(Scopes).
      setServiceAccountPrivateKeyFromP12File(createP12File).
      // setServiceAccountUser("user@example.com").
      build()

    } catch {
      case t: Throwable =>
        Log.log(Level.WARNING, "Could not get credentials for Google services.", t)
        new GoogleCredential.Builder().build
    }
  }


  /** Is there a simpler way to get a File handle to a resource? For now copy it into a temp file. */
  private def createP12File: File = {
    val in = getClass.getResourceAsStream(PrivateKeyFileName)
    val file = File.createTempFile("gemini", "key")
    val out = new FileOutputStream(file)
    IOUtils.copy(in, out)
    in.close()
    out.close()
    file
  }

}
