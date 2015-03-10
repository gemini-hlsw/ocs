package edu.gemini.sp.vcs

import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.DBLocalDatabase

import org.junit.Assert._
import org.junit.Test
import edu.gemini.pot.sp.{SPNodeKey, Conflicts}
import edu.gemini.sp.vcs.OldMergePlan.Zipper
import edu.gemini.spModel.data.ISPDataObject

/**
 *
 */
class TestMergePlan {

  class Env {
    val odb = DBLocalDatabase.createTransient()

    val a = newLeaf("a")
    val b = newLeaf("b").copy(children = List(newLeaf("b.0")))
    val c = newLeaf("c").copy(children = List(newLeaf("c.0"), newLeaf("c.1")))
    val d = newLeaf("d").copy(children = List(newLeaf("d.0"), newLeaf("d.1"), newLeaf("d.2")))
    val root = newLeaf("root").copy(children = List(a, b, c, d))

    val zip = OldMergePlan.Zipper(root)

    def newLeaf(title: String): OldMergePlan = {
      val sp = odb.getFactory.createProgram(new SPNodeKey(), null) // the kind of node is irrelevant here
      val ob = sp.getDataObject.asInstanceOf[ISPDataObject]
      ob.setTitle(title)
      sp.setDataObject(ob)
      OldMergePlan(sp, EmptyNodeVersions, ob, Conflicts.EMPTY, Nil)
    }

    implicit def testZip(zip: OldMergePlan.Zipper) = new Object {
      def isAt(mp: OldMergePlan): Boolean = mp == zip.focus
    }

    def arrivesAt(mp: OldMergePlan, dirs: (Zipper => Option[Zipper])*): Unit = {
      assertTrue(zip.seq(dirs: _*).exists(_.isAt(mp)))
    }

    def arrivesNowhere(dirs: (Zipper => Option[Zipper])*): Unit = {
      assertTrue(zip.seq(dirs: _*).isEmpty)
    }

    def childrenTitles(z: Zipper): List[String] =
      z.focus.children.map(_.sp.getDataObject.asInstanceOf[ISPDataObject].getTitle)

    def testAdd(testBlock: OldMergePlan => (List[String], Zipper)) {
      val zipChildren = List("a", "b", "c", "d")
      assertEquals(zipChildren, childrenTitles(zip))
      val (expected, zipper) = testBlock(newLeaf("x"))
      assertEquals(expected, childrenTitles(zipper))
      assertEquals(zipChildren, childrenTitles(zip))
    }
  }


  private def withEnv(block: Env => Unit): Unit = {
    val env = new Env()
    try {
      block(env)
    } finally {
      env.odb.getDBAdmin.shutdown()
    }
  }

  // root
  // |--a
  // |
  // |--b
  // |  |--b.0
  // |
  // |--c
  // |  |--c.0
  // |  |--c.1
  // |
  // etc.



  @Test def testNavigate() {
    withEnv { env =>
      import env._
      arrivesNowhere(_.up)
      arrivesAt(a, _.down)
      arrivesAt(root, _.down, _.up)
      arrivesNowhere(_.down, _.prev)
      arrivesAt(b, _.down, _.next)
      arrivesAt(c, _.down, _.next, _.next)
      arrivesAt(d, _.down, _.next, _.next, _.next)
      arrivesNowhere(_.down, _.next, _.next, _.next, _.next)
      arrivesAt(a, _.down, _.next, _.prev)
      arrivesAt(b.children.head, _.down, _.next, _.down)
    }
  }

  @Test def testFind() {
    withEnv { env =>
      import env._

      // Find the second child of d
      val d1 = zip.find(_ == d.children(1))

      // Make sure we're really at d1
      assertTrue(d1.exists(_.focus == d.children(1)))
      assertTrue(d1.flatMap(_.up).exists(_.focus == d))
      assertTrue(d1.flatMap(_.seq(_.prev, _.prev)).isEmpty)

      // Find the root itself
      val r = zip.find(_ == root)
      assertTrue(r.exists(_.focus == root))
      assertTrue(r.flatMap(_.up).isEmpty)

      // Find nothing
      assertTrue(zip.find(_.sp == null).isEmpty)
    }
  }

  @Test def testIncr() {
    withEnv { env =>
      import env._

      val id = LifespanId.random

      def versionA(z: Zipper): java.lang.Integer = z.down.get.focus.vv(id)

      // uuid not in map, default to 0
      assertEquals(0, versionA(zip))

      // new zipper with a node version of uuid set to 1
      val zip2 = zip.down.get.incr(id).top
      assertEquals(1, versionA(zip2))

      // nothing in the original zipper has been modified
      assertEquals(0, versionA(zip))
    }
  }

  @Test def testPrependChild() {
    withEnv { env =>
      import env._

      testAdd { x => (List("x", "a", "b", "c", "d"), zip.prependChild(x)) }
    }
  }

  @Test def testAppendChild() {
    withEnv { env =>
      import env._

      testAdd { x => (List("a", "b", "c", "d", "x"), zip.appendChild(x))}
    }
  }

  @Test def testPrepend() {
    withEnv { env =>
      import env._

      assertTrue(zip.prepend(newLeaf("x")).isEmpty)

      testAdd { x => (List("x", "a", "b", "c", "d"), zip.seq(_.down, _.prepend(x), _.up).get) }
      testAdd { x => (List("a", "x", "b", "c", "d"), zip.seq(_.down, _.next, _.prepend(x), _.up).get) }
    }
  }

  @Test def testAppend() {
    withEnv { env =>
      import env._
      assertTrue(zip.append(newLeaf("x")).isEmpty)

      testAdd { x => (List("a", "x", "b", "c", "d"), zip.seq(_.down, _.append(x), _.up).get)}
      testAdd { x => (List("a", "b", "c", "d", "x"), zip.seq(_.down, _.next, _.next, _.next, _.append(x), _.up).get)}
    }
  }

  @Test def testDelete() {
    withEnv { env =>
      import env._
      assertTrue(zip.seq(_.down, _.delete).map(childrenTitles) exists { _ == List("b", "c", "d")})
      assertTrue(zip.seq(_.down, _.next, _.delete).map(childrenTitles) exists { _ == List("a", "c", "d")})
      assertTrue(zip.delete.isEmpty)
    }
  }
}
