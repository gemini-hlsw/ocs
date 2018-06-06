package edu.gemini.auxfile.server.file;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.gemini.auxfile.api.AuxFileException;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlException;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;

public class MetaData {

	private static final Logger LOGGER = Logger.getLogger(MetaData.class.getName());

    private static final String PROP_ROOT         = "meta";
    private static final String PROP_DESCRIPTION  = "description";
	private static final String PROP_CHECKED      = "checked";
	private static final String PROP_LAST_EMAILED = "lastEmailed";

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

	private final File xml;
	private String description;
	private boolean checked;
	private Option<Instant> lastEmailed;

	@SuppressWarnings("deprecation")
	public static MetaData forFile(SPProgramID progId, String fileName) throws IOException, AuxFileException {

		// Make sure the dirs exist.
		FileManager.instance().initProgramDir(progId);

		File xml = FileManager.instance().getMetaFile(progId, fileName);
		MetaData md = new MetaData(xml);

		// If this is a new MetaData for this auxfile, copy the legacy description if
		// there is one, and delete the legacy description dir/file. Do this here since
		// we don't pass the progId/fileName info to the ctor.
		if (!xml.exists()) {

			// Previous versions had the description in its own file.
			final String DESC_DIR    = "desc";
			final String DESC_SUFFIX = ".desc";

			final FileManager r = FileManager.instance();
			final File ddir = new File(r.getProgramDir(progId), DESC_DIR);
			final File dfile = new File(ddir, fileName + DESC_SUFFIX);

			if (dfile.exists()) {
				LOGGER.info("Migrating legacy description file for " + fileName + " (" + progId + ")");
				md.setDescription(FileUtil.readString(dfile));
				dfile.delete(); // do this last in case the md update fails
			}

		}

		return md;
	}

	private MetaData(File xml) throws IOException {
		this.xml = xml;
		read();
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) throws IOException {
		this.checked = checked;
		store();
	}

	public Option<Instant> getLastEmailed() {
		return lastEmailed;
	}

	public void setLastEmailed(Option<Instant> lastEmailed) throws IOException {
		this.lastEmailed = lastEmailed;
		store();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) throws IOException {
		this.description = description;
		store();
	}

	private void store() throws IOException {

		// Create the PIO XML document.
		PioFactory factory = new PioXmlFactory();
		ParamSet node = factory.createParamSet(PROP_ROOT);

		// Set the metadata properties
		Pio.addBooleanParam(factory, node, PROP_CHECKED, checked);
		Pio.addParam(factory, node, PROP_DESCRIPTION, description);
		lastEmailed.foreach(t -> Pio.addParam(factory, node, PROP_LAST_EMAILED, TIME_FORMAT.format(t)));

		// And write out the file.
		try {
			PioXmlUtil.write(node, xml);
		} catch (PioXmlException pxe) {
			LOGGER.log(Level.SEVERE, "Trouble storing meta", pxe);
			IOException ioe = new IOException(pxe.getMessage());
			ioe.initCause(pxe);
			throw ioe;
		}
	}

	private void read() throws IOException {
		if (!xml.exists()) return;
		try {

			// Read the PIO XML document.
			ParamSet node = (ParamSet) PioXmlUtil.read(xml);

			// Get the metadata properties
			checked = Pio.getBooleanValue(node, PROP_CHECKED, false);
			description = Pio.getValue(node, PROP_DESCRIPTION, null);
			lastEmailed = ImOption.apply(Pio.getValue(node, PROP_LAST_EMAILED, null)).map(s ->
	            TIME_FORMAT.parse(s, Instant::from)
			);

		} catch (PioXmlException pxe) {
			LOGGER.log(Level.SEVERE, "Trouble reading meta", pxe);
			IOException ioe = new IOException(pxe.getMessage());
			ioe.initCause(pxe);
			throw ioe;

		} catch (DateTimeParseException dtpe) {
			LOGGER.log(Level.SEVERE, "Trouble reading meta", dtpe);
			IOException ioe = new IOException("Couldn't parse timestamp");
			ioe.initCause(dtpe);
			throw ioe;

		}
	}

}
