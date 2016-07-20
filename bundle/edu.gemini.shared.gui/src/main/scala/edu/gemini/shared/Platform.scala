package edu.gemini.shared

import java.awt.Toolkit
import java.awt.event.InputEvent
import java.net.URL
import java.util.logging.Logger
import javax.swing.{InputMap, JTextArea, JTextField, KeyStroke}

object Platform extends Enumeration {

  val Mac, Windows, Other = Value

  private val LOGGER = Logger.getLogger(getClass.getName)

  lazy val current = {
    def is(s: String) = System.getProperty("os.name").contains(s)
    if (is("Mac")) Mac else if (is("Windows")) Windows else Other
  }

  def IS_MAC = current == Mac
  def IS_WINDOWS = current == Windows

  lazy val MENU_ACTION_MASK = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
  lazy val TOGGLE_ACTION_MASK = if (current == Mac) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK

  if (current == Mac) {

    val CTRL = InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK

    def fix(omap: Option[InputMap]) {
      for {

      // Base case exits on None
        map <- omap

        // Do the recursive call here
        _ = fix(Option(map.getParent))

        // Get the keystrokes
        keys <- Option(map.keys)
        ks <- keys

        // And new modifiers
        mod = ks.getModifiers if (mod & CTRL) != 0
        newMod = (mod & ~CTRL) | MENU_ACTION_MASK
        newKs = KeyStroke.getKeyStroke(ks.getKeyCode, newMod, ks.isOnKeyRelease)
        action = map.get(ks)

      } {

        // Update the map
        map.remove(ks)
        map.put(newKs, action)

      }
    }

    LOGGER.info("Repairing edit component keymap for Mac OS X.")
    Seq(new JTextArea, new JTextField) foreach { o => fix(Option(o.getInputMap)) }

  }

  /*
   * Derived from http://www.javaworld.com/javaworld/javatips/jw-javatip66.html
   */
  def displayURL(url: URL) { displayURL(url.toString) }
  def displayURL(url: String) {
    current match {
      case Windows => exec("rundll32 url.dll,FileProtocolHandler " + url)
      case Mac     => exec("/usr/bin/open %s" format url)
      case _       => if (exec("firefox -remote openURL(%s)" format url).waitFor != 0) exec("firefox " + url)
    }
  }

  private def exec(s: String) = sys.runtime.exec(s)

}
