package edu.gemini.osgi.tools

import java.io.File

case class ProjectDirs(root: File) {
  
  val appRoot    = new File(root, "app")
  val bundleRoot = new File(root, "bundle")
  val libRoot    = new File(root, "lib/bundle")

  def app(name: String): File = 
    new File(appRoot, name)

  def bundle(name: String): File = 
    new File(bundleRoot, name)

}

