package jsky.app.ot.too;

import jsky.util.gui.Resources;

import java.net.URL;
import java.applet.AudioClip;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Loops an alert sound until dismissed.
 */
public enum TooAudioAlert {
    INSTANCE;

    private static final Logger  LOG = Logger.getLogger(TooAudioAlert.class.getName());
    private static final long PERIOD = 15000;

    private static final String  RES = "sounds/targetOfOpportunity.aiff";

    private static final class PlayClipTask extends TimerTask {
        private AudioClip clip;

        PlayClipTask(AudioClip clip) {
            this.clip = clip;
        }

        public void run() {
            clip.play();
        }
    }

    private Timer timer;
    private AudioClip clip;

    private synchronized AudioClip getClip() {
        if (clip == null) {
            try {
                URL url = Resources.getResource(RES);
                clip = java.applet.Applet.newAudioClip(url);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, ex.getMessage(), ex);
                return null;
            }
        }
        return clip;
    }

    public synchronized void alert() {
        if (timer != null) return; // already alerting

        AudioClip clip = getClip();
        if (clip == null) return;

        timer = new Timer("ToO Audio Timer", true);
        timer.schedule(new PlayClipTask(clip), 0, PERIOD);
    }

    public synchronized void dismiss() {
        if (clip == null) return;
        if (timer == null) return; // not alerting

        timer.cancel();
        timer = null;
    }
}
