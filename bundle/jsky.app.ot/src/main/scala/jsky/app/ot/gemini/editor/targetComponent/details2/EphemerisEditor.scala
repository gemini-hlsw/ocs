package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{Insets, GridBagConstraints, GridBagLayout, Color}
import java.text.SimpleDateFormat
import java.util.{ TimerTask, Timer, Date, TimeZone }
import javax.swing.{SwingConstants, BorderFactory, JPanel, JLabel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.core.{Coordinates, NonSiderealTarget, Target}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.swing.Swing
import scalaz._, Scalaz._

class EphemerisEditor extends TelescopePosEditor[SPTarget] with ReentrancyHack {

  @volatile private[this] var target: Option[NonSiderealTarget] = None

  // TODO: stop the timer when the window closes, or something?
  private val timer = new Timer(true)
  timer.scheduleAtFixedRate(new TimerTask {
    def run(): Unit = {
      val time = System.currentTimeMillis
      val text = target.flatMap(_.coords(time)).fold("--")(formatCoords)
      Swing.onEDT(now.setText(text))
    }
  }, 1000L, 1000L)

  val start, end, size, now, sched, schedCoords  = new JLabel <| { l =>
    l.setForeground(Color.DARK_GRAY)
    l.setHorizontalTextPosition(SwingConstants.LEFT)
  }

  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") <| { df =>
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
  }

  def clear(): Unit =
    List(start, end, size, now, sched, schedCoords).foreach(_.setText("--"))

  def formatDate(ut: Long): String =
    df.format(new Date(ut))

  def formatCoords(cs: Coordinates): String =
    cs.ra.toAngle.formatHMS + " " + cs.dec.formatDMS

  def edit(ctx: GOption[ObsContext], spt: SPTarget, node: ISPNode): Unit = {
    nonreentrant {
      clear()
      target = spt.getNonSiderealTarget

      // Ephemeris bounds and resolution
      for {
        e <- Target.ephemeris.get(spt.getTarget).filterNot(_.isEmpty)
        a <- e.data.findMin.map(_._1)
        b <- e.data.findMax.map(_._1)
      } {
        start.setText(formatDate(a))
        end  .setText(formatDate(b))
        size.setText(e.size.toString)
      }

      // Scheduled time and coordinates
      for {
        t <- target
        c <- ctx.asScalaOpt
        s <- c.getSchedulingBlockStart.asScalaOpt
      } {
        sched.setText(formatDate(s))
        t.coords(s).map(formatCoords).foreach(schedCoords.setText)
      }

    }
  }

  val panel = new JPanel <| { p =>

    p.setLayout(new GridBagLayout)
    p.setBorder(BorderFactory.createCompoundBorder(titleBorder("Ephemeris"),
      BorderFactory.createEmptyBorder(1, 2, 1, 2)))

    p.add(new JLabel("Elements"), new GridBagConstraints <| { c =>
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



