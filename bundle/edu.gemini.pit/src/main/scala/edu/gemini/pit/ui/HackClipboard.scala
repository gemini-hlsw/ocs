package edu.gemini.pit.ui

import java.awt.datatransfer.Clipboard

/**
 * An app-local clipboard that we use due to bugs in the system clipboard API (getContents throws an IOException if
 * invoked a second time, after the contents have changed). The system clipboard still seems to work for drag and drop;
 * we just need this for Cut/Copy/Paste.
 */
object HackClipboard extends Clipboard("Hacked local clipboard.")