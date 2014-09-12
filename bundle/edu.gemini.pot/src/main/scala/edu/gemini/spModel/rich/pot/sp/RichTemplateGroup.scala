package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp.ISPTemplateGroup
import edu.gemini.spModel.template.TemplateGroup
import edu.gemini.spModel.util.VersionToken

/**
 * Wraps the template group with convenience for getting and setting the
 * version token.
 */
final class RichTemplateGroup(tg: ISPTemplateGroup) {
  def dataObject   = tg.getDataObject.asInstanceOf[TemplateGroup]

  def versionToken = dataObject.getVersionToken

  def versionToken_=(vt: VersionToken): Unit = {
    val dob = dataObject
    if (dob.getVersionToken != vt) {
      dob.setVersionToken(vt)
      tg.setDataObject(dob)
    }
  }
}
