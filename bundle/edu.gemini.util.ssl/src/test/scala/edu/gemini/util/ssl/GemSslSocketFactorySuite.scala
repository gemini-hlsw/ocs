package edu.gemini.util.ssl

import org.scalatest._

class GemSslSocketFactorySuite extends FunSuite {

  private val urls = List(
    // Core Gemini services
    "archive.gemini.edu",
    "gnauxodb.gemini.edu",
    "phase1.gemini.edu",
    // External services
    "ssd.jpl.nasa.gov",
    "archive.eso.org",
    "gsss.stsci.edu",
    "gea.esac.esa.int",
    // "simbad.cfa.harvard.edu",
    "simbad.u-strasbg.fr",
    "github.com"
  )

  test("create secure socket connections") {
    val factory = GemSslSocketFactory.get()

    urls.foreach { url =>
      println(s"Connecting to $url")
      val socket = factory.createSocket(url, 443).asInstanceOf[javax.net.ssl.SSLSocket]
      try {
        socket.startHandshake()
        assert(socket.isConnected, s"Failed to connect to $url")
      } finally {
        socket.close()
      }
    }
  }

}
