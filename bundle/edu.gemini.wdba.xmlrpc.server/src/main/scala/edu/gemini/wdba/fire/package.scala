// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba

import edu.gemini.spModel.core.catchingNonFatal
import edu.gemini.wdba.fire.FireFailure.FireException

import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import scalaz._
import scalaz.Scalaz._
import scalaz.concurrent.Task

package object fire {

  type FireAction[A] = EitherT[Task, FireFailure, A]

  object FireAction {

    def apply[A](a: => A): FireAction[A] =
      EitherT(Task.delay(a.right))

    val unit: FireAction[Unit] =
      apply(())

    def fail[A](f: => FireFailure): FireAction[A] =
      EitherT(Task.delay(f.left[A]))

    def catching[A](a: => A): FireAction[A] =
      EitherT(Task.delay(catchingNonFatal(a).leftMap(t => FireException(t): FireFailure)))

  }
}
