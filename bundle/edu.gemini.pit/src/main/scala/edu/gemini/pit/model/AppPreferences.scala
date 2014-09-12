package edu.gemini.pit.model

import edu.gemini.model.p1.pdf.P1PDF

import java.util.prefs.Preferences._
import scala.collection.JavaConverters._
import java.util.prefs.{Preferences, PreferenceChangeEvent, PreferenceChangeListener}
import java.util.logging.{Level, Logger}
import swing.Swing

// The UI has a few global, persistent preferences. Other bits and pieces of the app can have their own "local"
// persistent prefs, for example, the file chooser remembers where it was the last time you opened it.
case class AppPreferences(pdf: Option[P1PDF.Template], mode:AppPreferences.PITMode.Value)

object AppPreferences {

  // Members
  private lazy val logger = Logger.getLogger(getClass.getName)
  private lazy val node = userNodeForPackage(getClass)
  private var listeners:List[AppPreferences => Unit] = Nil

  implicit class pimpNode(val p:Preferences) extends AnyVal {
    def get(key:String):Option[String] = Option(p.get(key, null))
    def get[A](f: String => Option[A])(key:String):Option[A] = get(key).flatMap(f)
    def put[A](f: A => String)(key:String)(a:Option[A]) { a.foreach(a => p.put(key, f(a))) }
  }

  // Enumeration for PIT mode
  object PITMode extends Enumeration {
    val PI, TAC = Value
  }

  // PDF pref, getter, setter
  private lazy val pdfPref = getClass.getName + ".pdf"
  private lazy val putPdf = node.put((t:P1PDF.Template) => t.name)(pdfPref) _
  private def getPdf = node.get((s:String) => P1PDF.templates.asScala.find(_.name == s))(pdfPref)

  // Mode pref, getter, setter
  private lazy val modePref = getClass.getName + ".mode"
  private lazy val putMode = node.put((m:PITMode.Value) => m.toString)(modePref) _
  private def getMode = node.get((s:String) => PITMode.values.find(_.toString == s))(modePref)

  /** Returns the current AppPreferences */
  def current: AppPreferences = AppPreferences(getPdf, getMode.getOrElse(PITMode.PI))

  /** Sets the current AppPreferences, notifying any listeners. */
  def current_=(p: AppPreferences) {
    putPdf(p.pdf)
    putMode(Some(p.mode))
  }

  /** Permanently adds the specified listener. */
  def addListener(f: AppPreferences => Unit) {
    synchronized {
      listeners = f :: listeners
      notifyListener(current)(f)
    }
  }

  // Notify on the event dispatch thread
  private def notifyListener(ps:AppPreferences)(f: AppPreferences => Unit) {
    Swing.onEDT {
      try {
        f(ps)
      } catch {
        case e: Throwable => logger.log(Level.WARNING, "Problem notifying pref listener.", e)
      }
    }
  }

  // Hook up the listener machinery
  node.addPreferenceChangeListener(new PreferenceChangeListener {
    def preferenceChange(evt:PreferenceChangeEvent) {
      synchronized {
        val n = notifyListener(current) _
        listeners.foreach(n)
      }
    }
  })

}

