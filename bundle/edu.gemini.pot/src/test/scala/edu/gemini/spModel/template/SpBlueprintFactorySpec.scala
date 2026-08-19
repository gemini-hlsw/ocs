package edu.gemini.spModel.template

import java.io.File
import java.lang.reflect.Modifier

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

/**
 * SpBlueprintFactory maps each blueprint's PARAM_SET_NAME to its ParamSet
 * constructor via a list that has to be maintained by hand, and the compiler
 * cannot check that the list is complete.
 * The test finds the existing bluprints and ensures there is one for each
 * registered type.
 */
object SpBlueprintFactorySpec extends Specification {

  private def concreteBlueprints(root: File): List[Class[_ <: SpBlueprint]] = {

    def classFiles(d: File): List[File] =
      Option(d.listFiles).toList.flatMap(_.toList).flatMap { f =>
        if (f.isDirectory) classFiles(f)
        else if (f.getName.endsWith(".class")) List(f)
        else Nil
      }

    val loader = classOf[SpBlueprint].getClassLoader
    val prefix = root.getAbsolutePath + File.separator

    classFiles(root).flatMap { f: File =>
      val name = f.getAbsolutePath
        .stripPrefix(prefix)
        .stripSuffix(".class")
        .replace(File.separatorChar, '.')

      val found: List[Class[_ <: SpBlueprint]] =
        try {
          val c = Class.forName(name, false, loader)
          if (classOf[SpBlueprint].isAssignableFrom(c) &&
              !Modifier.isAbstract(c.getModifiers) &&
              c != classOf[SpBlueprint] &&
              // Altair is nested inside the blueprints that use it (see
              // SpAltairReaders) rather than being a blueprint map entry of
              // its own, so it is never looked up by name.
              !classOf[SpAltair].isAssignableFrom(c))
            List(c.asSubclass(classOf[SpBlueprint]))
          else Nil
        } catch {
          case _: Throwable => Nil
        }
      found
    }.distinct
  }

  private val blueprints: List[Class[_ <: SpBlueprint]] =
    concreteBlueprints(new File(classOf[SpBlueprint].getProtectionDomain.getCodeSource.getLocation.toURI))

  "SpBlueprintFactory" should {

    "discover the blueprints from the compiled output" in {
      blueprints.size must beGreaterThan(30)
    }

    "have a registry entry for every blueprint" in {
      val registered = SpBlueprintFactory.registeredTypes.asScala
      blueprints.filterNot(registered.contains) must beEmpty
    }
  }
}
