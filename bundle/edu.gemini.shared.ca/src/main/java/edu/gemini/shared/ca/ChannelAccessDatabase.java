package edu.gemini.shared.ca;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import edu.gemini.shared.ca.internal.ConfigFileDatabase;

/**
 * Interfaces and factory for a channel-access database, based on the C++ libraries in ocswish/libChannels.
 * The API remains minimal at this point because it is not being used yet; at this point you can simply
 * instantiate a database from a config file and examine its contents.
 * <p>
 * At this point there exists a single factory method, for producing a ChannelAccessDatabase from a
 * libChannels-style ca_config file such as those in the seqexec source. 
 * @author rnorris
 */
public interface ChannelAccessDatabase {

	/**
	 * Static factory class, with methods for instantiating a ChannelAccessDatabase.  
	 */
	class FACTORY {
		public static ChannelAccessDatabase newInstanceFromConfig(File file) throws IOException {
			return new ConfigFileDatabase(file);
		}
	}
	
	interface Entry {
		
		/** Name of the channel in OCS **/
		String getOcsName();
		
		/** Name of the channel in EPICS **/
		String getEpicsName();
		
		/** User-friendly description. **/
		String getDescription();
		
	}
	
	interface Channel extends Entry {

		/** TODO: what is this? **/
		int getSkipCount();

	}
	
	interface Apply extends Entry {
		
		/** Apply CAR EPICS name **/
		String getApplyCarEpicsName();
		
	}
	
	interface Status {

		/** Status name **/
		String getOcsName();
		
		/** Description **/
		String getDescription();

		/** Entries **/
		List getEntries();
		
	}

	interface Command {
		
		/** Status name **/
		String getOcsName();
		
		/** OCS Apply record name **/
		String getOcsApplName();
		
		/** Description **/
		String getDescription();

		/** Entries **/
		List getEntries();

	}	
	
	Collection getChannels();
	Collection getStatuses();
	
}
