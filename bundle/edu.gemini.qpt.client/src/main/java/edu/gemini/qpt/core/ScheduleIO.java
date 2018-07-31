package edu.gemini.qpt.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedSet;
import java.util.concurrent.TimeoutException;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.qpt.shared.sp.MiniModel;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlException;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import edu.gemini.util.security.auth.keychain.KeyChain;

public class ScheduleIO {

    static final String PROP_ROOT = "qpt-archive";
    static final String PROP_VERSION = "version";
    static final String PROP_SITE = "site";
    static final String PROP_TIMESTAMP = "timestamp";
    static final String PROP_SCHEDULE = "schedule";
    static final String PROP_DIGEST = "digest";

    static final int VERSION_PRE_104 = 1;
    static final int VERSION_104 = 104;   // 1.0.4
    static final int VERSION_1030 = 1030; // 1.0.30 (new infrastructure, Dec 2013)
    static final int VERSION_1031 = 1031; // 1.0.31 (additional facilities (enums) as part of QV, Jan 2014)
    static final int VERSION_1032 = 1032; // 18B.1.1.5 (switch to civil twilight)
    static final int VERSION_CURRENT = VERSION_1032;

    public static void write(Schedule sched, File file) throws IOException {
        try {

            PioFactory factory = new PioXmlFactory();
            ParamSet qptArchive = factory.createParamSet(PROP_ROOT);
            Pio.addIntParam(factory, qptArchive, PROP_VERSION, VERSION_CURRENT);
            Pio.addEnumParam(factory, qptArchive, PROP_SITE, sched.getSite());
            Pio.addLongParam(factory, qptArchive, PROP_TIMESTAMP, System.currentTimeMillis());
            ParamSet core = sched.getParamSet(factory, PROP_SCHEDULE);
            Pio.addParam(factory, qptArchive, PROP_DIGEST, digest(core));

            qptArchive.addParamSet(core);
            PioXmlUtil.write(qptArchive, file);

        } catch (Exception e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }

    }

    public static Schedule read(File file, long timeout, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) throws IOException, TimeoutException {
        return read(file.toURL(), timeout, authClient, magTable);
    }

    public static Schedule read(URL url, long timeout, KeyChain authClient, AgsMagnitude.MagnitudeTable magTable) throws IOException, TimeoutException {

        try {

            InputStream is = url.openStream();
            Reader reader = new InputStreamReader(is);

            ParamSet node = (ParamSet) PioXmlUtil.read(reader);
            int version = Pio.getIntValue(node, PROP_VERSION, Integer.MAX_VALUE);
            if (version > VERSION_CURRENT) throw new IOException("This file was written by a newer version of this software.");

            ParamSet core = node.getParamSet(PROP_SCHEDULE);

            // Extract the site
            final Site parsedSite = Site.tryParse(Pio.getValue(node, PROP_SITE, Site.GS.name()));
            final Site siteDesc = (parsedSite == null) ? Site.GS : parsedSite;
            final Peer peer = authClient.asJava().peer(siteDesc);
            if (peer == null)
                throw new IOException("No peer found for " + siteDesc);

            // HACK: Extract the blocks to find the start of the schedule. This is lame.
            Schedule.BlockUnion blocks = Schedule.getBlockUnion(core);
            SortedSet<Block> intervals = blocks.getIntervals();
            if (intervals.size() == 0) throw new IOException("Schedule has no blocks.");

            if (LttsServicesClient.getInstance() == null) {
                LttsServicesClient.newInstance(intervals.first().getStart(), peer);
            }

            // Also need to extract the extra semesters, doh
            StringSet extraSemesters = Schedule.getExtraSemesters(core);

            try {
                MiniModel model = MiniModel.newInstance(authClient, peer, intervals.last().getEnd(), extraSemesters, magTable);
                return new Schedule(model, core, version);
            } finally {
            }

        } catch (TimeoutException te) {
            throw te;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception pxe) {
            IOException ioe = new IOException(pxe.getMessage());
            ioe.initCause(pxe);
            throw ioe;
        }


    }

    private static String digest(ParamSet ps) throws PioXmlException, NoSuchAlgorithmException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PioXmlUtil.write(ps, new OutputStreamWriter(baos));
        MessageDigest md = MessageDigest.getInstance("MD5");
        return new BigInteger(md.digest(baos.toByteArray())).toString(16);
    }

}
