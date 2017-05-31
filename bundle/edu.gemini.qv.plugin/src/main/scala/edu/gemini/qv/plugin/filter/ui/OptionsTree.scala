package edu.gemini.qv.plugin.filter.ui

import scala.swing.Component
import javax.swing.{ImageIcon, JComponent, JTree, tree}
import javax.swing.tree._
import java.awt.event.{MouseAdapter, MouseEvent}

import edu.gemini.qv.plugin.QvContext

import collection.JavaConverters._
import edu.gemini.qv.plugin.filter.core.{ConfigurationFilter, EmptyFilter, Filter, OptionsFilter}

import scala.Some
import edu.gemini.qv.plugin.data.{DataChanged, ObservationProvider}

/**
 * An options tree that allows users to select/deselect arbitrary combinations of thematically grouped options.
 * Most prominent examples are instrument configurations like e.g. filters and dispersers.
 * Most of the code here is stolen/adapted from QPT. There is currently no scala swing Tree wrapper, therefore
 * this implementation is based on the java swing JTree.
 */
class OptionsTree(ctx: QvContext, data: ObservationProvider, filters: Set[ConfigurationFilter[_]], showAvailableOnly: Boolean = true, showCounts: Boolean = true) extends Component {

  private val IconIndeterminate = new ImageIcon(getClass.getResource("img/check_indefinite.gif"))
  private val IconSelected = new ImageIcon(getClass.getResource("img/check_selected.gif"))
  private val IconUnselected = new ImageIcon(getClass.getResource("img/check_unselected.gif"))

  private var instrumentNodesMap = Map[String, InstrumentNode]()

  object Selection extends Enumeration {
    type Selection = Value
    val Indeterminate, Selected, Unselected = Value
  }
  import Selection._

  override lazy val peer = new JTree()

  // create current content
  private val root = new RootNode
  peer.setShowsRootHandles(true)
  peer.setCellRenderer(new OutlineNodeRenderer)
  peer.setSelectionModel(null)
  peer.addMouseListener(new OutlineNodeMouseListener)
  peer.setToggleClickCount(Integer.MAX_VALUE); // don't expand on multi-clicks
  peer.setRootVisible(false)
  createNodes() // set model for the first time

  listenTo(data)
  reactions += {
    // update model whenever data changes
    // this will reflect changes in the underlying data like presence of instruments etc.
    case DataChanged => {
      root.removeAllChildren()
      createNodes()
    }
//    case FilterChanged => {
//      // gray out instruments that are currently not selected in the main filter
//      val presentInstruments = data.presentValues(Filter.Instruments().collector)
//      val activeInstruments = data.presentValuesFiltered(Filter.Instruments().collector)
//      presentInstruments.foreach { i =>
//        val active = activeInstruments.contains(i)
//        if (instrumentNodesMap.contains(i.readableStr)) {
//          instrumentNodesMap(i.readableStr).enabled = active
//        }
//      }
//    }

  }

  def filter = root.filter

  def activeFilter = root.activeFilter

  private def createNodes() {
    // get available instruments, remove all filters for not-available instruments
    val presentInstruments = data.presentValues(Filter.Instruments().collector(_, ctx))
    val availableFilters = filters.filter(f => presentInstruments.contains(f.instrument))

    // group all filters by their instrument, then create a node per instrument and nodes for all groups
    val instrumentNodes = availableFilters.
      groupBy(_.instrument).
      map({ case (instr, filters) => {
          (new InstrumentNode(instr.readableStr), filters.map(f => new GroupNode(f.group, f)))
        }
      })

    // now add all group nodes to their instrument node and finally add the instrument to the root node
    instrumentNodesMap = Map()
    instrumentNodes.toSeq.sortBy(_._1.label).                         // sort instruments alphabetically
      foreach({ case (instrNode, groupNodes) => {
        groupNodes.toSeq.sortBy(_.label).foreach(instrNode.add)       // sort groups alphabetically and add them to instrument
        root.add(instrNode)
        instrumentNodesMap += instrNode.label -> instrNode            // keep a map of all instrument nodes
      }
    })

    // finally create model and set it
    val model = new DefaultTreeModel(root)
    peer.setModel(model)

  }

  // === specialized renderer and helper classes

  /**
   * Specialized node renderer that deals with showing different icons depending on selection of underlying
   * options (all, some, none).
   * @tparam T
   */
  @SuppressWarnings(Array("serial"))
  private class OutlineNodeRenderer[T] extends DefaultTreeCellRenderer {
    override def getTreeCellRendererComponent(tree: JTree, value: AnyRef, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): JComponent = {
      val ret: OutlineNodeRenderer[T] = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus).asInstanceOf[OutlineNodeRenderer[T]]
      value match {
        case n: Node[_] =>
          n.selection match {
            case Indeterminate => setIcon(IconIndeterminate)
            case Selected => setIcon(IconSelected)
            case Unselected => setIcon(IconUnselected)
          }
          ret.setEnabled(n.enabled)
      }
      return ret
    }
  }

  /** Mouse listener that deals with click events and initiates updates of underlying nodes. */
  private class OutlineNodeMouseListener[T] extends MouseAdapter {
    override def mousePressed(e: MouseEvent) {
      val tree  = e.getSource.asInstanceOf[JTree]
      val row = tree.getRowForLocation(e.getX, e.getY)
      val path = tree.getPathForRow(row)
      if (path != null) {
        val node = path.getLastPathComponent.asInstanceOf[Node[T]]
        // toggle selection according to current state
        // this will update children and parents as needed
        node.selection = node.selection match {
          case Selected => Unselected
          case _ => Selected
        }

        // update the model changes in the UI
        for { o <- path.getPath }
        yield tree.getModel.asInstanceOf[DefaultTreeModel].nodeChanged(o.asInstanceOf[TreeNode])

        // set new filter on data
        publish(FilterElement.FilterElementChanged)

      }
    }

  }

  // === traits and classes to represent different types of nodes

  /** Any node that provides a filter */
  private trait FilterNode {
    def filter: Filter
    def activeFilter: Filter
  }

  private class RootNode extends Node("ROOT", false) with FilterNode {
    setAllowsChildren(true)

    def filter =
      myChildren.map(n => n.asInstanceOf[FilterNode].filter).filter(!_.isEmpty).reduceOption(_.and(_)).getOrElse(new EmptyFilter)

    def activeFilter = {
      // NOTE: because we are working with an or filter here we need to add an instrument filter for all instruments
      // for which we don't have any configuration filters to make sure that observations for these instruments are
      // not filtered by the configuration options tree filter
      val f = myChildren.map(n => n.asInstanceOf[FilterNode].activeFilter).filter(!_.isEmpty).reduceOption(_.or(_)).getOrElse(new EmptyFilter)
      val instrumentsWithoutConfig = Filter.Instruments().values -- filters.map(_.instrument)
      f.or(Filter.Instruments(instrumentsWithoutConfig))
    }

  }

  /** Group node that represents an instrument. */
  private class InstrumentNode(val label: String) extends Node(label,false) with FilterNode {
    setAllowsChildren(true)

    def filter =
      myChildren.map(n => n.asInstanceOf[FilterNode].filter).filter(!_.isEmpty).reduceOption(_.and(_)).getOrElse(new EmptyFilter)

    def activeFilter =
      myChildren.map(n => n.asInstanceOf[FilterNode].filter).reduceOption(_.and(_)).getOrElse(new EmptyFilter)
  }

  /** Group node that represents a group of options, e.g. filters, masks etc. */
  private class GroupNode[T](val label: String, f: OptionsFilter[T]) extends Node(label, false) with FilterNode {
    // check which values are actually available
    val presentOptions = data.presentValuesWithCount(f.collector(_, ctx))

    // filter available options (if show available only) and add those options..
    setAllowsChildren(true)
    if (showAvailableOnly) {
      f.sortedValues.
//        filter(presentOptions.contains).
        foreach(v => add(new OptionNode[T](f, v, if (presentOptions.contains(v)) presentOptions(v) else 0)))
    } else {
      f.sortedValues.
        foreach(v => add(new OptionNode[T](f, v, 1)))
    }

    def filter = f.updated(curSelection)
    def activeFilter = filter

    private def curSelection: Set[T] = {
      myChildren.
        filter(n => n.asInstanceOf[OptionNode[T]].selection == Selected).
        map(n => n.asInstanceOf[OptionNode[T]].value.asInstanceOf[T]).
        toSet
    }
  }

  /** Leaf node that represents a single available option. */
  private class OptionNode[T](f: OptionsFilter[T], val value: T, count: Int) extends Node(if (showCounts) s"${f.valueName(value)}  ($count)" else f.valueName(value), f.selection.contains(value)) {
    enabled = count > 0
  }

  /** Base class for nodes in the options tree.
    * This class mainly deals with propagating changes up and down the tree, it covers all nodes, including leafs.
    * @param value
    */
  private abstract class Node[T](value: T, selected: Boolean) extends DefaultMutableTreeNode(value) {
    private var _selection = if (selected) Selected else Unselected
    private var _enabled = true

    def enabled = _enabled
    def enabled_=(e: Boolean) {
      _enabled = e
      myChildren.foreach { _.enabled = e }
    }

    /** Selection getter. */
    def selection = _selection

    /** Selection setter. */
    def selection_=(s: Selection) {
      _selection = s
      updateChildren(s)
      updateParent(s)
    }

    /** Propagates a new selection value to all parents.
      * @param s
      */
    private def updateParent(s: Selection): Unit =
      myParent.map(p => {p._selection = p.compareWithChildren(s); p.updateParent(s)})

    /** Propagates a new selection value to all children.
      * @param s
      */
    private def updateChildren(s: Selection): Unit =
      myChildren.map(c => {c._selection = s; c.updateChildren(s)})

    /** Gets the children of this node.
      * Helper method to deal with null values and convert from Java nodes to a typed Scala sequence.
      * @return
      */
    def myChildren: Seq[Node[T]] =
      if (children == null) Seq()
      else children.asScala.map(_.asInstanceOf[Node[T]])

    /** Gets the parent of this node as an option.
      * Helper method to deal with null values and convert from Java to Scala type.
     * @return
     */
    private def myParent: Option[Node[T]] =
      if (parent == null) None
      else Some(parent.asInstanceOf[Node[T]])

    /** Checks if all children have the same value.
     * @param s
     * @return the selection if all children have the same selection value, Indterminate otherwise
     */
    private def compareWithChildren(s: Selection): Selection =
      myChildren.foldLeft(s)((curSel,c) => if (curSel == c.selection) curSel else Indeterminate)

    override def add(node: MutableTreeNode) = {
      // call super to actually add node
      super.add(node)
      // when inserting a node we need to update all parents of this node
      val n = node.asInstanceOf[Node[T]]
      n.updateParent(n.selection)
    }

  }

}
