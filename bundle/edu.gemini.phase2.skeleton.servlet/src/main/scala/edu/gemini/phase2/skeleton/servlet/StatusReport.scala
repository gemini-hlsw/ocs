package edu.gemini.phase2.skeleton.servlet

import edu.gemini.phase2.core.model.SkeletonStatus

import xml.Elem
import edu.gemini.spModel.core.SPProgramID

case class StatusReport(id: SPProgramID, status: SkeletonStatus) {
  def toXML: Elem =
    <skeleton>
      <id>
        {id.toString}
      </id>
      <status>
        {status.display}
      </status>
    </skeleton>
}
