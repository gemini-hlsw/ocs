package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{Insets, GridBagConstraints, GridBagLayout, Color}
import java.text.SimpleDateFormat
import java.util.{ TimerTask, Timer, Date, TimeZone }
import javax.swing.{SwingConstants, BorderFactory, JPanel, JLabel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.core.{Ephemeris, NonSiderealTarget, Target}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import edu.gemini.shared.util.immutable.ScalaConverters._

import scalaz._, Scalaz._

class EphemerisEditor extends TelescopePosEditor with ReentrancyHack {

  private[this] var target: Option[NonSiderealTarget] = None

  // TODO: stop the timer when the window closes, or something?
  private val timer = new Timer(true)
  timer.scheduleAtFixedRate(new TimerTask {
    def run(): Unit =
      target.flatMap(_.coords(System.currentTimeMillis)) match {
        case Some(cs) => now.setText(cs.ra.toAngle.formatHMS + " " + cs.dec.formatDMS)
        case None     => now.setText("--")
      }
    }, 1000L, 50L) // unjustifiably fast but it looks cool

  val start, end, size, now, sched, schedCoords  = new JLabel <| { l =>
    l.setForeground(Color.DARK_GRAY)
    l.setMinimumSize(l.getMinimumSize <| (_.width = 200))
    l.setHorizontalTextPosition(SwingConstants.LEFT)
  }

  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") <| { df =>
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
  }

  def clear(): Unit =
    List(start, end, size, now, sched, schedCoords).foreach(_.setText("--"))

  def formatDate(ut: Long): String =
    df.format(new Date(ut))

  def edit(ctx: GOption[ObsContext], spt: SPTarget, node: ISPNode): Unit = {
    nonreentrant {
      clear()
      target = spt.getNonSiderealTarget
      Target.ephemeris.get(spt.getTarget).filterNot(_.isEmpty).foreach { e =>
        for {
          a <- e.findMin.map(_._1)
          b <- e.findMax.map(_._1)
        } {
          start.setText(formatDate(a))
          end  .setText(formatDate(b))
          val res = (b - a) / e.size
          e.findMin.map(_._1).map(formatDate).foreach(start.setText)
          e.findMax.map(_._1).map(formatDate).foreach(end.setText)
          size.setText(res / (1000 * 60 * 60) + " minutes")
        }
      }
      ctx.asScalaOpt.flatMap(_.getSchedulingBlockStart.asScalaOpt).foreach { t =>
        sched.setText(formatDate(t))
        target.flatMap(_.coords(t)).foreach { cs =>
          schedCoords.setText(cs.ra.toAngle.formatHMS + " " + cs.dec.formatDMS)
        }
      }
    }
  }

  val panel = new JPanel <| { p =>

    p.setLayout(new GridBagLayout)
    p.setBorder(BorderFactory.createCompoundBorder(titleBorder("Ephemeris"),
      BorderFactory.createEmptyBorder(1, 2, 1, 2)))

    p.add(new JLabel("Resolution"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 0
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(size, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 0
      c.weightx = 10
      c.insets = new Insets(2, 1, 0, 0)
    })

    p.add(new JLabel("Start"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 1
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(start, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 1
      c.fill   = GridBagConstraints.HORIZONTAL
      c.weightx = 10
      c.insets = new Insets(2, 1, 0, 0)
    })

    p.add(new JLabel("End"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 2
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(end, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 2
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 1, 0, 0)
    })

    p.add(new JLabel("Scheduled"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 3
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(sched, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 3
      c.insets = new Insets(2, 1, 0, 0)
    })

    p.add(schedCoords, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 4
      c.insets = new Insets(2, 1, 0, 0)
    })

    p.add(new JLabel("Current"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 5
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(now, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 5
      c.insets = new Insets(2, 1, 0, 0)
    })

  }

}



