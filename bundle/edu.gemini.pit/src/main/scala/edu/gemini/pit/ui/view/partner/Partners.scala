package edu.gemini.pit.ui.view.partner

import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util.SharedIcons
import java.awt.{Component, Graphics}
import javax.swing.Icon

object PartnersFlags {
  val flag = Map[Any, Icon](
    NgoPartner.AR          -> SharedIcons.FLAG_AR,
    NgoPartner.AU          -> SharedIcons.FLAG_AU,
    NgoPartner.BR          -> SharedIcons.FLAG_BR,
    NgoPartner.CA          -> SharedIcons.FLAG_CA,
    NgoPartner.CL          -> SharedIcons.FLAG_CL,
    NgoPartner.KR          -> SharedIcons.FLAG_KR,
    NgoPartner.US          -> SharedIcons.FLAG_US,
    NgoPartner.UH          -> SharedIcons.FLAG_UH,
    ExchangePartner.CFH    -> SharedIcons.FLAG_CFH,
    ExchangePartner.KECK   -> SharedIcons.FLAG_KECK,
    ExchangePartner.SUBARU -> SharedIcons.FLAG_JP,
    LargeProgramPartner    -> SharedIcons.FLAG_GEMINI
  ).map {
    case (p, i) => (p,
      new Icon {
        // Scooch the icon over a couple px
        def getIconWidth = i.getIconWidth + 2
        def getIconHeight = i.getIconHeight
        def paintIcon(c:Component, g:Graphics, x:Int, y:Int) {
          i.paintIcon(c, g, x + 2, y)
        }
      })
  }

}

