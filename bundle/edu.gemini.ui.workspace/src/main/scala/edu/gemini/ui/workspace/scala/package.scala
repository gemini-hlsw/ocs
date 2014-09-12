package edu.gemini.ui.workspace

package object scala {

  private var cache: Map[IShell, RichShell[_]] = Map()

  implicit def enrichShell[A](shell: IShell):RichShell[A] = synchronized {
    cache.getOrElse(shell, {
      val ret = new RichShell(shell)
      cache = cache + (shell -> ret)
      ret
    }).asInstanceOf[RichShell[A]]
  }

  implicit def enrichShellContext[A](context: IShellContext):RichShellContext[A] = new RichShellContext[A](context)

}