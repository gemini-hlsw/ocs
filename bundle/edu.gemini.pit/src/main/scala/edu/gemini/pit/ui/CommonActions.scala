package edu.gemini.pit.ui

// Each view can provide its own implementation of these actions by overriding BoundView.commonActions. The
// actions in the map will be invoked by the RetargetActions in the main menu.
object CommonActions extends Enumeration {
	val Cut, Copy, Paste, Delete, SelectAll = Value
	type CommonActions = Value
}
