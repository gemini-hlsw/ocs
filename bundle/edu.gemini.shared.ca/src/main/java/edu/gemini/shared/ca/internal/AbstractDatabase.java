package edu.gemini.shared.ca.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.gemini.shared.ca.ChannelAccessDatabase;

/**
 * Base implementation of ChannelAccessDatabase, with implementations of the inner
 * datatypes but no support for construction or [de]serialization.
 * @author rnorris 
 */
public abstract class AbstractDatabase implements ChannelAccessDatabase {

	private final Collection<Channel> channels = new ArrayList<Channel>();
	private final Collection<Command> commands = new ArrayList<Command>();
	private final Collection<Status>  statuses = new ArrayList<Status>();
	private final Collection<Apply>   applies  = new ArrayList<Apply>();
	
	/**
	 * Creates a new channel, which may be passed to add(). 
	 * @param ocsName
	 * @param EpicsName
	 * @param description
	 * @param skipcount
	 * @return a new Channel
	 */
	protected Channel createChannel(final String ocsName, final String EpicsName, final String description, final int skipcount) {
		return new Channel(){
			public String getDescription() {
				return description;
			}
			public String getEpicsName() {
				return EpicsName;
			}
			public String getOcsName() {
				return ocsName;
			}
			public int getSkipCount() {
				return skipcount;
			}		
		};
	}

	/**
	 * Creates a new Command, which may be passed to add().
	 * @param name
	 * @param applyName
	 * @param description
	 * @param entries
	 * @return a new Command
	 */
	protected Command createCommand(final String name, final String applyName, final String description, final Collection<Entry> entries) {
		return new Command() {
			List<Entry> _entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
			public List getEntries() {
				return _entries;
			}
			public String getDescription() {
				return description;
			}
			public String getOcsApplName() {
				return applyName;
			}		
			public String getOcsName() {
				return name;
			}		
		};
	}
	
	/**
	 * Creates a new Status/Command Entry, which may be passed to createCommand()/createStatus()
	 * @param name
	 * @param channel
	 * @param description
	 * @return a new Status/Command Entry
	 */
	protected Entry createEntry(final String name, final String channel, final String description) {
		return new Entry() {
			public String getDescription() {
				return description;
			}
			public String getEpicsName() {
				return channel;
			}
			public String getOcsName() {
				return name;
			}		
		};
	}
	
	/**
	 * Creates a new Status, which may be passed to add().
	 * @param name
	 * @param description
	 * @param entries
	 * @return a new Status
	 */
	protected Status createStatus(final String name, final String description, final Collection<Entry> entries) {
		return new Status() {		
			List<Entry> _entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
			public List getEntries() {
				return _entries;
			}		
			public String getDescription() {
				return description;
			}		
			public String getOcsName() {
				return name;
			}		
		};
	}
	
	/**
	 * Creates a new Apply, which may be passed to add().
	 * @param name
	 * @param channel
	 * @param car
	 * @param desc
	 * @return 
	 */
	protected Apply createApply(final String name, final String channel, final String car, final String desc) {
		return new Apply() {		
			public String getDescription() {
				return desc;
			}		
			public String getEpicsName() {
				return channel;
			}		
			public String getOcsName() {
				return name;
			}		
			public String getApplyCarEpicsName() {
				return car;
			}		
		};
	}
	
	protected void add(Channel channel) {
		// TODO: error if duplicate name
		// TODO: warn if duplicate pointer
		channels.add(channel);
	}

	protected void add(Command command) {
		// TODO: error if duplicate name
		// TODO: error if apply doesn't exist
		commands.add(command);
	}
	
	protected void add(Status status) {
		// TODO: error if duplicate name
		statuses.add(status);
	}

	protected void add(Apply apply) {
		// TODO: error if duplicate name
		applies.add(apply);
	}
	
	public Collection getChannels() {
		return Collections.unmodifiableCollection(channels);
	}

	public Collection getStatuses() {
		return Collections.unmodifiableCollection(statuses);
	}
	
}
