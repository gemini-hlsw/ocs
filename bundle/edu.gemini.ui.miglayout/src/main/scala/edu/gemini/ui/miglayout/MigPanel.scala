package edu.gemini.ui.miglayout

import scala.swing._
import net.miginfocom.swing._
import net.miginfocom.layout.{LC => MigLC, AC => MigAC, CC => MigCC}

import scala.swing.event.ButtonClicked

object constraints {
  // MigLayout vertical align objects
  sealed trait VMigAlign {
    protected [constraints] def toAlign: String
  }
  case object TopAlign extends VMigAlign {
    override def toAlign = "top"
  }
  case object BottomAlign extends VMigAlign {
    override def toAlign = "bottom"
  }
  // MigLayout horizontal align objects
  sealed trait HMigAlign {
    protected [constraints] def toAlign: String
  }
  case object RightAlign extends HMigAlign {
    override def toAlign = "right"
  }
  case object LeftAlign extends HMigAlign {
    override def toAlign = "left"
  }

  // Constructs for type-safer units
  // use Mig Prefix to pollute less the use of Units
  // We'll support null, pixel and percentage but MigLayout has a few other units
  sealed trait MigUnits[T] {
    val value: T
    def toUnits: String
  }

  case class NoUnit(value: Int) extends MigUnits[Int] {
    override def toUnits = value.toString
  }

  case class PixelsUnit(value: Int) extends MigUnits[Int] {
    override def toUnits = s"${value}px"
  }

  case class PercentUnit[T: Numeric](value: T) extends MigUnits[T] {
    override def toUnits = s"$value%"
  }

  // Converts Ints and Doubles to Units
  implicit class Int2Unit(val value: Int) extends AnyVal {
    def px:MigUnits[Int] = PixelsUnit(value)
    def pct:MigUnits[Int] = PercentUnit(value)
  }

  implicit class Double2Unit(val value: Double) extends AnyVal {
    def pct:MigUnits[Double] = PercentUnit(value)
  }

  /**
   * Decorator for the LC class so we can construct as LC()
   * Note that MigCC is final otherwise we'd rather extend it
   */
  object LC {
    def apply(): MigLC = new MigLC()
  }

  implicit class LCOps(val lc: MigLC) extends AnyVal {
    /**
     * Create insets all around the same value in pixels
     */
    def insets(s: Int):MigLC =
      lc.insets(s"${s}px", s"${s}px", s"${s}px", s"${s}px")

    /**
     * Create insets in pixels
     */
    def insets(top: Int, left: Int, bottom: Int, right: Int):MigLC =
      lc.insets(s"${top}px", s"${left}px", s"${bottom}px", s"${right}px")

    /**
     * Set alignment on X and Y in one call
     */
    def align(x: HMigAlign, y: VMigAlign): MigLC = lc.align(x.toAlign, y.toAlign)
  }

  /**
   * Decorator for the MigAC class so we can construct as CC()
   * Note that MigCC is final otherwise we'd rather extend it
   */
  object AC {
    def apply(): MigAC = new MigAC()
  }

  /**
   * Decorator for the MigCC class so we can construct as CC()
   * Note that MigCC is final otherwise we'd rather extend it
   */
  object CC {
    def apply(): MigCC = new MigCC()
  }

  // Structural type for a class that support alignment
  private type Alignable[T] = {
    def alignX(s: String): T
    def alignY(s: String): T
  }

  // This implicit class applies to LC/CC using structural types
  implicit class AlignableOps[T](val a: Alignable[T]) extends AnyVal {
    /**
     * Type safe alignY
     */
    def alignY(align: VMigAlign):T = a.alignY(align.toAlign)

    /**
     * Type safe alignX
     */
    def alignX(align: HMigAlign):T = a.alignX(align.toAlign)

  }

  // Structural type for a class that support setting width/height
  private type Sizable[T] = {
    def width(s: String): T
    def height(s: String): T
    def maxWidth(s: String): T
    def maxHeight(s: String): T
  }

    // This implicit class applies to LC/CC using structural types
  implicit class SizableOps[T](val a: Sizable[T]) extends AnyVal {
    /**
     * Type safe width
     */
    def width[U](units: MigUnits[U]): T = a.width(units.toUnits)

    /**
     * Type safe height
     */
    def height[U](units: MigUnits[U]): T = a.height(units.toUnits)

    /**
     * Type safe maxWidth
     */
    def maxWidth[U](units: MigUnits[U]): T = a.maxWidth(units.toUnits)

    /**
     * Type safe maxHeight
     */
    def maxHeight[U](units: MigUnits[U]): T = a.maxHeight(units.toUnits)

  }

}

/**
 * Panel that uses the MigLayout
 */
class MigPanel(layoutConstraints: MigLC = constraints.LC(), colConstraints: MigAC = constraints.AC(), rowConstraints: MigAC = constraints.AC()) extends Panel with LayoutContainer {
  override lazy val peer = new javax.swing.JPanel(new MigLayout(layoutConstraints, colConstraints, rowConstraints)) with SuperMixin

  type Constraints = MigCC

  def layoutManager = peer.getLayout.asInstanceOf[MigLayout]

  override def contents: MigContent = new MigContent

  protected class MigContent extends Content {
    def +=(c: Component, l: Constraints) = add(c, l)
  }

  protected def constraintsFor(comp: Component) =
    layoutManager.getConstraintMap.get(comp.peer) match {
      case c:MigCC => c
      case _       => sys.error("cannot happen")
    }

  protected def areValid(c: Constraints): (Boolean, String) = (true, "")

  protected def add(c: Component, l: Constraints = constraints.CC()) = peer.add(c.peer, l)

}

/**
 * Sample application
 */
object MigLayoutDemo extends App {
  import constraints._

  val frame:Frame = new MainFrame() {
    lazy val quitButton = new Button("Quit") {
      reactions += {
        case ButtonClicked(_) =>
          closeOperation()
      }
    }

    // A top level panel that grows and no borders
    contents = new MigPanel(LC().fill().insets(0).width(100.pct).alignX(LeftAlign)) {
      // The upper part shows a form that grows on width and aligns to the top
      add(new MigPanel(LC().fill()) {
        // First row
        add(new Label("First Name"))
        add(new TextField(10), CC().growX())
        add(new Label("Last Name"))
        add(new TextField(10), CC().wrap().growX())
        add(new Label("Phone Number"))
        // Span 3 items
        add(new TextField(30), CC().wrap().spanX(3).growX())
      }, CC().growX().alignY(TopAlign).wrap())

      // Use a Grid on the middle
      add(new MigPanel(LC().fill().debug(0)) {
        add(new Button("Fixed size"), CC().cell(0, 0).width(100.px).height(20.px))
        add(new Button("Fixed max size"), CC().cell(1, 0).growX().maxHeight(15.px))
        add(new Button("C"), CC().cell(0, 1).grow())
        add(new Button("D"), CC().cell(1, 1).grow())
        // Span 2
        add(new Button("D"), CC().cell(0, 2, 2, 2).grow())
        // One button spanning several columns and a max width
        add(new Button("Max Height 75%"), CC().cell(2, 0, 2, 3).grow().maxHeight(75.pct))
      }, CC().growX().alignY(TopAlign).wrap())

      // Label and exit button
      add(new MigPanel(LC().fill().align(LeftAlign, TopAlign)) {
        // Keep on the right
        add(new Label("Demo App"), CC().alignX(LeftAlign))
        // Keep on the left
        add(quitButton, CC().alignX(RightAlign))
      }, CC().growX().alignY(BottomAlign))
    }

    peer.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE)
  }

  frame.pack()
  frame.centerOnScreen()
  frame.visible = true
}