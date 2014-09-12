package edu.gemini.shared.ca;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * A trivial database viewer that loads up a ca_config file and displays its contents and
 * current channel data in a window. Just pass the config file on the commandline.
 * <p>
 * Note that this program doesn't reconnect when a channel dies. Sorry. Use the binding
 * support class if you need it to be more robust.
 * @author rnorris
 */
public class DatabaseViewer extends JFrame implements ConnectionListener, MonitorListener, GetListener {

	private static final long serialVersionUID = 1L;

	static {
		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "172.17.2.255");
		System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");				
	}

	private static final String NO_VALUE = "-";
	private static final int COL_VALUE = 1;
	private static final String[] COLUMNS = { "Channel", "Value", "Description" };
	
	private final List<Channel> channels = new ArrayList<Channel>();

	private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {		
		private static final long serialVersionUID = 1L;		
		public boolean isCellEditable(int arg0,int arg1) {
			return false;
		}
	};

	public DatabaseViewer() {
		getContentPane().add(new JScrollPane(new JTable(model) {{ setSize(800, 600); }}));
		pack();
	}

	public void setDatabase(ChannelAccessDatabase db, Context ctx) throws IllegalStateException, CAException {
		for (Iterator it = db.getStatuses().iterator(); it.hasNext();) {
			ChannelAccessDatabase.Status status = (ChannelAccessDatabase.Status) it.next();
			for (Iterator it2 = status.getEntries().iterator(); it2.hasNext();) {
				ChannelAccessDatabase.Entry e = (ChannelAccessDatabase.Entry) it2.next();
				channels.add(ctx.createChannel(e.getEpicsName(), this));
				model.addRow(new String[] { e.getEpicsName(), NO_VALUE, e.getDescription() });
			}
		}
	}
	
	public static void main(String[] args) throws IOException, CAException, IllegalStateException {
		if (args.length != 1) {
			System.out.println("Usage: " + DatabaseViewer.class.getName() + " path/to/config.ca");
		} else {			
			File file = new File(args[0]);
			ChannelAccessDatabase db = ChannelAccessDatabase.FACTORY.newInstanceFromConfig(file);
			Context ctx = JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);	
			DatabaseViewer monitor = new DatabaseViewer();
			monitor.addWindowListener(new WindowAdapter(){		
				public void windowClosing(WindowEvent we) {
					System.exit(0);
				}		
			});
			monitor.setVisible(true);
			monitor.setDatabase(db, ctx);			
		}
	}

	public void connectionChanged(ConnectionEvent ce) {
		final Channel ch = (Channel) ce.getSource();
		if (ce.isConnected()) {
			try {
				ch.addMonitor(Monitor.VALUE, this);
				ch.getContext().flushIO();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (CAException e) {
				e.printStackTrace();
			}
		} else {
			update(ch, null);
		}
	}

	public void monitorChanged(MonitorEvent me) {
		Channel ch = (Channel) me.getSource();
		try {
			ch.get(this);
			ch.getContext().flushIO();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (CAException e) {
			e.printStackTrace();
		}
	}
	
	private void update(Channel ch, DBR dbr) {
		final int row = channels.indexOf(ch);		
		String value = NO_VALUE;
		if (dbr != null) {
			try {
				Object o = dbr.getValue();
				if (o instanceof String[]) {
					value = ((String[]) o)[0];
				} else if (o instanceof int[]) {
					value = Integer.toString(((int[]) o)[0]);
				} else if (o instanceof double[]) {
					value = Double.toString(((double[]) o)[0]);
				} else {
					value = o.toString();
				}					
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		model.setValueAt(value, row, COL_VALUE);
	}

	public void getCompleted(GetEvent ge) {
		update((Channel) ge.getSource(), ge.getDBR());
	}
	
}


