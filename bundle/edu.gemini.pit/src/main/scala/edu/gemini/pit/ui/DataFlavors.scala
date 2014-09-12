package edu.gemini.pit.ui

import view.obs.{ObsListElem, ObsListModel}
import edu.gemini.model.p1.immutable.Target
import java.awt.datatransfer.{UnsupportedFlavorException, Transferable, DataFlavor}

object DataFlavors {

  // A transferable that supports a single flavor, for JVM-local data transfer
  abstract class SingleFlavorTransferable[A <: AnyRef](value:Option[A])(implicit m:Manifest[A]) extends Transferable {

    val flavor = new DataFlavor(
      DataFlavor.javaJVMLocalObjectMimeType + ";class=%s".format(m.runtimeClass.getName),
      "Data flavor for " + m.runtimeClass.getName,
      getClass.getClassLoader)

    val getTransferDataFlavors:Array[DataFlavor] = Array(flavor)

    def isDataFlavorSupported(f:DataFlavor):Boolean = f == flavor && value.isDefined

    def getTransferData(f:DataFlavor):A = {
      if (!isDataFlavorSupported(f))
        throw new UnsupportedFlavorException(f)
      assert(value.isDefined) // should be guaranteed by isDataFlavorSupported returning true
      value.get
    }

  }

  case class TransferableTarget(t:Option[Target]) extends SingleFlavorTransferable(t)
  case class TransferableTargetList(ts:List[Target]) extends SingleFlavorTransferable(Some(ts).filterNot(_.isEmpty))
  case class TransferableObsListElem(ole:Option[(ObsListModel, ObsListElem)]) extends SingleFlavorTransferable(ole)

  lazy val ObsListElemFlavor = TransferableObsListElem(None).flavor
  lazy val TargetFlavor = TransferableTarget(None).flavor
  lazy val TargetListFlavor = TransferableTargetList(Nil).flavor

}
