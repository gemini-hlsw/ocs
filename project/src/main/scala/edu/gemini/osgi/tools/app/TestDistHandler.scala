package edu.gemini.osgi.tools.app

import java.io.{ File, PrintWriter }

import java.util.jar.Manifest

import scala.xml.dtd.{ PublicID, DocType }
import scala.xml.XML

object TestDistHandler extends GenericUnixDistHandler(false, None)