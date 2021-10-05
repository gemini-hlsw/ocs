package jsky.app.ot.gemini.obscat;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.dataset.DataflowStatus;
import edu.gemini.spModel.dataset.DataflowStatus$;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsQaState;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.ObsoletableSpType;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.shared.gemini.obscat.ObsCatalogInfo;
import jsky.app.ot.viewer.QueryManager;
import jsky.app.ot.viewer.ViewerService;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.util.NameValue;
import jsky.util.gui.DialogUtil;
import scala.Option$;

import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * A class for querying the science program database for observations
 * matching a given set of constraints. This class treats the science
 * program database as a catalog that can be queried in the usual
 * way. It extends the SkycatCatalog class so that it fits in with the
 * existing catalogs and so that the base positions of the
 * observations found can be easily plotted on an image.
 *
 * @author Allan Brighton
 */
public final class ObsCatalog extends SkycatCatalog {

    // A shared instance of this class */
    public static final ObsCatalog INSTANCE = new ObsCatalog();

    // Used to cache instrument specific parameter information
    private static final Map<String, Tuple2<FieldDescAdapter[], Boolean>> _instParamTab =
            new TreeMap<>();

    @SuppressWarnings("WeakerAccess")
    public ObsCatalog() {
        super(newConfigEntry());
    }

    // Return the configuration entry for this catalog.
    public static SkycatConfigEntry newConfigEntry() {
        final SkycatConfigEntry _configEntry;
        final Properties p = new Properties();
        p.setProperty(SkycatConfigFile.SERV_TYPE, "catalog");
        p.setProperty(SkycatConfigFile.LONG_NAME, "Gemini Science Program Database");
        p.setProperty(SkycatConfigFile.SHORT_NAME, "spdb");
        _configEntry = new SkycatConfigEntry(p);
        _configEntry.setParamDesc(newParamDesc());
        return _configEntry;
    }

    // Return an array describing the query parameters for this catalog
    private static FieldDescAdapter[] newParamDesc() {
        final Collection<FieldDescAdapter> params = new ArrayList<>();
        FieldDescAdapter p;
        final String wildcards = " Wildcards: * or %, ?, |";

        // Row 1
        p = new FieldDescAdapter(ObsCatalogInfo.PROG_REF);
        p.setDescription(
                "Enter wildcard expression for Gemini science program reference number. " +
                        wildcards);
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.INSTRUMENT);
        p.setDescription("Select the instrument used by the observation");
        p.setOptions(_getStringOptions(ObsCatalogInfo.INSTRUMENTS));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.TARGET_NAME);
        p.setDescription(
                "Enter wildcard expression for base position target name" + wildcards);
        params.add(p);


        // Row 2
        p = new FieldDescAdapter(ObsCatalogInfo.SEMESTER);
        p.setDescription(
                "Enter wildcard expression for the observing semester (2004A, 2004B, ...)");
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.AO);
        p.setDescription("Select the adaptive optics setting");
        p.setOptions(_getEnumOptions(AOConstants.AO.values()));
        params.add(p);


        p = new FieldDescAdapter(ObsCatalogInfo.MIN_RA);
        p.setDescription("Min Right Ascension, format: hh:mm:ss.sss or hh.hhh");
        params.add(p);


        // Row 3
        p = new FieldDescAdapter(ObsCatalogInfo.QUEUE_BAND);
        p.setDescription("Select one or more queue bands to search for");
        p.setOptions(_getQueueBandOptions());
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.PRIORITY);
        p.setDescription("Select the observation priority");
        p.setOptions(_getStringOptions(SpTypeUtil.getFormattedDisplayValueAndDescriptions(SPObservation.Priority.class)));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.MAX_RA);
        p.setDescription("Max Right Ascension, format: hh:mm:ss.sss or hh.hhh");
        params.add(p);


        // Row 4
        p = new FieldDescAdapter(ObsCatalogInfo.ACTIVE);
        p.setDescription("Search only active/inactive programs");
        p.setOptions(_getEnumOptions(SPProgram.Active.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.OBS_CLASS);
        p.setDescription("Search the observation class");
        p.setOptions(_getEnumOptions(ObsClass.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.MIN_DEC);
        p.setDescription("Min Declination, format: dd:mm:ss.sss or dd.ddd");
        params.add(p);


        // Row 5
        p = new FieldDescAdapter(ObsCatalogInfo.COMPLETED);
        p.setDescription("Search only completed/uncompleted programs");
        p.setOptions(_getEnumOptions(YesNoType.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.OBS_STATUS);
        p.setDescription("Select the Gemini observation status");
        p.setOptions(_getEnumOptions(ObservationStatus.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.MAX_DEC);
        p.setDescription("Max Declination, format: dd:mm:ss.sss or dd.ddd");
        params.add(p);


        // Row 6
        p = new FieldDescAdapter(ObsCatalogInfo.ROLLOVER);
        p.setDescription("Search only programs with rollover status");
        p.setOptions(_getEnumOptions(YesNoType.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.TOO);
        p.setDescription("Select the required TOO status");
        p.setOptions(_getEnumOptions(TooType.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.SKY_BACKGROUND);
        p.setDescription("Select the required sky background observing conditions");
        p.setOptions(_getEnumOptions(SPSiteQuality.SkyBackground.values()));
        params.add(p);


        // Row 7
        p = new FieldDescAdapter(ObsCatalogInfo.THESIS);
        p.setDescription("Search only programs related to supporting a graduate thesis");
        p.setOptions(_getEnumOptions(YesNoType.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.OBS_QA);
        p.setDescription("Select the Gemini observation QA state");
        p.setOptions(_getEnumOptions(ObsQaState.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.CLOUD_COVER);
        p.setDescription("Select the required cloud cover observing conditions");
        p.setOptions(_getEnumOptions(SPSiteQuality.CloudCover.values()));
        params.add(p);


        // Row 8
        p = new FieldDescAdapter(ObsCatalogInfo.PI_LAST_NAME);
        p.setDescription(
                "Enter wildcard expression for last name of primary investigator." +
                        wildcards);
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.DATAFLOW_STEP);
        p.setDescription("Select the dataflow step");
        p.setOptions(_getDataflowStatusNameValues());
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.IMAGE_QUALITY);
        p.setDescription("Select the required image quality observing conditions");
        p.setOptions(_getEnumOptions(SPSiteQuality.ImageQuality.values()));
        params.add(p);


        // Row 9
        p = new FieldDescAdapter(ObsCatalogInfo.EMAIL);
        p.setDescription("Enter wildcard expression for the email address of the PI or contact scientists. "
                + wildcards);
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.PARTNER_COUNTRY);
        p.setDescription("Select the partner country");
        p.setOptions(_getEnumOptions(Affiliate.values()));
        params.add(p);

        p = new FieldDescAdapter(ObsCatalogInfo.WATER_VAPOR);
        p.setDescription("Select the required water vapor observing conditions");
        p.setOptions(_getEnumOptions(SPSiteQuality.WaterVapor.values()));
        params.add(p);

        final FieldDescAdapter[] paramDesc = new FieldDescAdapter[params.size()];
        params.toArray(paramDesc);
        return paramDesc;
    }

    private static NameValue[] _getDataflowStatusNameValues() {
        final List<NameValue> res = new ArrayList<>();
        res.add(new NameValue(DataflowStatus$.MODULE$.NoData(), DataflowStatus$.MODULE$.NoData()));
        for (DataflowStatus status : DataflowStatus$.MODULE$.AllJava()) {
            res.add(new NameValue(status.description(), status.description()));
        }
        final NameValue[] resA = new NameValue[res.size()];
        return res.toArray(resA);
    }

    /**
     * Return the query parameters specific to the given instrument.
     */
    public static FieldDescAdapter[] getInstrumentParamDesc(String instName) {
        final Boolean showObsolete = BrowserPreferences.fetch().showObsoleteOptions();
        final Tuple2<FieldDescAdapter[], Boolean> tup = _instParamTab.get(instName);
        if ((tup != null) && (tup._2() == showObsolete)) {
            return tup._1();
        }

        final Collection<FieldDescAdapter> params = new ArrayList<>();

        final List<InstConfigInfo> instConfigInfoList = ObsCatalogInfo.getInstConfigInfoList(instName);
        if (instConfigInfoList == null) {
            return null;
        }

        // SCT-295: Bryan requested that we not show obsolete choices
        // in the browser
        // 2009B: Inger needs obsolete choices for statistics she makes
        instConfigInfoList.stream().filter(InstConfigInfo::isQueryable).forEach(info -> {
            final FieldDescAdapter p = new FieldDescAdapter(info.getName());
            p.setDescription("Select the " + info.getDescription());

            // SCT-295: Bryan requested that we not show obsolete choices
            // in the browser
            // 2009B: Inger needs obsolete choices for statistics she makes
            final Enum<?>[] types = showObsolete ? info.getAllTypes() : info.getValidTypes();
            if (types != null) {
                p.setOptions(_getEnumOptions(types));
            }
            params.add(p);
        });

        final FieldDescAdapter[] paramDesc = new FieldDescAdapter[params.size()];
        params.toArray(paramDesc);
        _instParamTab.put(instName, new Pair<>(paramDesc, showObsolete));
        return paramDesc;
    }


    /**
     * Return an array of NameValue objects containing all of the items in the
     * given array.
     */
    private static NameValue[] _getStringOptions(String[] ar) {
        final NameValue[] nv = new NameValue[ar.length];
        for (int i = 0; i < ar.length; i++) {
            nv[i] = new NameValue(ar[i], ar[i]);
        }
        return nv;
    }

    /**
     * Return an array describing the options for the queue band.
     */
    private static NameValue[] _getQueueBandOptions() {
        // XXX What are the allowed values? 1,2,3,4?
        final NameValue[] nv = new NameValue[4];
        for (int i = 0; i < nv.length; i++) {
            final String s = Integer.toString(i + 1);
            nv[i] = new NameValue(s, s);
        }
        return nv;
    }

    private static NameValue[] _getEnumOptions(Enum<?>[] types) {
        int i = 0;
        final NameValue[] nv = new NameValue[types.length];
        for (Enum<?> e : types) {
            String displayValue = e.name();
            if (e instanceof DisplayableSpType) {
                displayValue = ((DisplayableSpType) e).displayValue();
            }
            if ((e instanceof ObsoletableSpType) && ((ObsoletableSpType) e).isObsolete()) {
                displayValue = "* " + displayValue;
            }
            final String value = e.name();
            nv[i] = new NameValue(displayValue, value);
            i++;
        }
        return nv;
    }

    /**
     * Query the spdb using the given argument and return the resulting table.
     *
     * @param args An object describing the query arguments.
     * @return An object describing the result of the query (a SkycatTable).
     */
    @Override
    public QueryResult query(QueryArgs args) {
        throw new UnsupportedOperationException();
    }

    public static QueryManager QUERY_MANAGER = () -> {

        // Create a catalog (cat) object based on the science program database
        final IDBDatabaseService db = SPDB.get();
        if (db == null) {
            DialogUtil.error("SessionManager returned a null database.");
            return;
        }

        //final Navigator navigator = NavigatorManager.open();
        final scala.swing.Frame browserFrame = ObsCatalogFrame.instance();
        browserFrame.visible_$eq(true);
        ViewerService.instance().get().registerView(browserFrame);
        browserFrame.peer().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                ViewerService.instance().get().unregisterView(browserFrame);
            }
        });
    };

}


