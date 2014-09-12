package edu.gemini.spModel.rich.pot

import edu.gemini.pot.spdb.IDBDatabaseService

package object spdb {
  implicit def odbWrapper(id: IDBDatabaseService) = new RichOdb(id)
}