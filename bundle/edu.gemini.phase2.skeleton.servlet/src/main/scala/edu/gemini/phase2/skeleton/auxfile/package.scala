package edu.gemini.phase2.skeleton

import java.io.File

package object auxfile {
  implicit def pimpFile(f: File): FileOps = FileOps(f)
}