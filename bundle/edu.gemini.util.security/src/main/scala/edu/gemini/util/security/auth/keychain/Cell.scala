package edu.gemini.util.security.auth.keychain

// A mutable cell
class Cell[A] private (initial: A) {
  private var a = initial
  def get: Action[A] = Action(a)
  def put(newValue: A): Action[Unit] = Action { a = newValue }
  def modify(f: A => A) = get.flatMap(a => put(f(a)))
}

object Cell {
  def apply[A](a: A): Action[Cell[A]] =
    Action(new Cell(a))
}

