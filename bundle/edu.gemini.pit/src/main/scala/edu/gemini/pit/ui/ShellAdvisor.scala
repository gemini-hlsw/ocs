package edu.gemini.pit.ui

import java.net.URL

import edu.gemini.pit.util._
import edu.gemini.pit.ui.CommonActions._
import edu.gemini.pit.ui.view.proposal._
import edu.gemini.pit.ui.view.problem._
import edu.gemini.pit.ui.view.obs._
import edu.gemini.shared.Platform.MENU_ACTION_MASK
import edu.gemini.pit.ui.action._
import edu.gemini.ui.workspace.IViewAdvisor.Relation._
import edu.gemini.ui.workspace.IActionManager.Relation._
import edu.gemini.ui.workspace.scala.{RichShell, RichShellAdvisor, RichShellContext}
import edu.gemini.ui.workspace.util.{InternalFrameHelp, RetargetAction}
import java.awt.event.KeyEvent._
import java.lang.reflect.{InvocationHandler, Method, Proxy}
import java.util.logging.{Level, Logger}
import javax.swing.KeyStroke.getKeyStroke
import javax.swing.JOptionPane.{NO_OPTION, WARNING_MESSAGE, YES_NO_CANCEL_OPTION, YES_OPTION, showConfirmDialog}

import edu.gemini.pit.ui.util._
import edu.gemini.pit.model._
import java.awt.BorderLayout

import view.partner.PartnerView
import view.scheduling.SchedulingView
import view.submit.SubmitView
import edu.gemini.model.p1.immutable._
import java.io.File
import java.awt.event.ActionEvent
import javax.swing.{AbstractAction, JFrame}

import view.tac.TacView
import view.target.TargetView
import java.util.{Locale, Optional}

import edu.gemini.model.p1.submit.SubmitClient
import edu.gemini.pit.ui.binding._
import edu.gemini.pit.ui.robot.{AgsRobot, GsaRobot, ProblemRobot, VisibilityRobot}
import edu.gemini.shared.Platform
import edu.gemini.shared.gui.Browser


// GFace (written for QPT and modeled on Eclipse JFace) is a little awkward to use from Scala, sorry. It depends on
// lifecycle callbacks and has a bit of mutable state. It works and in practice you don't have to mess with it too
// often, but it's annoying.

class ShellAdvisor(
    version: String,     // taken from the PIT bundle metadata, shown in the title bar
    startModel: Model,   // Never null
    file: Option[File],  // We may or may not have a file association
    newShellHandler: ((Model, Option[File]) => Unit), // Call this to open a new shell
    locale: Locale)      // This is used as a hint to select a default TAC partner
  extends RichShellAdvisor[Model] with ActionManagerHelpers {
  private val LOGGER = Logger.getLogger(classOf[ShellAdvisor].getName)

  // The first thing that will happen is the call to open() below, which sets this variable. But until then we can't
  // reference any of the lazy vals below.
  var context: RichShellContext[Model] = null

  // Our shell
  lazy val shell: RichShell[Model] = context.shell

  // Some robots
  lazy val problemHandler = new ProblemRobot(this)

  // Our submit client depends on a system property
  val submitClient: SubmitClient = if (AppMode.isTest) SubmitClient.test else SubmitClient.production
  LOGGER.info("Submit client is " + submitClient)

  // Provide access to the UI for robotic stuff like quick fixes
  // See the bottom of this file for some associated methods.
  lazy val targetView = new TargetView(this)
  lazy val proposalView = new ProposalView(this)
  lazy val investigatorView = proposalView.investigators
  lazy val partnerView = new PartnerView
  lazy val problemView = new ProblemView(this)
  lazy val submitView = new SubmitView(problemHandler, newShellHandler, new SaveAction(shell).applyBoolean, submitClient)
  lazy val obsListView = new ObsListView(this, Band.BAND_1_2)
  lazy val obsListViewB3 = new ObsListView(this, Band.BAND_3)
  lazy val tacView = new TacView(locale)
  lazy val schedulingView = new SchedulingView(this)

  // Status bar
  lazy val status = new StatusBar(this)

  // These actions are actually Bound objects so we need refs to them here. Other actions are just constructed with
  // their menu items (below). Maybe all actions should work like this ... ?
  lazy val targetImportAction = new TargetImportAction(shell, selectView("targets"))
  lazy val targetExportAction = new TargetExportAction(shell)

  // Ok this is the lifecycle method that's called soon after instantiation.
  def open(_context: RichShellContext[Model]): Unit = {
    context = _context

    // Hook the Quit menu if we're on MacOS. This lets us catch Cmd+Q.
    if (Platform.IS_MAC)
      hookMacQuitMenu(context.shell)

    // Tell the shell about our initial model
    val wasRolled = startModel.conversion.transformed

    def updateAttachment(startModel: Model, i: Int): Model = {
      // The attachment may be on the local dir, fix the model
      /* Builds a new proposal changing the attachment */
      def updatedAttachment(attachment: Option[File]):Proposal =
        startModel.proposal.copy(meta = startModel.proposal.meta.copy(attachments = startModel.proposal.meta.attachments.zipWithIndex.map {
          case (a, j) if j == i => a.copy(name = attachment)
          case (a, _)           => a
        }))

      val attachment = startModel.proposal.meta.attachments.lift(i)
      startModel.copy(proposal = updatedAttachment(PDF.relocatedPdf(file, attachment.flatMap(_.name))))
    }

    val fixedModel = updateAttachment(updateAttachment(startModel, 1), 2)

    shell.init(Some(fixedModel), file, wasRolled)

    // HACK: tell each obs list view about the other. We can't do it until now
    obsListView.other = Some(obsListViewB3)
    obsListViewB3.other = Some(obsListView)

    // We have some bound objects with no UI that we bind thus: // TODO: improve
    def addRoot(boundElement: Bound[Model, _], undoable: Boolean = true): Unit = {
      BoundView.bindToShell(shell, boundElement, undoable) {
        m => ()
      }
    }

    // Configure some of the robots
    AgsRobot.setAutoRefresh(300000)
    GsaRobot.setAutoRefresh(300000)

    // Add root listeners. Note that the robots don't affect undo status when they manipulate the model.
    addRoot(targetImportAction)
    addRoot(targetExportAction)
    addRoot(AgsRobot, undoable = false)
    addRoot(VisibilityRobot, undoable = false)
    addRoot(GsaRobot, undoable = false)
    addRoot(problemHandler, undoable = false)

    // Update the title bar if the mode or model change
    shell.listen(updateTitle())
    AppPreferences.addListener(_ => updateTitle())

    // Set the frame icon, useful for Windows and Linux
    if (!Platform.IS_MAC) {
      shell.peer.setIconImage(SharedIcons.PIT.getImage)
    }

    // Title and modified handler. The window title has the filename without extension (or "Untitled") and the PIT
    // version. If the document has been modified, it has the standard modification decoration on the Mac (dot in the
    // window close button) and "[*]" after the filename on other platforms.
    def updateTitle(): Unit = {
      val name = shell.file.map {
        f =>
          val n = f.getName
          val i = n.lastIndexOf('.')
          if (i > 0) n.subSequence(0, i) else n
      } getOrElse "Untitled"

      val mod = if (shell.isModified && !Platform.IS_MAC) "[*]" else ""
      val tac = if (AppPreferences.current.mode == AppPreferences.PITMode.TAC) " - TAC" else ""
      val test = if (AppMode.isTest) " - TEST" else ""
      val versionRegex = """(\d\d\d\d)\.(\d*)\.(\d*)""".r
      val minorVersion = version match {
        case versionRegex(_, s, m) => s"$s.$m"
        case x                        => x
      }
      context.title = s"$name$mod - Gemini PIT ${Semester.current.year}${Semester.current.half}.$minorVersion$tac$test"
      if (Platform.IS_MAC)
        shell.peer.getRootPane.putClientProperty("Window.documentModified", shell.isModified)
    }

    def helpAction(s: String): Optional[InternalFrameHelp] =
      Optional.of(InternalFrameHelp(new AbstractAction() {
            override def actionPerformed(e: ActionEvent): Unit = Browser.open(new URL(s))
          }, SharedIcons.ICON_HELP, "Open the help page for this tab in web browser."))


    // Add our views. Note that the ordering is significant; the underlying GFace code isn't very good so it can take
    // some fiddling around to make things look the way you want.
    context.context.addView(new BoundView.Advisor("Problems", problemView), "problems", null, null)
    context.context.addView(new BoundView.Advisor("Overview", proposalView), "proposal", NorthOf, "problems", helpAction("http://www.gemini.edu/node/11761"))
    context.context.addView(new BoundView.Advisor("Band 3", obsListViewB3), "band3", EastOf, "proposal", helpAction("http://www.gemini.edu/node/11774"))
    context.context.addView(new BoundView.Advisor("Time Requests", partnerView), "partners", Beneath, "proposal", helpAction("http://www.gemini.edu/node/11762"))
    context.context.addView(new BoundView.Advisor("Scheduling", schedulingView), "scheduling", Beneath, "proposal", helpAction("http://www.gemini.edu/node/11884"))
    context.context.addView(new BoundView.Advisor("Submit", submitView), "submit", Beneath, "partners", helpAction("http://www.gemini.edu/node/11765"))
    context.context.addView(new BoundView.Advisor("Observations", obsListView), "obs", Above, "band3", helpAction("http://www.gemini.edu/node/11763"))
    context.context.addView(new BoundView.Advisor("Targets", targetView), "targets", Above, "obs", helpAction("http://www.gemini.edu/node/11764"))
    context.context.addView(new BoundView.Advisor("TAC", tacView), "tac", Beneath, "partners")

    // Add our status bar, which requires some secret knowledge
    shell.peer.getContentPane.add(status.peer, BorderLayout.SOUTH)

    // File Menu
    val fileMenu = Menu("File", FirstChildOf, None)
    context.actionManager.add(fileMenu,
      Some(new NewAction(shell, newShellHandler)),
      Some(new OpenAction(shell, newShellHandler)),
      None,
      Some(new CloseAction(shell)),
      None,
      Some(new SaveAction(shell)),
      Some(new SaveAsAction(shell)),
      Some(new PdfAction(shell)),
      None,
      Some(targetImportAction.peer),
      Some(targetExportAction.peer),
      None,
      Some(new AppPreferencesAction(shell)),
      None,
      Some(new QuitAction(shell)))

    // TODO: an open recent menu?

    // Edit Menu
    val editMenu = Menu("Edit", NextSiblingOf, Some(fileMenu))
    context.actionManager.add(editMenu,
      Some(new UndoAction(shell)),
      Some(new RedoAction(shell)),
      None,
      Some(new RetargetAction(Cut, "Cut", getKeyStroke(VK_X, MENU_ACTION_MASK))),
      Some(new RetargetAction(Copy, "Copy", getKeyStroke(VK_C, MENU_ACTION_MASK))),
      Some(new RetargetAction(Paste, "Paste", getKeyStroke(VK_V, MENU_ACTION_MASK))),
      None,
      Some(new RetargetAction(Delete, "Delete", getKeyStroke(VK_BACK_SPACE, 0))),
      Some(new RetargetAction(SelectAll, "Select All", getKeyStroke(VK_A, MENU_ACTION_MASK))))

    // View Menu
    val viewMenu = Menu("View", NextSiblingOf, Some(editMenu))
    context.actionManager.add(viewMenu,
      Some(new EnumBoxAction(DegreePreference.BOX, DegreePreference.DEGREES, "Degrees")),
      Some(new EnumBoxAction(DegreePreference.BOX, DegreePreference.HMSDMS, "HMS/DMS")))

    // Catalog Menu
    val catalogMenu = Menu("Catalog", NextSiblingOf, Some(viewMenu))
    context.actionManager.add(catalogMenu,
      Some(new BooleanPreferenceAction(BooleanToolPreference.SIMBAD, null, "SIMBAD")),
      Some(new BooleanPreferenceAction(BooleanToolPreference.NED, null, "NED")),
      Some(new BooleanPreferenceAction(BooleanToolPreference.HORIZONS, null, "HORIZONS")))

    // Help Menu
    val helpMenu = Menu("Help", NextSiblingOf, Some(catalogMenu))


    context.actionManager.add(helpMenu,
      Some(new BrowseAction("http://www.gemini.edu/node/11760", "Phase I Tool Help")),
      Some(new BrowseAction(URLConstants.GET_TEMPLATES._1, URLConstants.GET_TEMPLATES._2)),
      Some(new BrowseAction(URLConstants.OPEN_ITC._1, URLConstants.OPEN_ITC._2))
    )

    // Debug Menu -- for developers only
    if (AppMode.isTest) {
        val debugMenu = Menu("Debug", NextSiblingOf, Some(helpMenu))
        context.actionManager.add(debugMenu, Some(new ValidateAction(shell)))
    }

    // Hack the key bindings for text widgets
    registerKeyBindings()

  }

  private def registerKeyBindings(): Unit = {
    import java.awt.event.KeyEvent._
    import java.awt.event.InputEvent._
    import javax.swing.{AbstractAction, Action, KeyStroke}
    import javax.swing.text._
    import javax.swing.text.DefaultEditorKit._

    val kit = new DefaultEditorKit
    val action = kit.getActions.map(a => a.getValue(Action.NAME) -> a).toMap

    // Add some emacs-style bindings
    val bindings = List(
      (VK_B, CTRL_DOWN_MASK, backwardAction),
      (VK_F, CTRL_DOWN_MASK, forwardAction),
      (VK_A, CTRL_DOWN_MASK, beginLineAction),
      (VK_E, CTRL_DOWN_MASK, endLineAction),
      (VK_P, CTRL_DOWN_MASK, upAction),
      (VK_N, CTRL_DOWN_MASK, downAction),
      (VK_D, CTRL_DOWN_MASK, deleteNextCharAction),
      (VK_E, CTRL_DOWN_MASK | SHIFT_DOWN_MASK, selectionEndLineAction),
      (VK_A, CTRL_DOWN_MASK | SHIFT_DOWN_MASK, selectionBeginAction)
    )

    val km = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP)
    bindings foreach {
      case (key, mod, act) => km.addActionForKeyStroke(KeyStroke.getKeyStroke(key, mod), action(act))
    }

    // No built in action for this?
    val cutToEnd = new AbstractAction() {
      def actionPerformed(e: ActionEvent): Unit = {
        action(selectionEndLineAction).actionPerformed(e)
        action(cutAction).actionPerformed(e)
      }
    }
    km.addActionForKeyStroke(KeyStroke.getKeyStroke(VK_K, CTRL_DOWN_MASK), cutToEnd)
  }

  // This is called when the user closes the window or selects Quit. If there's a model, we want to
  // invoke the Close method.
  def close(context: RichShellContext[Model]): Boolean = {

    def confirmClose(frame: JFrame) = showConfirmDialog(frame,
      "There are unsaved changes.\nDo you wish to save first?",
      "Confirm Close", YES_NO_CANCEL_OPTION, WARNING_MESSAGE)

    val shell = context.shell
    shell.isClean || (confirmClose(shell.peer) match {
      case YES_OPTION => new SaveAction(shell)();shell.isClean
      case NO_OPTION  => shell.checkpoint();true
      case _          => false
    })

  }

  private def hookMacQuitMenu(shell: RichShell[_]): Unit = {

    // This is what we want to do, but in order to make it buildable on Linux
    // we need to do it all with introspection.

    //		import com.apple.eawt.Application;
    //		import com.apple.eawt.ApplicationAdapter;
    //		import com.apple.eawt.ApplicationEvent;
    //
    //		Application app = Application.getApplication();
    //		app.removeAboutMenuItem();
    //		app.addApplicationListener(new ApplicationAdapter(){
    //			public void handleQuit(ApplicationEvent ae) {
    //				ae.setHandled(false);
    //				context.getShell().close(); // will prompt for save, etc
    //			}
    //		});

    try {
      val applicationClass = Class.forName("com.apple.eawt.Application")
      val applicationEventClass = Class.forName("com.apple.eawt.ApplicationEvent")
      val applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener")
      val app = applicationClass.getMethod("getApplication").invoke(null)
      applicationClass.getMethod("removeAboutMenuItem").invoke(app)
      applicationClass.getMethod("addApplicationListener", applicationListenerClass).invoke(app,
        Proxy.newProxyInstance(getClass.getClassLoader, Array(applicationListenerClass),
          new InvocationHandler() {
            def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = {
              if (method.getName.equals("handleQuit")) {
                applicationEventClass.getMethod("setHandled", java.lang.Boolean.TYPE)
                  .invoke(args(0), false.asInstanceOf[AnyRef])
                shell.close() // will prompt for save, etc
              }
              null // fortunately all methods on this interface are void
            }
          }))
      LOGGER.log(Level.INFO, "Hooked Quit action for Mac.")
    } catch {
      case e:Exception if System.getProperty("java.version").startsWith("1.8") => LOGGER.log(Level.WARNING, "Trouble installing Quit hook for Mac.", e)
    }
  }


  // These are some methods that make it a bit easier to do things robotically (like quick fixes).

  def inOverview(body: ProposalView => Unit): Unit = {
    withView("proposal", proposalView, body)
  }

  def inObsListView(b: Band, body: ObsListView => Unit): Unit = {
    b match {
      case Band.BAND_1_2 => withView("obs", obsListView, body)
      case Band.BAND_3   => withView("band3", obsListViewB3, body)
    }
  }

  def inPartnersView(body: PartnerView => Unit): Unit = {
    withView("partners", partnerView, body)
  }

  def inTargetsView(body: TargetView => Unit): Unit = {
    withView("targets", targetView, body)
  }

  private def withView[V](id: String, view: V, body: V => Unit): Unit = {
    selectView(id)
    body(view)
  }

  def showPartnersView(): Unit = {
    selectView("partners")
  }

  def showObsListView(): Unit = {
    selectView("obs")
  }

  def showObsListView(b: Band): Unit = {
    b match {
      case Band.BAND_1_2 => selectView("obs")
      case Band.BAND_3   => selectView("band3")
    }
  }

  def showTacView(partner: Any): Unit = {
    tacView.form.partner.selection.item = partner
    selectView("tac")
  }

  private def selectView(n: String): Unit = {
    context.context.getShell.selectView(n)
  }

}
