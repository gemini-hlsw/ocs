package edu.gemini.osgi.tools

import java.io._
import scala.io.Source

object FileUtils {
  /** List the files immediately contained in a parent directory. */
  def listFiles(parentDir: File): List[File] =
    if (parentDir.isDirectory) parentDir.listFiles.toList
    else Nil

  /** List of immediate child directories of the given directory. */
  def childDirs(parentDir: File): List[File] =
    listFiles(parentDir).filter(_.isDirectory)

  /** Finds the first file contained in root with the given name. */
  def findFile(root: File, name: String): Option[File] = {
    def go(fs: List[File]): Option[File] = fs match {
      case Nil      => None
      case (h :: t) => if (h.getName == name) Some(h)
                       else go(listFiles(h) ++ t)
    }
    go(List(root))
  }

  /** Converts a File into a List of its path elements. */
  def pathToList(f: File): List[String] = 
    f.getAbsolutePath.split(File.separator).toList

  /** Computes a relative file path needed to traverse from the "fromDir" to the "to" file.  */
  def relativePath(fromDir: File, to: File): String = {
    require(fromDir.isDirectory, "fromDir is not a directory")

    val fromPath = pathToList(fromDir)
    val toPath   = pathToList(to)

    // Remove the common prefix from the two paths.
    val diff = fromPath.zipAll(toPath, "", "") dropWhile { case (a, b) => a == b }

    // Get the remaining "from" and "to" path suffixes
    val (f, t) = diff.unzip

    // Anything left over in the "from" path (if anything), gets converted to
    // ".." parent directory.  Anything left over in the "to" path is then
    // appended.
    val rmEmpty: Traversable[String] => Traversable[String] = _.filterNot(_ == "")
    val path = (rmEmpty(f) map { _ => ".." }) ++ rmEmpty(t)

    path.mkString(File.separator)
  }

  /** Writes a string to file. */
  def save(str: String, to: File): Unit = {
    val pw = new PrintWriter(to)
    str.lines.foreach(pw.println)
    pw.close()
  }

  /** Writes an xml hunk to a file, pretty-printed. */
  def save(n: xml.Node, to: File): Unit = 
    save(new xml.PrettyPrinter(240, 2).format(n), to)

  /** A file filter for jar files . */
  val jarFilter: FileFilter = 
    new FileFilter { 
      def accept(f: File): Boolean = 
        f.getName.endsWith(".jar") 
    }

}
