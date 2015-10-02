package edu.gemini.ui.miglayout

import scala.language.reflectiveCalls
import scala.swing._
import net.miginfocom.swing._
import net.miginfocom.layout.{LC => MigLC, AC => MigAC, CC => MigCC}

import scala.swing.event.ButtonClicked

/**
 * Definition of constraints to add elements to a MigLayout
 * Using these constraints we can avoid the usage of too many String-based definitions
 * See the example @see MigLayoutDemo
 */
object constraints {
  // MigLayout vertical align objects
  sealed abstract class VMigAlign(protected [constraints] val toAlign: String)
  case object TopAlign extends VMigAlign("top")
  case object BottomAlign extends VMigAlign("bottom")
  case object BaselineAlign extends VMigAlign("baseline")
  // MigLayout horizontal align objects
  sealed abstract class HMigAlign(protected [constraints] val toAlign: String)
  case object RightAlign extends HMigAlign("right")
  case object LeftAlign extends HMigAlign("left")
  case object CenterAlign extends HMigAlign("center")

  // Constructs for type-safer units
  // use Mig Prefix to pollute less the use of Units
  sealed trait MigUnits[T] {
    val value: T
    def toBoundSize: String
  }

    // We'll support null, pixel and percentage but MigLayout has a few other, less used, units
  case class NoUnit(value: Int) extends MigUnits[Int] {
    override def toBoundSize = value.toString
  }

  case class PixelsUnit(value: Int) extends MigUnits[Int] {
    override def toBoundSize = s"${value}px"
  }

  case class PercentUnit[T: Numeric](value: T) extends MigUnits[T] {
    override def toBoundSize = s"$value%"
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

  // Add operations to LC()
  implicit class LCOps(val lc: MigLC) extends AnyVal {
    /**
     * Create insets all around the same value in pixels
     */
    def insets(s: Int):MigLC =
      lc.insets(s"${s}px", s"${s}px", s"${s}px", s"${s}px")

    /**
     * Create insets all around the same value
     */
    def insets[T](s: MigUnits[T]):MigLC =
      lc.insets(s.toBoundSize, s.toBoundSize, s.toBoundSize, s.toBoundSize)

    /**
     * Create insets in pixels
     */
    def insets(top: Int, left: Int, bottom: Int, right: Int):MigLC =
      lc.insets(s"${top}px", s"${left}px", s"${bottom}px", s"${right}px")

    /**
     * Create insets
     */
    def insets[T, U, V, W](top: MigUnits[T], left: MigUnits[U], bottom: MigUnits[V], right: MigUnits[W]):MigLC =
      lc.insets(top.toBoundSize, left.toBoundSize, bottom.toBoundSize, right.toBoundSize)

    /**
     * insetsAll in units
     */
    def insetsAll[T](s: MigUnits[T]):MigLC =
      lc.insetsAll(s.toBoundSize)

    /**
     * Set alignment on X and Y in one call
     */
    def align(x: HMigAlign, y: VMigAlign): MigLC = lc.align(x.toAlign, y.toAlign)

    /**
     * Set grid X gaps in Units
     */
    def gridGapX[T](g: MigUnits[T]) = lc.gridGapX(g.toBoundSize)

    /**
     * Set grid Y gaps in Units
     */
    def gridGapY[T](g: MigUnits[T]) = lc.gridGapY(g.toBoundSize)

    /**
     * Set grid gap on X and Y in Units
     */
    def gridGap[T, U](x: MigUnits[T], y: MigUnits[U]) = lc.gridGap(x.toBoundSize, y.toBoundSize)
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
  protected trait Alignable[T] {
    def alignX(a: T, s: String): T
    def alignY(a: T, s: String): T
  }

  // This implicit class applies to LC/CC using structural types
  implicit class AlignableOps[T](val a: T) extends AnyVal {
    /**
     * Type safe alignY
     */
    def alignY(align: VMigAlign)(implicit ev: Alignable[T]):T = ev.alignY(a, align.toAlign)

    /**
     * Type safe alignX
     */
    def alignX(align: HMigAlign)(implicit ev: Alignable[T]):T = ev.alignX(a, align.toAlign)
  }

  implicit val LCAlignable = new Alignable[MigLC] {
    override def alignX(a: MigLC, s: String) = a.alignX(s)
    override def alignY(a: MigLC, s: String) = a.alignY(s)
  }
  implicit val CCAlignable = new Alignable[MigCC] {
    override def alignX(a: MigCC, s: String) = a.alignX(s)
    override def alignY(a: MigCC, s: String) = a.alignY(s)
  }

  // Structural type for a class that support setting width/height
  protected trait Sizable[T] {
    def width(a: T, s: String): T
    def height(a: T, s: String): T
    def maxWidth(a: T, s: String): T
    def maxHeight(a: T, s: String): T
    def minWidth(a: T, s: String): T
    def minHeight(a: T, s: String): T
  }

  // This implicit class applies to LC/CC using structural types
  implicit class SizableOps[T](val a: T) extends AnyVal {
    /**
     * Type safe width
     */
    def width[U](units: MigUnits[U])(implicit ev: Sizable[T]): T = ev.width(a, units.toBoundSize)

    /**
     * Type safe height
     */
    def height[U](units: MigUnits[U])(implicit ev: Sizable[T]): T = ev.height(a, units.toBoundSize)

    /**
     * Type safe maxWidth
     */
    def maxWidth[U](units: MigUnits[U])(implicit ev: Sizable[T]): T = ev.maxWidth(a, units.toBoundSize)

    /**
     * Type safe maxHeight
     */
    def maxHeight[U](units: MigUnits[U])(implicit ev: Sizable[T]): T = ev.maxHeight(a, units.toBoundSize)

    /**
     * Type safe minWidth
     */
    def minWidth[U](units: MigUnits[U])(implicit ev: Sizable[T]): T = ev.minWidth(a, units.toBoundSize)

    /**
     * Type safe minHeight
     */
    def minHeight[U](units: MigUnits[U])(implicit ev: Sizable[T]): T = ev.minHeight(a, units.toBoundSize)
  }

  implicit val LCSizable = new Sizable[MigLC] {
    override def width(a: MigLC, s: String): MigLC = a.width(s)
    override def height(a: MigLC, s: String): MigLC = a.height(s)
    override def maxWidth(a: MigLC, s: String): MigLC = a.maxWidth(s)
    override def maxHeight(a: MigLC, s: String): MigLC = a.maxHeight(s)
    override def minWidth(a: MigLC, s: String): MigLC = a.minWidth(s)
    override def minHeight(a: MigLC, s: String): MigLC = a.minHeight(s)
  }
  implicit val CCSizable = new Sizable[MigCC] {
    override def width(a: MigCC, s: String): MigCC = a.width(s)
    override def height(a: MigCC, s: String): MigCC = a.height(s)
    override def maxWidth(a: MigCC, s: String): MigCC = a.maxWidth(s)
    override def maxHeight(a: MigCC, s: String): MigCC = a.maxHeight(s)
    override def minWidth(a: MigCC, s: String): MigCC = a.minWidth(s)
    override def minHeight(a: MigCC, s: String): MigCC = a.minHeight(s)
  }


  // Add methods to CC()
  implicit class CCOps(val cc: MigCC) extends AnyVal {

    /**
     * Specify top gap in Units
     */
    def gapTop[T](u: MigUnits[T]): MigCC = cc.gapTop(u.toBoundSize)

    /**
     * Specify bottom gap in Units
     */
    def gapBottom[T](u: MigUnits[T]): MigCC = cc.gapBottom(u.toBoundSize)

    /**
     * Specify right gap in Units
     */
    def gapRight[T](u: MigUnits[T]): MigCC = cc.gapRight(u.toBoundSize)

    /**
     * Specify left gap in Units
     */
    def gapLeft[T](u: MigUnits[T]): MigCC = cc.gapLeft(u.toBoundSize)

    /**
     * Specify before gap in Units
     */
    def gapBefore[T](u: MigUnits[T]): MigCC = cc.gapBefore(u.toBoundSize)

    /**
     * Specify after gap in Units
     */
    def gapAfter[T](u: MigUnits[T]): MigCC = cc.gapAfter(u.toBoundSize)

    /**
     * Specify before and after gap on Y in Units
     */
    def gapY[T, U](before: MigUnits[T], after: MigUnits[U]): MigCC = cc.gapY(before.toBoundSize, after.toBoundSize)

    /**
     * Specify before and after gap on X in Units
     */
    def gapX[T, U](before: MigUnits[T], after: MigUnits[U]): MigCC = cc.gapX(before.toBoundSize, after.toBoundSize)

    /**
     * Specify gaps in Units
     */
    def gap[T, U, V, W](left: MigUnits[T], right: MigUnits[U], top: MigUnits[V], bottom: MigUnits[W]): MigCC = {
      cc.gapLeft(left.toBoundSize)
      cc.gapRight(right.toBoundSize)
      cc.gapTop(top.toBoundSize)
      cc.gapBottom(bottom.toBoundSize)
    }
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
    def remove(c: Component):Unit = {
      val index = this.indexWhere(_ == c)
      if (index >= 0) {
        this.remove(index)
      }
    }
  }

  override protected def constraintsFor(comp: Component) =
    layoutManager.getConstraintMap.get(comp.peer) match {
      case c:MigCC => c
      case _       => sys.error("cannot happen")
    }

  override protected def areValid(c: Constraints): (Boolean, String) = (true, "")

  override protected def add(c: Component, l: Constraints = constraints.CC()) = peer.add(c.peer, l)

}

/**
 * Sample application with a Frame containing a set of nested MigPanels
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
    contents = new MigPanel(LC().fill().insets(0.px).width(100.pct).alignX(LeftAlign)) {
      // The upper part shows a form that grows on width and aligns to the top
      add(new MigPanel(LC().fill()) {
        // First row
        // Using the += syntax
        contents += new Label("First Name")
        contents += (new TextField(10), CC().growX())
        contents += new Label("Last Name")
        contents += (new TextField(10), CC().wrap().growX())
        contents += new Label("Phone Number")
        // Span 3 items
        contents += (new TextField(30), CC().wrap().spanX(3).growX())
      }, CC().growX().alignY(TopAlign).wrap())

      // Use a Grid on the middle
      add(new MigPanel(LC().fill().debug(0)) {
        // Using the add syntax
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