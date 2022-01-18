package edu.gemini.pit.ui.util

import scala.swing.{ Dialog, UIElement }

/**
 * A class of dialogs that implement an editor for a value of type A. Implementers should call
 * close(A) passing the edited value on ok, or close() on cancel. Clients call open(A) with the
 * initial value and receive the new value (or None). This lets us say
 * <code>
 *   for {
 *     b <- new FooEditor(a).open(parent)
 *   } println("new value is B")
 * </code>
 * where the result of a cancel is nothing.
 */
class ModalEditor[A] extends Dialog with CloseOnEsc {

  private var result: Option[A] = None

  /** Closes this dialog with a successful result. */
  def close(a: A): Unit = {
    result = Some(a)
    close()
  }

  /** 
   * Open the dialog modally, centered over the specified parent element, and return the result
   * of the edit (or None if the dialog is closed without a result). 
   */
  def open(parent: UIElement): Option[A] = {
    modal = true
    Option(parent).foreach(setLocationRelativeTo)
    open()
    result
  }

}

