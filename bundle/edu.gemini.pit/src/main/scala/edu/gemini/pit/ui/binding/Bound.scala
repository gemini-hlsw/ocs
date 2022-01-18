package edu.gemini.pit.ui.binding

import scalaz._

/**
 * Data binding support. An object of type Bound[A,M] is bound to a value of type A, focused down to a value of type
 * M ("model") via a provided Lens. This allows each UI component to handle a particular small part of the Proposal,
 * without worrying about the rest of it. External changes are pushed down, and local changes are pushed up. This is
 * how the PIT UI works.
 *
 * The model (the value you're watching and perhaps changing) is available via the read/write property 'model', of type
 * Option[M]. Writing this property causes the new value to be pushed up through the lens to the top-level binding
 * provider (in our case a BoundView, which ultimately pushes model changes up to the Shell).
 *
 * External changes will similarly be pushed down into bound objects. This event can be detected, if desired, by
 * overriding refresh(m:Option[M]):Unit.
 *
 * An object of type Bound[_,M] can have a list of child objects of type Bound[M,_], declared by overriding 'children'.
 * This is how you hook up a tree of components and sub-components. The mechanics of pushing model changes to children
 * is provided for free.
 */
trait Bound[A, M] {

  private[this] var a: Option[A] = None
  private[this] var p: Option[Option[A] => Unit] = None
  private[this] var cachedModel:Option[M] = None

  private val mutex = new Object

  /**
   * This method binds a new model and provides a method for pushing a new model back out. It is
   * called by the framework and generally should not be used in client code.
   */
  final def bind(a: Option[A], p: Option[A] => Unit): Unit = {
    mutex.synchronized {
      cachedModel = None
      this.a = a
      this.p = Some(p)
      refresh(model)
      for {
        c <- children
      } c.bind(model, model_=)
    }
  }

  // TODO: this is a hack only used for some warning logging in BoundView. Fix it
  private[binding] def outer = a

  def rebind(): Unit = {
    p.foreach(bind(a, _))
  }

  /** Returns the model, if any. */
  protected def model: Option[M] = (if (cachedModel == null) None else cachedModel) orElse (for {
    a <- Option(a) // sadly this can be null for a moment, I think
    m <- a.map(lens.get)
  } yield { cachedModel = Some(m); m })

  /**
   * Sets the model, pushing it out through the lens if possible; if this method is called prior
   * to bind() then this method has no effect.
   */
  protected def model_=(m: Option[M]): Unit = {
    p.foreach(_(for {a <- a; m <- m} yield lens.set(a, m)))
  }

  /** Implementors must provide a lens that focuses the outer model. */
  protected def lens: Lens[A, M]

  /** Implementors must provide a method to react to model changes. */
  protected def refresh(m: Option[M]): Unit = {
    ()
  }

  /** Our children, if any. */
  protected def children: List[Bound[M, _]] = Nil

}

object Bound {

  /**
   * Data binding without lensing.
   */
  trait Self[A] extends Bound[A, A] {
    val lens = Lens.lensId[A]
  }

}
