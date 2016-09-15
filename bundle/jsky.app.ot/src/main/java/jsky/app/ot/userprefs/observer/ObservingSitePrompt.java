package jsky.app.ot.userprefs.observer;

import edu.gemini.spModel.core.Site;

import javax.swing.*;
import java.util.TimeZone;

import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;

/**
 * Prompt the user for an observing site selection.
 */
public final class ObservingSitePrompt {
    private static final String TITLE = "Specify Gemini Site";
    private static final String MESSAGE =
            "Select the site for queuing observations and monitoring for ToO alerts.";
    private static final String[] OPTIONS = new String[] {
            "Use Gemini North", "Use Gemini South"
    };

    private static final TimeZone CAYENNE = TimeZone.getTimeZone("America/Cayenne");

    private static Site guess() {
        final TimeZone tz = TimeZone.getDefault();
        for (Site s : Site.values()) {
            if (s.timezone().hasSameRules(tz)) return s;
        }
        if (CAYENNE.hasSameRules(tz)) return Site.GS;
        return null;
    }

    /**
     * Returns a (possibly null) observing site selection from the user.
     */
    public static Site display() {
        final Site selected = ObserverPreferences.fetch().observingSite();
        final Site initial  = (selected == null) ? guess() : selected;
        final String option = (initial == Site.GS) ? OPTIONS[1] : OPTIONS[0];

        final int choice = JOptionPane.showOptionDialog(
                null, MESSAGE, TITLE, YES_NO_OPTION, QUESTION_MESSAGE,
                null, OPTIONS, option);

        final Site selectedSite;
        switch (choice) {
            case  0: selectedSite = Site.GN; break;
            case  1: selectedSite = Site.GS; break;
            default: selectedSite = null;
        }

        if (selectedSite != null) {
            ObserverPreferences.fetch().withObservingSite(selectedSite).store();
        }

        return selectedSite;
    }
}
