package edu.gemini.spdb.reports.collection.table;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.target.system.HMS;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.util.AbstractTable;


import java.util.*;

public class LGSTargetTable extends AbstractTable {

	private static final long serialVersionUID = 1L;

	private static final Set<ObservationStatus> RELEVANT_OBS_STATUSES = new HashSet<ObservationStatus>();
	static {
		RELEVANT_OBS_STATUSES.add(ObservationStatus.READY);
		RELEVANT_OBS_STATUSES.add(ObservationStatus.ONGOING);
	}

	public static enum Columns implements IColumn {

		ID("ID", "%d"),
		TARGET("Target", "%s"),
		RA_DEG("RA(deg)", "%1.3f"),
		DEC_DEG("DEC(deg)", "%1.3f"),
		RMAG("RMAG", "%s"),
//		UT_START("UT-Start", "%2h:%2m:%2d"),
//		UT_END("UT-End", "%2h:%2m:%2d"),
		RA("RA(h:m:s)", "%s"),
		;

		final String caption;
		final String format;

		Columns(String caption, String format) {
			this.caption = caption;
			this.format = format;
		}

		public String getCaption() {
			return caption;
		}

		public String format(Object value) {
			return String.format(Locale.getDefault(), format, value);
		}

		public Comparator getComparator() {
			return null;
		}

	}

	private int id = 0;

	public LGSTargetTable() {
		super(Domain.OBSERVATION, Columns.values());
	}

	public List<Map<IColumn, Object>> getRows(Object node) {
//		try {

			ISPObservation obsShell = (ISPObservation) node;
			SPObservation obs = (SPObservation) obsShell.getDataObject();

			if (isReadyOrOngoing(obsShell) && isLGS(obsShell)) {
				TargetObsComp targetEnv = getTargetEnv(obsShell);
				if (targetEnv != null) {

					double ra = targetEnv.getTargetEnvironment().getBase().getXaxis();
					double dec = targetEnv.getTargetEnvironment().getBase().getYaxis();
					String targetName = targetEnv.getTargetEnvironment().getBase().getName();

					// TODO: add the progid (compressed) tp the target name

					String brightness = targetEnv.getTargetEnvironment().getBase().getBrightness();

					Map<IColumn, Object> row = new TreeMap<IColumn, Object>();
					row.put(Columns.ID, ++id);
					row.put(Columns.TARGET, targetName);
					row.put(Columns.RA_DEG, ra);
					row.put(Columns.DEC_DEG, dec);
					row.put(Columns.RMAG, brightness);


					row.put(Columns.RA, new HMS(ra));
					return Collections.singletonList(row);

				}
			}

			// This obs is irrevant, so skip it.
			return Collections.emptyList();

//		} catch (RemoteException e) {
//
//			should never happen
//			throw new RuntimeException(e);
//
//		}

	}

	private TargetObsComp getTargetEnv(ISPObservation obsShell)  {
		for (ISPObsComponent obsCompShell: obsShell.getObsComponents()) {
			SPComponentType type = obsCompShell.getType();
			if (type.equals(TargetObsComp.SP_TYPE)) {
				return (TargetObsComp) obsCompShell.getDataObject();
			}
		}
		return null;
	}

	private boolean isReadyOrOngoing(ISPObservation obs) {
		final ObservationStatus obsStatus = ObservationStatus.computeFor(obs);
		return (obsStatus != null && RELEVANT_OBS_STATUSES.contains(obsStatus));
	}

	private boolean isLGS(ISPObservation obsShell)  {
		return true; // LIE!
//		for (ISPObsComponent comp: (List<ISPObsComponent>) obsShell.getObsComponents()) {
//			SPComponentType type = comp.getType();
//			if (type.equals(InstAltair.SP_TYPE)) {
//				InstAltair altair = (InstAltair)  comp.getDataObject();
//				AltairParams.GuideStarType gs = altair.getGuideStarType();
//				return gs == AltairParams.GuideStarType.LGS;
//			}
//		}
//		return false;
	}

}


















