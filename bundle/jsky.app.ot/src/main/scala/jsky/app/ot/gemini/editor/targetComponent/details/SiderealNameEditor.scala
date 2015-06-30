package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.catalog.skycat.CatalogException
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.HmsDegTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.catalog.skycat.{SkycatConfigFile, FullMimeSimbadCatalogFilter, SkycatCatalog}
import jsky.catalog.{BasicQueryArgs, TableQueryResult, Catalog}
import jsky.coords.WorldCoords
import jsky.util.gui.{DialogUtil, DropDownListBoxWidget, TextBoxWidgetWatcher, TextBoxWidget}

import scalaz.syntax.id._
import scalaz.{ \/-, -\/ }

// Name editor, with catalog lookup for sidereal targets
final class SiderealNameEditor extends TelescopePosEditor with ReentrancyHack {
  private[this] var spt = new SPTarget // never null

  def forkSearch(): Unit = {
    val cat = cats.getSelectedItem.asInstanceOf[Catalog]
    forkSwingWorker(lookupTarget(cat, name.getValue)) {
      case \/-(r) => processResult(r)
      case -\/(e) => DialogUtil.error(e.getMessage)
    }
  }

  val name = new TextBoxWidget <| { w =>
    w.setColumns(20)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setName(tbwe.getValue)
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        forkSearch()
    })
  }

  val search = searchButton(forkSearch())

  val cats = new DropDownListBoxWidget <| { m =>
    m.setChoices(SkycatConfigFile.getConfigFile.getNameServers)
    m.setVisible(false) // just hide this for now
  }

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(spt.getTarget.getName)
    }
  }

  def lookupTarget(catalog: Catalog, name: String): TableQueryResult = {

    // If it's a SkycatCatalog lookup on simbad, add a filter. No idea why.
    catalog match {
      case skycat: SkycatCatalog if skycat.getShortName.contains("simbad") =>
        skycat.addCatalogFilter(FullMimeSimbadCatalogFilter.getFilter)
      case _ => // do nothing special
    }

    // Prepare the query
    val queryArgs = new BasicQueryArgs(catalog) <| { as =>
      as.setId(name)
      as.setMaxRows(1)
    }

    // Go .. this is a blocking call
    catalog.query(queryArgs) match {
      case tqr: TableQueryResult => tqr
      case e: Exception => throw e
      case a => throw new CatalogException("Unexpected result: " + a)
    }

  }


  def processResult(tqr: TableQueryResult): Unit = {
    val t = spt.getTarget.asInstanceOf[HmsDegTarget] // always works, heh-heh

    // Get a value from the first row, which is the only one we look at
    def getValue(n: Int) = tqr.getValueAt(0, n)

    // Set the PM, if any
    tqr.getColumnIndex("pm1") <| { pm =>
      if (pm >= 0) {
        (getValue(pm), getValue(pm + 1)) match {
          case (ra: java.lang.Double, dec: java.lang.Double) =>
            t.setPropMotionRA(ra)
            t.setPropMotionDec(dec)
          case _ => // ???
        }
      } else {
        t.setPropMotionRA(0.0)
        t.setPropMotionDec(0.0)
      }
    }

    // Set the coordinates, if any
    tqr.getCoordinates(0) match {
      case pos: WorldCoords =>
        try {
          t.setRaString(pos.getRA.toString)
          t.setDecString(pos.getDec.toString)
        } catch {

          // N.B. the old target editor handled this case, so we do too. May never happen.
          case _: IllegalArgumentException =>
            new SPTarget(t).setRaDegrees(0)
            new SPTarget(t).setDecDegrees(0)

        }
      case _ => // ???
    }

    // Notify the world
    spt.notifyOfGenericUpdate()

  }

}
