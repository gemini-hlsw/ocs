package edu.gemini.catalog.ui.tpe

import java.net.URL
import javax.swing.event.ChangeListener

import edu.gemini.catalog.api.{MagnitudeLimits, RadiusLimits}
import jsky.catalog.Catalog
import jsky.catalog.QueryResult
import jsky.catalog.TableQueryResult
import jsky.catalog.gui.{TablePlotter, BasicTablePlotter, CatalogNavigatorOpener}
import jsky.coords.{WorldCoords, WorldCoordinateConverter, CoordinateConverter}
import jsky.graphics.CanvasGraphics
import jsky.image.fits.codec.FITSImage
import jsky.image.fits.gui.FITSKeywordsFrame
import jsky.image.fits.gui.FITSKeywordsInternalFrame
import jsky.image.gui.ImageDisplayMenuBar
import jsky.image.gui.ImageDisplayToolBar
import jsky.image.gui.{ImageGraphicsHandler, DivaMainImageDisplay}
import jsky.navigator._
import jsky.util.gui.DialogUtil
import javax.swing._
import java.awt._
import java.awt.event.{ActionListener, ActionEvent}
import java.awt.geom.AffineTransform
import java.util.HashSet
import java.util.Set

import scalaz._
import Scalaz._

/**
  * Interface to encapsulate an object that can display a catalog and interact with the TPE
  */
trait CatalogDisplay {
  def plotter: TablePlotter

  def getParentFrame: Component

  def saveFITSTable(table: TableQueryResult): Unit

  def getNavigatorPane: NavigatorPane

  def getCanvasGraphics: CanvasGraphics

  def getCoordinateConverter: CoordinateConverter

  def setNavigator(navigator: Navigator): Unit

  def setFilename(fileOrUrl: String, url: URL): Unit

  // Used to get the position of the TPE
  /**
    * Return the object used to convert between image and world coordinates,
    * or null if none was set.
    */
  def getWCS: WorldCoordinateConverter

  /**
    * Return the base or center position in world coordinates.
    * If there is no base position, this method returns the center point
    * of the image. If the image does not support WCS, this method returns (0,0).
    * The position returned here should be used as the base position
    * for any catalog or image server requests.
    */
  def getBasePos: WorldCoords

  /**
    * Return the default min and max search radius values to use for catalog searches, in arcmin.
    *
    * @param centerPos the center position for the radius
    * @param useImageSize if true, use the image size to get the search radius
    * @return radius values
    */
  def getDefaultSearchRadius(centerPos: WorldCoords, useImageSize: Boolean): RadiusLimits

  /**
    * Return the default min and max magnitude values to use for catalog searches, or null
    * if there is no default.
    *
    * @return magnitude limits, including band
    */
  def getDefaultSearchMagRange: MagnitudeLimits

  // TODO, Remove used only by MaskDisplay....
  /**
    * Return the width of the source image in pixels
    */
  def getImageWidth: Int

  /**
    * Return the height of the source image in pixels
    */
  def getImageHeight: Int

  /**
    * Register as an image graphics handler.
    */
  def addImageGraphicsHandler(igh: ImageGraphicsHandler)

  /**
    * register to receive change events from this object whenever the
    * image or cut levels are changed.
    */
  def addChangeListener(l: ChangeListener)

  /**
    * If the current image is in FITS format, return the FITSImage object managing it,
    * otherwise return null. (The FITSImage object is available via the "#fits_image"
    * property from the FITS codec, which implements FITS support for JAI.)
    */
  def getFitsImage: FITSImage
}

/**
  * Extends the DivaMainImageDisplay class by adding support for
  * browsing catalogs and plotting catalog symbols on the image.
  */
abstract class CatalogImageDisplay(parent: Component, navigatorPane: NavigatorPane) extends DivaMainImageDisplay(navigatorPane, parent) with CatalogDisplay {
  val plotter = new BasicTablePlotter(getCanvasGraphics, getCoordinateConverter) <| {navigatorPane.setPlotter}

  // TODO Move to scala collection
  /** Set of filenames: Used to keep track of the files visited in this session. */
  private final val _filesVisited: Set[String] = new HashSet[String]

  /** Return the Diva pane containing the added catalog symbol layer. */
  override def getNavigatorPane: NavigatorPane = navigatorPane

  /**
    * Set the instance of the catalog navigator to use with this image display.
    */
  override def setNavigator(navigator: Navigator):Unit = ???

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
        case frame: FITSKeywordsInternalFrame =>
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
    */
  override def saveFITSTable(table: TableQueryResult):Unit = throw new UnsupportedOperationException()

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
      // TODO Fix saving the tables
      /*if (_navigatorFrame != null) {
        val filename = getFilename
        val fitsImage = getFitsImage
        if (fitsImage != null && filename != null) {
          if (!_filesVisited.contains(filename)) {
            _filesVisited.add(filename)
            try {
              NavigatorFITSTable.plotTables(filename, fitsImage.getFits, _navigator)
            } catch {
              case e: Exception =>
                DialogUtil.error(this, e)
            }
          }
        }
      }*/
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

  /**
    * Called when an object is selected in the Pick Object window.
    * <p>
    * Add the currently selected object in the "Pick Object" window to the currently
    * displayed table, or create a new table if none is being displayed.
    */
  protected override def pickedObject(): Unit = {
    val stats = getPickObjectPanel.getStatistics
    if (stats == null) {
      DialogUtil.error("No object was selected")
      return
    }
    // TODO Support picked objects
    //_navigator.addPickedObjectToTable(stats, getPickObjectPanel.isUpdate)
    ???
  }

  /**
    * Can be overridden in a derived class to filter the result of a catalog query.
    */
  def filterQueryResult(queryResult: QueryResult): QueryResult = queryResult
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

  // TODO These two items don't seem to be used, check if they could be deprecated
  val pickObjectMenuItem = getPickObjectMenuItem
  getViewMenu.remove(pickObjectMenuItem)
  _catalogMenu.add(catalogTreeMenu)
  _catalogMenu.add(pickObjectMenuItem)
  _catalogMenu.addSeparator()
  _catalogMenu.add(createSaveCatalogOverlaysWithImageMenuItem)
  add(_catalogMenu)

  /**
    * Create a menu item for saving the current catalog overlays as a FITS table in the image file.
    */
  @Deprecated
  private def createSaveCatalogOverlaysWithImageMenuItem: JMenuItem = {
    val menuItem = new JMenuItem("Save Catalog Overlays With Image")
    menuItem.addActionListener(new ActionListener() {
      override def actionPerformed(e: ActionEvent): Unit = imageDisplay.saveCatalogOverlaysWithImage()
    })
    menuItem
  }

  /** Return the handle for the Catalog menu */
  def getCatalogMenu: JMenu = _catalogMenu

  /** Return the handle for the Help menu */
  override def getHelpMenu: JMenu = _helpMenu
}
