package jsky.app.ot.plugin

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.pot.sp.ISPProgramNode
import edu.gemini.spModel.core.Peer
import edu.gemini.util.security.auth.keychain.KeyChain

case class OtContext(node: Option[ISPProgramNode], observingPeer: Option[Peer], keyChain: KeyChain, mt: MagnitudeTable)