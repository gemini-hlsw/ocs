package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.validator.{NodeType, TypeTree}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBLocalDatabase}

import org.junit.{After, Before, Test}
import org.junit.Assert._
import edu.gemini.spModel.core.SPProgramID

class TestMergeValidity {

  val id = SPProgramID.toProgramID("GS-2013A-Q-1")

  var odb: IDBDatabaseService = null
  var fact: ISPFactory = null

  @Before
  def setUp() {
    odb = DBLocalDatabase.createTransient()
    fact = odb.getFactory
  }

  @After
  def tearDown() {
    odb.getDBAdmin.shutdown()
  }


  private def node(n: ISPNode): MergePlan.Node =
    MergePlan.Node(n, EmptyNodeVersions, n.getDataObject, Conflicts.EMPTY)

  private def mp(n: ISPNode, children: MergePlan*): MergePlan =
    MergePlan(node(n), children.toList)

  private def tt(nt: NodeType[_ <: ISPNode], children: TypeTree*): TypeTree =
    TypeTree(nt, None, children.toList)

  private def same(expected: TypeTree, prog: ISPProgram, plan: MergePlan) {
    assertEquals(expected, MergeValidity(prog, odb).process(plan).right.get.toTypeTree.withoutKeys)
  }

  @Test def testMultipleSequences() {
    val prog = fact.createProgram(new SPNodeKey(), id)
    val obs = fact.createObservation(prog, null)
    val oel = fact.createObsExecLog(prog, null)
    val oql = fact.createObsQaLog(prog, null)
    val seq0 = fact.createSeqComponent(prog, ITERATOR_BASE, null)
    val seq1 = fact.createSeqComponent(prog, ITERATOR_BASE, null)

    val plan =
      mp(prog,
        mp(obs,
          mp(oel),
          mp(oql),
          mp(seq0),
          mp(seq1)
        )
      )

    val expected =
      tt(NodeType[ISPProgramNode](PROGRAM_BASIC),
        tt(NodeType[ISPObservation](OBSERVATION_BASIC),
          tt(NodeType[ISPConflictFolder](CONFLICT_FOLDER),
            tt(NodeType[ISPSeqComponent](ITERATOR_BASE))
          ),
          tt(NodeType[ISPObsExecLog](OBS_EXEC_LOG)),
          tt(NodeType[ISPObsQaLog](OBS_QA_LOG)),
          tt(NodeType[ISPSeqComponent](ITERATOR_BASE))
        )
      )

    same(expected, prog, plan)
  }

  // Fix two errors, which requires that the validity checker recurse.
  @Test def testRecursion() {
    val prog = fact.createProgram(new SPNodeKey(), id)
    val obs = fact.createObservation(prog, null)
    val oel = fact.createObsExecLog(prog, null)
    val oql = fact.createObsQaLog(prog, null)
    val ins0 = fact.createObsComponent(prog, INSTRUMENT_GMOSSOUTH, null)
    val ins1 = fact.createObsComponent(prog, INSTRUMENT_FLAMINGOS2, null)
    val seq0 = fact.createSeqComponent(prog, ITERATOR_BASE, null)
    val seq1 = fact.createSeqComponent(prog, ITERATOR_BASE, null)

    val plan =
      mp(prog,
        mp(obs,
          mp(oel),
          mp(oql),
          mp(ins0),
          mp(ins1),
          mp(seq0),
          mp(seq1)
        )
      )

    val expected =
      tt(NodeType[ISPProgramNode](PROGRAM_BASIC),
        tt(NodeType[ISPObservation](OBSERVATION_BASIC),
          tt(NodeType[ISPConflictFolder](CONFLICT_FOLDER),
            tt(NodeType[ISPObsComponent](INSTRUMENT_FLAMINGOS2)),
            tt(NodeType[ISPSeqComponent](ITERATOR_BASE))
          ),
          tt(NodeType[ISPObsExecLog](OBS_EXEC_LOG)),
          tt(NodeType[ISPObsQaLog](OBS_QA_LOG)),
          tt(NodeType[ISPObsComponent](INSTRUMENT_GMOSSOUTH)),
          tt(NodeType[ISPSeqComponent](ITERATOR_BASE))
        )
      )

    same(expected, prog, plan)
  }

  // We turned off this validity check.
  /*
  @Ignore @Test def testUnexpectedFailure() {
    val key  = new SPNodeKey()
    val prog = fact.createProgram(new SPNodeKey(), id)
    val obs0 = fact.createObservation(prog, key)
    val obs1 = fact.createObservation(prog, key)

    val plan =
      mp(prog,
        mp(obs0),
        mp(obs1)
      )

    MergeValidity(prog, odb).process(plan) match {
      case Left(msg) => // okay
      case _ => fail("didn't handle duplicate key issue")
    }
  }

  */
}
