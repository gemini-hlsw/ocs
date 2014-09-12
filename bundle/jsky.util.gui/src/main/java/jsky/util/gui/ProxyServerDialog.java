package jsky.util.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jsky.util.ProxyServerUtil;
import jsky.util.Preferences;

/**
 * Title:        Observing Tool
 * Description:  Dialog for proxy server settings
 * Company:      Gemini 8m Telescopes Project
 *
 * @author Allan Brighton
 * @version 1.0
 */

public class ProxyServerDialog extends JDialog {

    JPanel panel1 = new JPanel();
    JTextArea jTextArea1 = new JTextArea();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    JTextField httpProxyServerField = new JTextField();
    JLabel jLabel2 = new JLabel();
    JTextField httpProxyPortField = new JTextField();
    JTextArea jTextArea2 = new JTextArea();
    JLabel jLabel3 = new JLabel();
    JTextField nonProxyHostsField = new JTextField();
    JPanel jPanel1 = new JPanel();
    JButton cancelButton = new JButton();
    JButton applyButton = new JButton();
    JButton resetButton = new JButton();
    JButton okButton = new JButton();
    JLabel jLabel4 = new JLabel();
    JTextField httpsProxyServerField = new JTextField();
    JLabel jLabel5 = new JLabel();
    JTextField httpsProxyPortField = new JTextField();

    public ProxyServerDialog(Frame frame, String title, boolean modal) {
        super(frame, title, modal);
        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        reset();
        Preferences.manageLocation(this);
    }

    public ProxyServerDialog() {
        this(null, "", false);
    }

    void jbInit() throws Exception {
        panel1.setLayout(gridBagLayout1);
        jTextArea1.setBackground(new Color(204, 204, 204));
        jTextArea1.setEditable(false);
        jTextArea1.setText("If your host is behind a firewall, you may need to use a proxy server " +
                "to access remote catalogs via HTTP. Please enter the hostname and " +
                "port number for the proxy server:");
        jTextArea1.setLineWrap(true);
        jTextArea1.setWrapStyleWord(true);
        jLabel1.setLabelFor(httpProxyServerField);
        jLabel1.setText("HTTP Proxy Server:");
        jLabel2.setLabelFor(httpProxyPortField);
        jLabel2.setText("Port:");
        jTextArea2.setBackground(new Color(204, 204, 204));
        jTextArea2.setEditable(false);
        jTextArea2.setText("The value below can be a list of hosts, each seperated by a |. " +
                "In addition, a wildcard character (*) can be used for matching. For " +
                "example: *.foo.com|localhost :");
        jTextArea2.setLineWrap(true);
        jTextArea2.setWrapStyleWord(true);
        jLabel3.setLabelFor(nonProxyHostsField);
        jLabel3.setText("No Proxy for:");
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        applyButton.setText("Apply");
        applyButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                applyButton_actionPerformed(e);
            }
        });
        resetButton.setText("Reset");
        resetButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                resetButton_actionPerformed(e);
            }
        });
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        this.setTitle("Proxy Server");
        panel1.setMinimumSize(new Dimension(521, 220));
        panel1.setPreferredSize(new Dimension(521, 220));
        jLabel4.setText("HTTPS Proxy Server:");
        jLabel5.setText("Port:");
        httpsProxyServerField.setText("");
        httpsProxyPortField.setText("");
        getContentPane().add(panel1);
        panel1.add(jTextArea1, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        panel1.add(jLabel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        panel1.add(httpProxyServerField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        panel1.add(httpProxyPortField, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        panel1.add(jTextArea2, new GridBagConstraints(0, 3, 4, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        panel1.add(jLabel3, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        panel1.add(nonProxyHostsField, new GridBagConstraints(1, 4, 3, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        panel1.add(jPanel1, new GridBagConstraints(0, 5, 4, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(17, 11, 11, 11), 0, 0));
        jPanel1.add(okButton, null);
        jPanel1.add(resetButton, null);
        jPanel1.add(applyButton, null);
        jPanel1.add(cancelButton, null);
        panel1.add(jLabel4, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        panel1.add(httpsProxyServerField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        panel1.add(jLabel5, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        panel1.add(httpsProxyPortField, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        panel1.add(jLabel2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
    }

    void okButton_actionPerformed(ActionEvent e) {
        if (apply()) {
            close();
        }
    }

    void resetButton_actionPerformed(ActionEvent e) {
        reset();
    }

    void applyButton_actionPerformed(ActionEvent e) {
        apply();
    }

    void cancelButton_actionPerformed(ActionEvent e) {
        close();
    }

    /**
     * Apply changes and return true if okay.
     */
    public boolean apply() {
        String httpProxyHost = httpProxyServerField.getText();
        int httpProxyPort = 80;

        String httpsProxyHost = httpsProxyServerField.getText();
        int httpsProxyPort = 443;

        String s = httpProxyPortField.getText();
        if (s != null && s.length() != 0) {
            try {
                httpProxyPort = Integer.parseInt(httpProxyPortField.getText());
            } catch (Exception e) {
                DialogUtil.error("Please enter a valid HTTP proxy port number.");
                return false;
            }
        }

        s = httpsProxyPortField.getText();
        if (s != null && s.length() != 0) {
            try {
                httpsProxyPort = Integer.parseInt(httpsProxyPortField.getText());
            } catch (Exception e) {
                DialogUtil.error("Please enter a valid HTTPS proxy port number.");
                return false;
            }
        }

        String nonProxyHosts = nonProxyHostsField.getText();
        ProxyServerUtil.setProxy(
                httpProxyHost, httpProxyPort,
                httpsProxyHost, httpsProxyPort,
                nonProxyHosts, nonProxyHosts);

        return true;
    }

    /**
     * Revert to previously saved values.
     */
    public void reset() {
        String httpProxyHost = ProxyServerUtil.getHttpProxyHost();
        if (httpProxyHost == null) {
            httpProxyHost = "";
        }

        int httpProxyPort = ProxyServerUtil.getHttpProxyPort();

        String httpsProxyHost = ProxyServerUtil.getHttpsProxyHost();
        if (httpsProxyHost == null) {
            httpsProxyHost = "";
        }

        int httpsProxyPort = ProxyServerUtil.getHttpsProxyPort();

        String nonProxyHosts = ProxyServerUtil.getHttpNonProxyHosts();
        if (nonProxyHosts == null) {
            nonProxyHosts = "";
        }

        httpProxyServerField.setText(httpProxyHost);
        httpProxyPortField.setText("" + httpProxyPort);
        httpsProxyServerField.setText(httpsProxyHost);
        httpsProxyPortField.setText("" + httpsProxyPort);
        nonProxyHostsField.setText(nonProxyHosts);
    }

    /**
     * Close the window
     */
    public void close() {
        setVisible(false);
    }


    /**
     * Test main.
     */
    public static void main(String[] args) {
        ProxyServerUtil.init();

        new ProxyServerDialog() {

            public void close() {
                System.exit(0);
            }
        }.setVisible(true);
    }
}
