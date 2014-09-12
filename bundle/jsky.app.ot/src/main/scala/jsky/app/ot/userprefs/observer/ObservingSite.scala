package jsky.app.ot.userprefs.observer

import edu.gemini.spModel.core.Site
import jsky.app.ot.{StaffBean, OTOptions}

import java.beans.{PropertyChangeEvent, PropertyChangeListener}

/**
 * Convenience methods for obtaining (and potentially prompting for the
 * observing site) with "orNull" alternatives for Java clients.
 */
object ObservingSite {
  def get: Option[Site] = Option(ObserverPreferences.fetch().observingSite())
  def getOrNull: Site = get.orNull

  /**
   * Prompt the user for an ObservingSite option if none has been specified.
   */
  def getOrPrompt: Option[Site] = get orElse Option(ObservingSitePrompt.display())
  def getOrPromptOrNull: Site = getOrPrompt.orNull

  /**
   * Monitors the "is staff" property to prompt for the observing site whenever
   * we become staff.
   */
  def initWhenStaff() {
    def setIfNecessary() { if (get.isEmpty) ObservingSitePrompt.display() }

    // Prompt whenever we get staff privileges and there is no observing site
    StaffBean.addPropertyChangeListener(new PropertyChangeListener {
      def propertyChange(evt: PropertyChangeEvent) {
        if (evt.getNewValue.asInstanceOf[Boolean]) setIfNecessary()
      }
    })

    if (OTOptions.isStaffGlobally) setIfNecessary()
  }
}
