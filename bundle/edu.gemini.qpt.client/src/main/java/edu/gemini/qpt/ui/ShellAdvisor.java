package edu.gemini.qpt.ui;

import static edu.gemini.qpt.ui.util.BooleanToolPreference.TOOL_MAINTAIN_SPACING;
import static edu.gemini.qpt.ui.util.BooleanToolPreference.TOOL_SNAP;
import static edu.gemini.qpt.ui.util.BooleanViewPreference.*;
import static edu.gemini.ui.workspace.IActionManager.Relation.FirstChildOf;
import static edu.gemini.ui.workspace.IActionManager.Relation.LastChildOf;
import static edu.gemini.ui.workspace.IActionManager.Relation.NextSiblingOf;
import static edu.gemini.ui.workspace.IViewAdvisor.Relation.Above;
import static edu.gemini.ui.workspace.IViewAdvisor.Relation.Beneath;
import static edu.gemini.ui.workspace.IViewAdvisor.Relation.EastOf;
import static edu.gemini.ui.workspace.IViewAdvisor.Relation.NorthOf;
import static edu.gemini.ui.workspace.IViewAdvisor.Relation.SouthOf;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.KeyStroke;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.ui.action.*;
import edu.gemini.qpt.ui.action.nudge.BackAction;
import edu.gemini.qpt.ui.action.nudge.ForwardAction;
import edu.gemini.qpt.ui.action.nudge.ResolutionHigherAction;
import edu.gemini.qpt.ui.action.nudge.ResolutionLowerAction;
import edu.gemini.qpt.ui.find.FindAction;
import edu.gemini.qpt.ui.util.*;
import edu.gemini.qpt.ui.view.candidate.CandidateObsViewAdvisor;
import edu.gemini.qpt.ui.view.comment.CommentViewAdvisor;
import edu.gemini.qpt.ui.view.histo.HistoViewAdvisor;
import edu.gemini.qpt.ui.view.instrument.InstViewAdvisor;
import edu.gemini.qpt.ui.view.lchWindow.LchWindowType;
import edu.gemini.qpt.ui.view.lchWindow.LchWindowViewAdvisor;
import edu.gemini.qpt.ui.view.mask.MaskViewAdvisor;
import edu.gemini.qpt.ui.view.problem.ProblemViewAdvisor;
import edu.gemini.qpt.ui.view.program.ScienceProgramViewAdvisor;
import edu.gemini.qpt.ui.view.property.PropertyViewAdvisor;
import edu.gemini.qpt.ui.view.variant.VariantViewAdvisor;
import edu.gemini.qpt.ui.view.visit.VisitViewAdvisor;
import edu.gemini.qpt.ui.view.visualizer.PlotViewAdvisor;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.ui.workspace.IActionManager;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IShellAdvisor;
import edu.gemini.ui.workspace.IShellContext;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewAdvisor.Relation;
import edu.gemini.ui.workspace.util.RetargetAction;
import edu.gemini.util.security.auth.keychain.KeyChain;

/**
 * The workspace ShellAdvisor for the QPT application. This class assembles the workspace
 * window, which includes the menu bar (and all its actions) and the views.
 *
 * @author rnorris
 */
public class ShellAdvisor implements IShellAdvisor, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(ShellAdvisor.class.getName());

    @SuppressWarnings("unused")
    private final String title;
    private final String rootURL;
    private final String version;
    private final KeyChain authClient;

    private IShellContext context;


    public enum ViewAdvisor {

        Candidates(new CandidateObsViewAdvisor(), Above, null),
        Visualizer(new PlotViewAdvisor(), EastOf, Candidates),
        Visits(new VisitViewAdvisor(), SouthOf, Visualizer),
        Variants(new VariantViewAdvisor(), NorthOf, Candidates),
        Instruments(new InstViewAdvisor(), Beneath, Variants),
        Masks(new MaskViewAdvisor(), Beneath, Instruments),
        Prperties(new PropertyViewAdvisor(), SouthOf, Visits),
        Problems(new ProblemViewAdvisor(), SouthOf, Prperties),
        Comments(new CommentViewAdvisor(), Beneath, Problems),
        Clearance(new LchWindowViewAdvisor(LchWindowType.clearance), Beneath, Prperties),
        Shutter(new LchWindowViewAdvisor(LchWindowType.shutter), Beneath, Prperties),
        Program(new ScienceProgramViewAdvisor(), Beneath, Candidates),
        Histo(new HistoViewAdvisor(), Beneath, Program),;

        final IViewAdvisor advisor;
        final IViewAdvisor.Relation relation;
        final ViewAdvisor relative;

        ViewAdvisor(IViewAdvisor advisor, Relation relation, ViewAdvisor relative) {
            this.advisor = advisor;
            this.relation = relation;
            this.relative = relative;
        }

        void add(IShellContext context) {
            context.addView(advisor, name(), relation, relative == null ? null : relative.name());
        }

    }

    enum Menu {

        File(FirstChildOf, null),
        Edit(NextSiblingOf, File),
        Plan(NextSiblingOf, Edit),
        Candidate(NextSiblingOf, Plan),
        Visit(NextSiblingOf, Candidate),
        Help(LastChildOf, null);
        ;

        final IActionManager.Relation relation;
        final Menu relative;

        Menu(edu.gemini.ui.workspace.IActionManager.Relation relation, Menu relative) {
            this.relation = relation;
            this.relative = relative;
        }

        void add(IActionManager mgr) {
            mgr.addContainer(relation, relative == null ? "" : relative.name(), name(), name());
        }

    }

    private final PublishAction.Destination internal, pachon;
    private final AgsMagnitude.MagnitudeTable magTable;

    public ShellAdvisor(String name, String version, String rootURL, KeyChain authClient, PublishAction.Destination internal, PublishAction.Destination pachon, AgsMagnitude.MagnitudeTable magTable) {
        this.title      = name + " " + version;
        this.rootURL    = rootURL;
        this.version    = version;
        this.authClient = authClient;
        this.internal   = internal;
        this.pachon     = pachon;
        this.magTable   = magTable;
    }

    @SuppressWarnings("serial")
    public void open(final IShellContext context) {

        // Set up
        this.context = context;
        context.setTitle(title);
        final IShell shell = context.getShell();
        shell.addPropertyChangeListener(this);
        IActionManager mgr = context.getActionManager();

        // If we're on the Mac, hook the system Quit menu:
        if (Platform.IS_MAC)
            hookMacQuitMenu();

        // Add Views
        for (ViewAdvisor v : ViewAdvisor.values())
            v.add(context);

        // Add Menus
        addMenuItems(mgr,

                Menu.File,

                new NewAction(shell, authClient, magTable),
                new OpenAction(shell, authClient, magTable),
                new OpenFromWebAction(shell, authClient, magTable),
                null,
                new CloseAction(shell, authClient),
                new SaveAction(shell, authClient),
                new SaveAsAction(shell, authClient),
                null,
                new PublishAction(shell, authClient, internal, pachon),
                null,
                new QuitAction(shell)

        );

        addMenuItems(mgr,

                Menu.Edit,

                new RetargetAction(CommonActions.CUT, "Cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, Platform.MENU_ACTION_MASK)),
                new RetargetAction(CommonActions.COPY, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, Platform.MENU_ACTION_MASK)),
                new RetargetAction(CommonActions.PASTE, "Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, Platform.MENU_ACTION_MASK)),
                null,
                new RetargetAction(CommonActions.DELETE, "Delete", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
                new RetargetAction(CommonActions.SELECT_ALL, "Select All", KeyStroke.getKeyStroke(KeyEvent.VK_A, Platform.MENU_ACTION_MASK)),
                null,
                new ManageKeysAction(shell, authClient)

        );

        addMenuItems(mgr,

                Menu.Plan,

                new AddSemesterAction(shell, authClient, magTable),
                new RemoveSemesterAction(shell, authClient, magTable),
                null,
                new RefreshAction(shell, authClient, magTable),
                new MergeAction(shell, authClient, magTable),
                null,
                new IctdAction(shell, authClient)

        );


        addMenuItems(mgr,

                Menu.Candidate,
                new BooleanPreferenceAction(VIEW_LGS_ONLY, VIEW_ALL, "Hide non-LGS observations in LGS variants"),
                null,
                new BooleanPreferenceAction(VIEW_ALL, null, "Show All", KeyEvent.VK_A, Platform.MENU_ACTION_MASK | KeyEvent.SHIFT_DOWN_MASK),
                null,
                new BooleanPreferenceAction(VIEW_OVER_QUALIFIED_OBSERVATIONS, VIEW_ALL, "Over-Qualified"),
                new BooleanPreferenceAction(VIEW_BLOCKED_OBSERVATIONS, VIEW_ALL, "Blocked"),
                null,
                new BooleanPreferenceAction(VIEW_UNDER_QUALIFIED_OBSERVATIONS, VIEW_ALL, "Under-Qualified"),
                new BooleanPreferenceAction(VIEW_UNAVAILABLE, VIEW_ALL, "Unavailable Inst/Config"),
                new BooleanPreferenceAction(VIEW_MASK_IN_CABINET, VIEW_ALL, "Mask In Cabinet"),
                new BooleanPreferenceAction(VIEW_UNSCHEDULABLE, VIEW_ALL, "Unschedulable"),
                new BooleanPreferenceAction(VIEW_NOT_DARK_ENOUGH, VIEW_ALL, "Not Dark Enough"),
                new BooleanPreferenceAction(VIEW_LOW_IN_SKY, VIEW_ALL, "Low in Sky"),
                null,
                new BooleanPreferenceAction(VIEW_INACTIVE_PROGRAMS, VIEW_ALL, "Inactive Programs"),
                new BooleanPreferenceAction(VIEW_SCIENCE_OBS, VIEW_ALL, "Science Observations"),
                new BooleanPreferenceAction(VIEW_NIGHTTIME_CALIBRATIONS, VIEW_ALL, "Nighttime Calibration Observations"),
                new BooleanPreferenceAction(VIEW_DAYTIME_CALIBRATIONS, VIEW_ALL, "Daytime Calibration Observations"),
                null,
                new BooleanPreferenceAction(VIEW_BAND_1, VIEW_ALL, "Science Band 1", KeyEvent.VK_1),
                new BooleanPreferenceAction(VIEW_BAND_2, VIEW_ALL, "Science Band 2", KeyEvent.VK_2),
                new BooleanPreferenceAction(VIEW_BAND_3, VIEW_ALL, "Science Band 3", KeyEvent.VK_3),
                new BooleanPreferenceAction(VIEW_BAND_4, VIEW_ALL, "Science Band 4", KeyEvent.VK_4),
                null,
                new BooleanPreferenceAction(VIEW_SP_LP, VIEW_ALL,  ProgramType$.MODULE$.LP().name()),
                new BooleanPreferenceAction(VIEW_SP_FT, VIEW_ALL,  ProgramType$.MODULE$.FT().name()),
                new BooleanPreferenceAction(VIEW_SP_Q, VIEW_ALL,   ProgramType$.MODULE$.Q().name()),
                new BooleanPreferenceAction(VIEW_SP_C, VIEW_ALL,   ProgramType$.MODULE$.C().name()),
                new BooleanPreferenceAction(VIEW_SP_SV, VIEW_ALL,  ProgramType$.MODULE$.SV().name()),
                new BooleanPreferenceAction(VIEW_SP_DD, VIEW_ALL,  ProgramType$.MODULE$.DD().name()),
                new BooleanPreferenceAction(VIEW_SP_DS, VIEW_ALL,  ProgramType$.MODULE$.DS().name()),
                new BooleanPreferenceAction(VIEW_SP_ENG, VIEW_ALL, ProgramType$.MODULE$.ENG().name()),
                new BooleanPreferenceAction(VIEW_SP_CAL, VIEW_ALL, ProgramType$.MODULE$.CAL().name()),
                null,
                new BooleanPreferenceAction(SHOW_IN_VISUALIZER, null, "Show in Visualizer", KeyEvent.VK_E),
                new FindAction(shell, authClient)

        );

        addMenuItems(mgr,

                Menu.Visit,

                new BooleanPreferenceAction(TOOL_SNAP, null, "Snap when Dragging", KeyEvent.VK_F2, 0),
                null,
                new ShorterAction(shell),
                new LongerAction(shell),
                null,
                new SplitAction(shell),
                new JoinAction(shell),
                new BooleanPreferenceAction(TOOL_MAINTAIN_SPACING, null, "Maintain Spacing"),
                null,
                new ForwardAction(shell),
                new BackAction(shell),
                new ResolutionLowerAction(shell),
                new ResolutionHigherAction(shell),
                null,
                new DragLimitAction(shell, true),
                new DragLimitAction(shell, false),
                null,
                new ToggleSetupRequiredAction(shell),
                new CompactAction(shell),
                null,
                new EnumBoxAction<>(TimePreference.BOX, TimePreference.LOCAL, "Local Time"),
                new EnumBoxAction<>(TimePreference.BOX, TimePreference.UNIVERSAL, "Universal Time"),
                new EnumBoxAction<>(TimePreference.BOX, TimePreference.SIDEREAL, "Sidereal Time"),
                null,
                new EnumBoxAction<>(ElevationPreference.BOX, ElevationPreference.ELEVATION, "Elevation"),
                new EnumBoxAction<>(ElevationPreference.BOX, ElevationPreference.AIRMASS, "Airmass")


        );

        addMenuItems(mgr,

                Menu.Help,

                new OpenURLAction(rootURL + "/doc/index.html", "QPT Home Page"),
                new OpenURLAction(rootURL + "/doc/manual/index.html", "Reference Manual", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)),
                new OpenURLAction(rootURL + "/doc/troubleshooting.html", "Troubleshooting Guide"),
                new OpenURLAction(rootURL + "/doc/v" + version + "-notes.html", "Release Notes")

//            ,
//            null,
//            updateAction

        );

    }

    @SuppressWarnings("unchecked")
    private void hookMacQuitMenu() {

// This is what we want to do, but in order to make it buildable on Linux
// we need to do it all with introspection.

//        import com.apple.eawt.Application;
//        import com.apple.eawt.ApplicationAdapter;
//        import com.apple.eawt.ApplicationEvent;
//
//        Application app = Application.getApplication();
//        app.removeAboutMenuItem();
//        app.addApplicationListener(new ApplicationAdapter(){
//            public void handleQuit(ApplicationEvent ae) {
//                ae.setHandled(false);
//                context.getShell().close(); // will prompt for save, etc
//            }
//        });

        try {

            final Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            final Class<?> applicationEventClass = Class.forName("com.apple.eawt.ApplicationEvent");
            final Class<?> applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");

            final Object app = applicationClass.getMethod("getApplication").invoke(null);
            applicationClass.getMethod("removeAboutMenuItem").invoke(app);
            applicationClass.getMethod("addApplicationListener", applicationListenerClass).invoke(app,
                    Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{applicationListenerClass},
                            new InvocationHandler() {
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    if (method.getName().equals("handleQuit")) {
                                        applicationEventClass.getMethod("setHandled", Boolean.TYPE).invoke(args[0], false);
                                        context.getShell().close(); // will prompt for save, etc
                                    }
                                    return null; // fortunately all methods on this interface are void
                                }
                            }));

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble installing Quit hook for Mac.", e);
        }
    }

    public boolean close(IShellContext context) {

        // The user has closed the shell, or the framework is shutting down or
        // being updated. In either case we should try to close the model and
        // return true if it works. If we return false (which would happen if the
        // user hits cancel) the window close event will be cancelled.
        final CloseAction close = new CloseAction(context.getShell(), authClient);
        close.actionPerformed(null /* this value is unused in the impl. unsafe, sorry. */);
        return context.getShell().getModel() == null;

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(IShell.PROP_MODEL)) {

            // Set the window title.
            Schedule sched = (Schedule) context.getShell().getModel();
            context.setTitle(sched == null ? title : (sched.getName() + " - " + title));

        }
    }

    static void addMenuItems(IActionManager mgr, Menu menu, Action... actions) {
        menu.add(mgr);
        String path = menu.name();
        IActionManager.Relation rel = FirstChildOf;
        for (Action a : actions) {
            if (a != null) {
                String id = Integer.toString(System.identityHashCode(a));
//                System.out.println("Adding " + id + " as " + rel + " " + path);
                mgr.addAction(rel, path, id, a);
                path = menu.name() + "/" + id;
                rel = NextSiblingOf;
            }
        }
        path = menu.name();
        rel = FirstChildOf;
        for (Action a : actions) {
            if (a != null) {
                String id = Integer.toString(System.identityHashCode(a));
                path = menu.name() + "/" + id;
                rel = NextSiblingOf;
            } else {
//                System.out.println("Adding ----- as " + rel + " " + path);
                mgr.addSeparator(rel, path);
            }
        }
    }

}











