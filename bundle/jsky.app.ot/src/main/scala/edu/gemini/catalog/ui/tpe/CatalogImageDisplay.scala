package edu.gemini.catalog.ui.tpe

import jsky.catalog.{Catalog, CatalogDirectory, TableQueryResult}
import jsky.catalog.gui.{StoreImageServerAction, CatalogNavigator, TablePlotter, BasicTablePlotter}
import jsky.coords.WorldCoords
import jsky.image.fits.gui.FITSKeywordsFrame
import jsky.image.gui.ImageDisplayMenuBar
import jsky.image.gui.ImageDisplayToolBar
import jsky.image.gui.DivaMainImageDisplay
import jsky.navigator._
import jsky.util.Preferences
import jsky.util.gui.{ProxyServerDialog, DialogUtil}
import javax.swing._
import java.awt._
import java.awt.event.{ActionListener, ActionEvent}
import java.awt.geom.AffineTransform

import scalaz._
import Scalaz._

/**
  * Interface to encapsulate an object that can display a catalog and interact with the TPE
  */
trait CatalogDisplay {
  def plotter: TablePlotter

  /**
    * Return the base or center position in world coordinates.
    * If there is no base position, this method returns the center point
    * of the image. If the image does not support WCS, this method returns (0,0).
    * The position returned here should be used as the base position
    * for any catalog or image server requests.
    */
  def getBasePos: WorldCoords
}

/**
  * Extends the DivaMainImageDisplay class by adding support for
  * browsing catalogs and plotting catalog symbols on the image.
  */
abstract class CatalogImageDisplay(parent: Component, navigatorPane: NavigatorPane) extends DivaMainImageDisplay(navigatorPane, parent) with CatalogDisplay {
  val plotter = new BasicTablePlotter(getCanvasGraphics, getCoordinateConverter) <| {navigatorPane.setPlotter}

  /**
    * Load the sky image for the current location
   */
  def loadSkyImage(): Unit

  /** Display the FITS table at the given HDU index. */
  override def displayFITSTable(hdu: Int):Unit = {
    try {
      val fitsImage = getFitsImage
      val table = new NavigatorFITSTable(getFilename, fitsImage.getFits, hdu)
      //openCatalogWindow(table.getCatalog)
      val fitsKeywordsFrame = getFitsKeywordsFrame
      Option(fitsKeywordsFrame).foreach {
        case frame: FITSKeywordsFrame =>
          frame.getFITSKeywords.updateDisplay(hdu)
        case _ =>
      }
    } catch {
      case e: Exception =>
        DialogUtil.error(this, e)
    }
  }

  /**
    * Save (or update) the given table as a FITS table in the current FITS image.
    * NOTE This function was called from the removed menu "Save Image With Catalog Overlays"
    */
  def saveFITSTable(table: TableQueryResult):Unit = {
    Option(getFitsImage).ifNone {
      DialogUtil.error(this, "This operation is only supported on FITS files.")
    }
    Option(getFitsImage).foreach { i =>
      try {
        val newTable = NavigatorFITSTable.saveWithImage(getFilename, i.getFits, table)
        Option(newTable).foreach { t =>
          setSaveNeeded(true)
          checkExtensions(true)
          plotter.unplot(table)
          // TODO Should the table be displaye?
          //setQueryResult(newTable.getCatalog)
        }
      } catch {
        case e: Exception =>
          DialogUtil.error(this, e)
      }
    }
  }

  /**
    * This method is called before and after a new image is loaded, each time
    * with a different argument.
    *
    * @param before set to true before the image is loaded and false afterwards
    */
  protected override def newImage(before: Boolean) {
    super.newImage(before)
    if (!before) {
      // replot
      Option(plotter).foreach(_.replotAll())
    }
  }

  /**
    * Transform the image graphics using the given AffineTransform.
    */
  @Deprecated
  protected override def transformGraphics(trans: AffineTransform) {
    super.transformGraphics(trans)
    plotter.transformGraphics(trans)
  }

  /** Save any current catalog overlays as a FITS table in the image file. */
  def saveCatalogOverlaysWithImage(): Unit = {
    Option(plotter).foreach { p =>
      val tables = p.getTables
      Option(tables).foreach { t =>
        for (table <- t) saveFITSTable(table)
      }
    }
  }

}

/**
  * Extends the image display menubar by adding a catalog menu.
  */
class CatalogImageDisplayMenuBar(protected val imageDisplay: CatalogImageDisplay, toolBar: ImageDisplayToolBar) extends ImageDisplayMenuBar(imageDisplay, toolBar) {
  /** Handle for the Image menu */
  private val _catalogMenu = new JMenu("Catalog")
  /** Handle for the Help menu */
  private val _helpMenu = new JMenu("Help")

  val catalogTreeMenu = new NavigatorCatalogMenu(imageDisplay)

  val pickObjectMenuItem = getPickObjectMenuItem
  getViewMenu.remove(pickObjectMenuItem)
  catalogTreeMenu.imageServersMenu.foreach(_catalogMenu.add)
  catalogTreeMenu.proxyMenuItem.foreach(_catalogMenu.add)

  _catalogMenu.add(pickObjectMenuItem)
  add(_catalogMenu)

  /** Return the handle for the Catalog menu */
  def getCatalogMenu: JMenu = _catalogMenu

  /** Return the handle for the Help menu */
  override def getHelpMenu: JMenu = _helpMenu
}

/**
  * Create the menubar for the given main image display.
  *
  * @param opener the object responsible for creating and displaying the catalog window
  */
class NavigatorCatalogMenu(opener: CatalogImageDisplay) {

  val (imageServersMenu, proxyMenuItem) = {
    try {
      val dir = CatalogNavigator.getCatalogDirectory
      val imageMenu = imageServersSubMenu(dir)
      (imageMenu.some, proxySettingsMenuItem().some)
    } catch {
      case e: Exception =>
        DialogUtil.error(e)
        (None, None)
    }
  }

  /**
    * Create and return a submenu listing catalogs of the given type.
    *
    * @param dir the catalog directory (config file) reference
    * @return the ne or updated menu
    */
  private def imageServersSubMenu(dir: CatalogDirectory): JMenu = {
    val menu = new JMenu("Image Servers")
    Option(dir).foreach { d =>
      val n = d.getNumCatalogs
      val b = new ButtonGroup
      val userCat = userPreferredCatalog

      for {
        i <- 0 until n
        c = d.getCatalog(i)
        if c.isImageServer
      } {
        val mi = imageServersMenuItem(c)
        menu.add(mi)
        b.add(mi)
        userCat.filter(k => k.getName.equals(c.getName)).foreach(_ => mi.setSelected(true))
      }
    }
    menu
  }

  /**
    * Create a menu item for accessing a specific catalog.
    */
  private def imageServersMenuItem(cat: Catalog): JMenuItem = {
    val a = StoreImageServerAction.getAction(cat)
    val menuItem = new JRadioButtonMenuItem(a) <| {_.setText(cat.getName)} <| {_.addActionListener(new ActionListener() {
        override def actionPerformed(e: ActionEvent): Unit = {
          // First save the preference, then load the image
          a.actionPerformed(e)
          opener.loadSkyImage()
        }
      })}
    a.appendValue("MenuItem", menuItem)
    menuItem
  }

  private def userPreferredCatalog: Option[Catalog] = {
    val args = Preferences.get(Catalog.SKY_USER_CATALOG, Catalog.DEFAULT_IMAGE_SERVER).split("\\*")
    val t = args.nonEmpty option {
      val c = CatalogNavigator.getCatalogDirectory.getCatalog(args(0))
      Option(c).filter(_.isImageServer)
    }
    t.flatten
  }

  /**
    * Create the Catalog => "Proxy Settings..." menu item
    */
  private def proxySettingsMenuItem(): JMenuItem = {
    val proxyMenu = new JMenuItem("Proxy Settings...")
    proxyMenu.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        new ProxyServerDialog() <| {_.setVisible(true)}
      }
    })
    proxyMenu
  }

}