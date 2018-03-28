import edu.gemini.pot.sp.validator.Validator
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor, DBLocalDatabase}
import edu.gemini.pot.sp.{Instrument, SPNodeKey, ISPProgram, ISPNode}
import edu.gemini.spModel.io.SpImportService
import edu.gemini.util.security.principal.StaffPrincipal
import java.io.{StringReader, FilenameFilter, File}
import collection.JavaConverters._
import edu.gemini.pot.sp.SPComponentType._
import java.security.Principal
import org.junit.Ignore
import scala.io.Source

@Ignore
object CardinalityValidatorTest extends App {

  // Test with an in-memory database
  val db = DBLocalDatabase.createTransient

  try {

    val fact = db.getFactory

    def check(p:ISPProgram) {
      val result = Validator.validate(p)
      println("\n\nRESULT for " + p.getProgramID + ":\n")
      result match {
        case Left(v) => println(v); sys.exit(-1)
        case Right(_) => println("[ok]")
      }
    }

    // Test out some unusual cases
    {
      val p = fact.createProgram(new SPNodeKey(), null)

      val o = fact.createObservation(p, Instrument.Nifs.some, null)
      p.addObservation(o)

      val e = fact.createObsComponent(p, ENG_ENGNIFS, null)
      o.addObsComponent(e)

      check(p)
    }

    {
      val p = fact.createProgram(new SPNodeKey(), null)

      val o = fact.createObservation(p, Instrument.Nifs.some, null)
      p.addObservation(o)

      val e = fact.createObsComponent(p, ENG_ENGNIFS, null)
      o.addObsComponent(e)

      val c = fact.createConflictFolder(p, null)
      o.setConflictFolder(c)


      val i1 = fact.createObsComponent(p, INSTRUMENT_NIRI, null)
      val i2 = fact.createObsComponent(p, INSTRUMENT_GMOS, null)
      c.setChildren(List[ISPNode](i1, i2).asJava)


      check(p)
    }



    // Load up some test programs
    val importer = new SpImportService(db)
    val filter = new FilenameFilter {
      def accept(dir: File, name: String): Boolean = name.matches(""".*-(\d+)\.xml$""")
    }
    val fs  = new File("/Users/rnorris/Downloads/2012B/").listFiles(filter).toList
    fs.foreach { f => importer.importProgramXml(new StringReader(Source.fromFile(f).mkString)) }

    // Validate each
    val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)
    db.getQueryRunner(user).queryPrograms(new DBAbstractQueryFunctor {
      def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]) {
        check(node.asInstanceOf[ISPProgram])
      }
    })

  } catch {
    case e:Exception => e.printStackTrace()
  } finally {
    db.getDBAdmin.shutdown()
    sys.exit()
  }
}
