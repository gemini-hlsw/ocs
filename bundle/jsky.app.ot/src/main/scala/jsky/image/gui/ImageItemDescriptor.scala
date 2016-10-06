package jsky.image.gui

import jsky.coords.WorldCoords
import jsky.image.ImageProcessor
import java.io.Serializable
import java.net.URL

import scalaz._
import Scalaz._

case class ImageItemDisplayProperties(scale: Float, cmap: String, itt: String, hcut: Double, lcut: Double, userSetCutLevels: Boolean, scaleAlg: Int)
case class ImageItemId(title: String, url: URL, filename: String)

case class ImageItemDescriptor private (imageId: ImageItemId, raDeg: Double, decDeg: Double, widthDeg: Double,
     heightDeg: Double, imageDisplayProperties: ImageItemDisplayProperties) extends Serializable {

  /**
    * Returns the distance from the history item image center to the given ra,dec position
    * in arcmin, if the position is on the image, otherwise None.
    */
  def matches(raDeg: Double, decDeg: Double): Option[Double] = {
    def calcDistance: Option[Double] = {
      val halfWidth = widthDeg / 2.0
      val halfHeight = heightDeg / 2.0

      val decDifference = Math.abs(decDeg - this.decDeg)
      val isInsideImageOnDec = decDifference <= halfHeight

      def calcDiff: Option[Double] = {
        def isOutsideImageOnRa(ra: Double) = Math.abs(ra - this.raDeg) < halfWidth

        isOutsideImageOnRa(raDeg).fold(raDeg.some, {
          // Make sure that if both RAs are around 0, they aren't close enough.
          if (raDeg > (360.0 - halfWidth)) {
            val rd = raDeg - 360
            isOutsideImageOnRa(rd) option rd
          } else if (raDeg < halfWidth) {
            val rd = raDeg + 360
            isOutsideImageOnRa(rd) option rd
          } else {
            none
          }
        }).map(WorldCoords.dist(_, decDeg, this.raDeg, this.decDeg))
      }

      isInsideImageOnDec.fold(calcDiff, None)
    }
    List(this.raDeg, this.decDeg, widthDeg, heightDeg).forall(!_.isNaN).fold(calcDistance, None)
  }

  /**
    * Signature appropriate for Java clients
    */
  def matches4Java(raDeg: Double, decDeg: Double): Option[java.lang.Double] = {
    matches(raDeg, decDeg).map(java.lang.Double.valueOf)
  }
}

object ImageItemDescriptor {
  /**
    * Create an image history item data based on the given arguments.
    *
    * @param imageDisplay the image display widget
    * @param raDeg        the image center RA coordinate
    * @param decDeg       the image center Dec coordinate
    * @param widthDeg     the image width in deg
    * @param heightDeg    the image height in deg
    * @param title        the title for the history menu
    * @param url          the URL for the original image
    * @param filename     the local filename, if downloaded
    */
  def apply(imageDisplay: MainImageDisplay, raDeg: Double, decDeg: Double, widthDeg: Double, heightDeg: Double, title: String, url: URL, filename: String): ImageItemDescriptor = {
    val scale = imageDisplay.getScale
    val imageProcessor: ImageProcessor = imageDisplay.getImageProcessor
    val cmap = imageProcessor.getColorLookupTableName
    val itt = imageProcessor.getIntensityLookupTableName
    val hcut = imageProcessor.getHighCut
    val lcut = imageProcessor.getLowCut
    val userSetCutLevels = imageProcessor.isUserSetCutLevels
    val scaleAlg = imageProcessor.getScaleAlgorithm

    val imageId = ImageItemId(title, url, filename)
    val displayProperties = ImageItemDisplayProperties(scale, cmap, itt, hcut, lcut, userSetCutLevels, scaleAlg)
    ImageItemDescriptor(imageId, raDeg, decDeg, widthDeg, heightDeg, displayProperties)
  }
}
