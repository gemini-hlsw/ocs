package jsky.app.ot.gemini.editor.targetComponent.details


trait ReentrancyHack {

  @volatile private[this] var updating = false

  /**
   * Perform the given action, ignoring re-entrant calls. This isn't even close to being
   * threadsafe. Use on UI thread and hope for the best.
   */
  protected[this] def nonreentrant(f: => Unit): Unit =
    if (!updating) {
      updating = true
      try f finally updating = false
    }

}