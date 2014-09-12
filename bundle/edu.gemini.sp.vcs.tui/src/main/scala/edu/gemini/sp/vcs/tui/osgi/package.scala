package edu.gemini.sp.vcs.tui

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.util.security.auth.keychain.KeyChain

import org.osgi.util.tracker.ServiceTracker

/**
 *
 */
package object osgi {
  type OdbTracker = ServiceTracker[IDBDatabaseService, IDBDatabaseService]
  type RegTracker = ServiceTracker[VcsRegistrar, VcsRegistrar]
  type AuthTracker = ServiceTracker[KeyChain, KeyChain]
}
