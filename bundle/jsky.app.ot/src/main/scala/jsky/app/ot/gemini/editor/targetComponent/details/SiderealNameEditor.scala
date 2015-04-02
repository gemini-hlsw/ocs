package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Insets, GridBagConstraints, GridBagLayout}
import javax.swing.JPanel

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

import scalaz.concurrent.Task
import scalaz.syntax.id._
import scalaz.{ \/-, -\/ }

// Name editor, with catalog lookup for sidereal targets
final class SiderealNameEditor extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget // never null

  val name = new TextBoxWidget <| { w =>
    w.setColumns(20)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.getTarget.setName(tbwe.getValue)
          spt.notifyOfGenericUpdate()
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        lookupTarget(cats.getSelectedItem.asInstanceOf[Catalog], tbwe.getValue).runAsync {
          case \/-(r) => processResult(r)
          case -\/(e) => DialogUtil.error(e.getMessage)
        }

    })
  }

  val cats = new DropDownListBoxWidget <| { m =>
    m.setChoices(SkycatConfigFile.getConfigFile.getNameServers)
  }

  setLayout(new GridBagLayout)

  add(name, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.fill = GridBagConstraints.HORIZONTAL
    c.weightx = 2
  })

  add(cats, new GridBagConstraints <| { c =>
    c.gridx = 1
    c.gridy = 0
    c.insets = new Insets(0, 2, 0, 0)
  })

  def edit(ctx: GOption[ObsContext], target: SPTarget): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(spt.getTarget.getName)
    }
  }

  def lookupTarget(catalog: Catalog, name: String): Task[TableQueryResult] =
    Task {

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
          t.getRa.setValue(pos.getRA.toString)
          t.getDec.setValue(pos.getDec.toString)
        } catch {

          // N.B. the old target editor handled this case, so we do too. May never happen.
          case _: IllegalArgumentException =>
            t.getRa.setAs(0, Units.DEGREES)
            t.getDec.setAs(0, Units.DEGREES)

        }
      case _ => // ???
    }

    // Notify the world
    spt.notifyOfGenericUpdate()

  }

}
