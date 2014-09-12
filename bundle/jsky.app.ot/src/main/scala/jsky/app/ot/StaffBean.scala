package jsky.app.ot

import java.beans.{PropertyChangeListener, PropertyChangeSupport}
import scala.swing.Swing
import scalaz.effect.IO
import edu.gemini.util.security.auth.keychain.Action._

/**
 * Tracks the auth client to notice changes in whether or not the user has
 * global staff privileges.
 */
object StaffBean {
  var wasStaff = OTOptions.isStaffGlobally

  OT.getKeyChain.addListener(IO{
    setStaff(OTOptions.isStaffGlobally)
    true
  }).unsafeRunAndThrow

  val StaffProperty = "Staff"
  val sup = new PropertyChangeSupport(())

  def addPropertyChangeListener(pcl: PropertyChangeListener) {
    sup.addPropertyChangeListener(pcl)
  }

  def removePropertyChangeListener(pcl: PropertyChangeListener) {
    sup.removePropertyChangeListener(pcl)
  }

  def isStaff: Boolean = OTOptions.isStaffGlobally

  def setStaff(nv: Boolean) {
    Swing.onEDT {
      if (nv != wasStaff) {
        wasStaff = nv
        sup.firePropertyChange(StaffProperty, !nv, nv)
      }
    }
  }
}
