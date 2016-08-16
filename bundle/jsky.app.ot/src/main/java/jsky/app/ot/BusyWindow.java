//
// $Id: BusyWindow.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot;

import jsky.util.gui.Resources;
import jsky.util.gui.ProgressBarUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


//
// There may be something like this in jsky util already.  If so, it won't
// hurt my feelings if this class goes away in favor of that one.  If not,
// maybe it can be moved.  It could be parameterized with the icon to
// display as well as the message.
//
// The main feature is that it allows the user to specify a delay before the
// busy message is displayed.  Should the busy window be canceled before the
// delay expires, it doesn't appear.  That prevents the busy window from
// appearing at all if the operation is short.  We should keep this feature
// whatever we do.  Also, the window that appears is not decorated, which
// is appropriate for startup.
//
// --shane
//


/**
 * A class used to display a "busy please wait message" while performing
 * an operation.
 */
public class BusyWindow {

    private static class Handler implements ActionListener {
        private String _message;
        private JFrame _frame;

        Handler(String message) {
            _message = message;
        }

        public synchronized void actionPerformed(ActionEvent e) {
            _frame = _showBusyIndicator(_message);
        }

        synchronized void stop() {
            if (_frame != null) {
                _frame.hide();
                _frame.dispose();
            }
        }
    }

    private Handler _handler;
    private Timer _timer;

    public synchronized void show(int delay, String message) {
        if (_timer != null) {
            throw new IllegalStateException("Already showing.");
        }
        _handler = new Handler(message);
        _timer = new Timer(delay, _handler);
        _timer.setRepeats(false);
        _timer.start();
    }

    public synchronized void hide() {
        _timer.stop();
        _timer = null;
        _handler.stop();
        _handler = null;
    }

    private static JFrame _showBusyIndicator(String message) {
         JFrame frame = new JFrame();
         frame.setUndecorated(true);
         frame.setResizable(false);

         JPanel contentPane = new JPanel(new GridBagLayout());
         contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

         GridBagConstraints gbc;

         JLabel iconLabel = new JLabel(Resources.getIcon("stopwatch.gif"));
         gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.gridheight = 2;
         gbc.insets = new Insets(0, 0, 0, 5);
         contentPane.add(iconLabel, gbc);

         JLabel lab = new JLabel(message);
         gbc = new GridBagConstraints();
         gbc.gridx = 1;
         gbc.gridy = 0;
         gbc.insets = new Insets(0, 0, 5, 0);
         contentPane.add(lab, gbc);

         ProgressBarUtil progressBarUtil = new ProgressBarUtil();
         gbc = new GridBagConstraints();
         gbc.gridx = 1;
         gbc.gridy = 1;
         gbc.fill  = GridBagConstraints.HORIZONTAL;
         contentPane.add(progressBarUtil, gbc);

         frame.setContentPane(contentPane);
         progressBarUtil.startAnimation();

         frame.pack();

         // Center the window on the screeen.
         Dimension dim = frame.getPreferredSize();
         Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
         frame.setLocation(screen.width / 2 - dim.width / 2,
                           screen.height / 2 - dim.height / 2);

         frame.setVisible(true);
         return frame;
     }
}
