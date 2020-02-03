package edu.gemini.pit.ui.util

 import scala.swing._
 import scala.swing.event._
 import scala.swing.Swing._
 import java.awt
 import awt.geom.Rectangle2D
 import awt.image.BufferedImage
 import awt.{Color, TexturePaint}
 import java.awt.datatransfer.DataFlavor
 import java.awt.dnd._
 import java.awt.dnd.DragSource.{getDefaultDragSource => ds}

 import javax.swing.border.Border
 import edu.gemini.ui.gface.GSelection

object Test extends App {

  import ReorderBar._

  val f = new MainFrame {
    contents = new BorderPanel {
      add(new ReorderBar("Conditions", "Instrument", "Target")("Drag items to rearrage.") {
        reactions += {
          case Reorder(as0) => println("New order: " + as0)
        }
      }, BorderPanel.Position.North)
    }
  }

  f.visible = true

}

object ReorderBar {

  // Some constants
  val CellSpacing = 12

  // An event indicating that the elements have been re-ordered
  case class Reorder[A](as:Seq[A]) extends Event

  // MouseAdapter that converts to Scala-style events.
  // For example: peer.addMouseListener(MouseAdapter(publish))
  private def MouseAdapter(f:MouseEvent => Unit) = new awt.event.MouseAdapter {
    override def mouseClicked(e:awt.event.MouseEvent) {
      f(new MouseClicked(e))
    }
    override def mouseMoved(e:awt.event.MouseEvent) {
      f(new MouseMoved(e))
    }
    override def mouseEntered(e:awt.event.MouseEvent) {
      f(new MouseEntered(e))
    }
    override def mouseExited(e:awt.event.MouseEvent) {
      f(new MouseExited(e))
    }
  }

  // Arithmetic on AWT points.
  implicit def pimpPoint(a:awt.Point) = new Object {
    def +(b:awt.Point):Point = (a.x + b.x, a.y + b.y)
    def -(b:awt.Point):Point = (a.x - b.x, a.y - b.y)
    def signum:Point = (a.x.signum, a.y.signum)
  }

  // Item state
  object ItemState extends Enumeration {
    val Dropped, Dragging, Dropping = Value
    type ItemState = Value
  }

  // Item border :-/
  object ItemBorder extends Border {

    val insets = new awt.Insets(2, 15, 2, 5)
    val isBorderOpaque = true
    val gray = new Color(0xBBBBBB)

    // A stipple paint
    object paint extends TexturePaint(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB) {
      setRGB(1, 1, 0xFF808080)
      setRGB(2, 2, 0xFFFFFFFF)
    }, new Rectangle2D.Double(2, 2, 3, 3))

    def getBorderInsets(c:awt.Component): Insets = insets

    def paintBorder(c:awt.Component, _g:awt.Graphics, x:Int, y:Int, w:Int, h:Int) {
      val g:awt.Graphics2D = _g.asInstanceOf[Graphics2D]
      val prevColor = g.getColor
      val prevPaint = g.getPaint
      try {
        g.setColor(gray)
        g.drawRect(0, 0, w - 1, h - 1)
        g.setPaint(paint)
        g.fillRect(2, 2, 10, h - 4)
      } finally {
        g.setColor(prevColor)
        g.setPaint(prevPaint)
      }
    }

  }

}

class ReorderBar[A](val as:A*)(helpMessage:String = "Drag items to rearrange.") extends Component {

  import ReorderBar._
  import ReorderBar.ItemState._

  val Flavor: DataFlavor = GSelection.flavorForSelectionOf(classOf[ReorderBar[A]#Item])

  // Create and add our items. We use this list if we don't care about their order.
  private val items = {
    val is = as.map(Item)
    is.map(_.peer).foreach(peer.add)
    is
  }

  // Calculate our preferred size and perform initial layout
  preferredSize = (items.map(CellSpacing + _.size.width).sum, items.map(_.size.height).max)
  layout(true)

  // Our data elements, sorted left to right.
  def elems: List[A] = orderedItems.map(_.a).toList

  // Sync this bar with another one
  def copyStateFrom(other:ReorderBar[A]) {
    require(as == other.as, "Bars have different elements.")
    if (elems != other.elems) {
      items.zip(other.items).foreach {
        case (i, i0) => i.location = i0.location
      }
      layout(true)
      assert(elems == other.elems) // HACK: it seems that this sometimes doesn't work. check on it
      publish(Reorder(elems))
    }
  }

  // Our items, sorted left to right.
  private def orderedItems: Seq[Item] = items.sortBy(_.location.x)

  // Our visible items
  private def layoutItems: Seq[Item] = orderedItems.filter(_.state != Dragging)

  // Calculate the x-offsets of space-occupying items based on their state
  private def offsets = {
    val widths = layoutItems.filter(_.state != Dragging).map(_.size.width + CellSpacing)
    (List(CellSpacing) /: widths)((is, j) => (j + is.head) :: is).reverse
  }

  // Our layout routine. We only care about x values
  private def layout(initial:Boolean = false) {
    layoutItems.zip(offsets).foreach {
      case (i, x) =>
        if (initial) {
          i.location = (x, 0)
        } else {
          i.destination = (x, 0)
        }
    }
  }

  ///
  /// DND - we drag items and drop them on the reorderbar itself
  ///

  // We need to unmap items from their peers and toStrings (sigh)
  private def fromPeer(c:awt.Component): Option[Item] = items.find(_.peer == c)

  // Our DragSourceListener, common to all items
  object dsl extends DragSourceAdapter {
    override def dragDropEnd(dsde:DragSourceDropEvent) {
      if (dsde.getDropSuccess) {
        ReorderBar.this.publish(Reorder(elems))
      } else {
        fromPeer(dsde.getDragSourceContext.getComponent).foreach(_.state = Dropped)
      }
    }
  }

  // Our DragGestureListener is shared by all items
  object dgl extends DragGestureListener {
    def dragGestureRecognized(e:DragGestureEvent) {
      fromPeer(e.getComponent) foreach {i =>
        i.state = Dragging
        layout()
        e.startDrag(DragSource.DefaultMoveDrop, new GSelection(i), dsl)
      }
    }
  }

  // Our DropTargetListener
  object dtl extends DropTargetAdapter {

    // State is optionally an item and its original location
    private var item:Option[(Item, Point)] = None

    // On enter, capture whatever is being dragged.
    override def dragEnter(e:DropTargetDragEvent) {
      e.getTransferable.getTransferDataFlavors.foreach(println)
      val i = e.getTransferable.getTransferData(Flavor).asInstanceOf[GSelection[ReorderBar[A]#Item]]
      item = items.find(_.a == i.first.a).map {i => (i, i.location)}
      dragOver(e)
    }

    // On dragover,
    override def dragOver(e:DropTargetDragEvent) {
      if (droppable(e.getLocation)) {
        e.acceptDrag(DnDConstants.ACTION_MOVE)
        item.foreach {
          case (i, pos) =>
            i.location = (e.getLocation.x, 0)
            i.state = Dropping
        }
      } else reset()
    }

    def drop(e:DropTargetDropEvent) {
      item.foreach {
        case (i, pos) =>
          e.acceptDrop(DnDConstants.ACTION_MOVE)
          e.dropComplete(true)
          i.state = Dropped
      }
    }

    override def dragExit(e:DropTargetEvent) {
      reset()
      item = None
    }

    private def reset() {
      item.foreach {
        case (i, pos) =>
          i.location = pos
          i.state = Dragging
      }
    }

    private def droppable(p:awt.Point): Boolean = {
      val over = peer.getComponentAt(p)
      over == peer || !over.isVisible
    }

  }

  // Hook up dragging for all items.
  items.foreach {i =>
    ds.createDefaultDragGestureRecognizer(i.peer, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
  }

  // And dropping (we can discard the reference)
  new DropTarget(peer, DnDConstants.ACTION_MOVE, dtl, true);

  // A self-sizing, label-like UI element that can move itself around
  private case class Item(val a:A) extends Label(a.toString) {

    // Our destination and state are mutable
    private var _dest:Point = (0, 0)
    private var _state:ItemState = Dropped

    // Our border. TODO: make it prettier
    border = ItemBorder

    // Go ahead and set our own size
    peer.setSize(preferredSize)

    // This label publishes mouse events
    peer.addMouseListener(MouseAdapter(publish))

    // On click
    reactions += {
      case _:MouseClicked =>
        Dialog.showMessage(this, helpMessage, "Hint", Dialog.Message.Info)
    }

    // Mutator for destination.
    def destination: Point = _dest
    def destination_=(p:Point) {
      _dest = p
       animator ! 'Go
    }

    // Mutator for state
    def state: ItemState = _state
    def state_=(s:ItemState) {
      if (s != _state) {
        _state = s
        visible = _state == Dropped
        layout()
      }
    }

    // Mutator for location
    def location_=(p:awt.Point) {
      peer.setLocation(p)
    }

    // The animator is very simple; whenever we receive a 'Go command we start looping
    // until we have moved location to its destination. When they're the same, we stop.
    // This allows us to change locations in mid-stride without ill effects.
     private val animator = actor {
       loop {
         react {
           case 'Go =>
             location = location + (destination - location).signum
             reactWithin(2) {
               case TIMEOUT if (destination != location) => actorSelf ! 'Go
               case x                                    => actorSelf ! x
             }
         }
       }
     }
  //
  }

}
