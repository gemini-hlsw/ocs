// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosNorth;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGnirs;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.smartgcal.UpdatableCalibrationRepository;
import edu.gemini.spModel.smartgcal.provider.CalibrationProviderImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Random;

// this is mainly meant for manual testing
@Ignore
public class UpdaterTest implements ActionListener {

    private static final Random random = new Random();

    private UpdatableCalibrationRepository fileCache;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void doUpdate() throws InterruptedException, IOException {

        fileCache = new CalibrationFileCache(temporaryFolder.newFolder("test"));
        CalibrationRemoteRepository remoteRepository = new CalibrationRemoteRepository("sbfux.cl.gemini.edu", 8443);
        CalibrationProviderImpl calibrationProvider = new CalibrationProviderImpl(fileCache);
        CalibrationProviderHolder.setProvider(calibrationProvider);

        CalibrationUpdater.instance.addListener(this);

        for (int i = 0; i < 10; i++) {
            CalibrationConsumer consumer = new CalibrationConsumer();
            consumer.start();
        }

        CalibrationUpdater.instance.start(fileCache, remoteRepository, 2);


        Thread.sleep(600000); // let run for a while


    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        CalibrationUpdateEvent event = (CalibrationUpdateEvent) actionEvent;
        CalibrationProviderHolder.setProvider(new CalibrationProviderImpl(fileCache));
        System.out.println("Wow, did it : " + ((CalibrationUpdateEvent)actionEvent).getUpdatedFiles());
    }

    //-- a simple calibration consumer
    private class CalibrationConsumer extends Thread {

        protected CalibrationConsumer() {
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    readGMOSNCalibration();
                    readGNIRSCalibration();
                    Thread.sleep(random.nextInt(2000));
                } catch (InterruptedException e) {
                    e.printStackTrace(); // ignore
                }
            }
        }

        private void readGNIRSCalibration() {
            List<Calibration> calibrations = CalibrationProviderHolder.getProvider().getCalibrations(
                    new CalibrationKeyImpl.WithWavelength(
                            new ConfigKeyGnirs(
                                    CalibrationProvider.GNIRSMode.SPECTROSCOPY,
                                    GNIRSParams.PixelScale.PS_005,
                                    GNIRSParams.Disperser.D_10,
                                    GNIRSParams.CrossDispersed.LXD,
                                    GNIRSParams.SlitWidth.SW_1,
                                    GNIRSParams.WellDepth.SHALLOW
                            ),
                            1.11
                    )
            );
            Assert.assertNotNull(calibrations);
            Assert.assertTrue(calibrations.size() > 0);
            //System.out.println("successfully read GNIRS calibrations");
        }

        private void readGMOSNCalibration() {
            List<Calibration> calibrations = CalibrationProviderHolder.getProvider().getCalibrations(
                    new CalibrationKeyImpl.WithWavelength(
                            new ConfigKeyGmosNorth(
                                    GmosNorthType.DisperserNorth.MIRROR,
                                    GmosNorthType.FilterNorth.r_G0303,
                                    GmosNorthType.FPUnitNorth.FPU_NONE,
                                    GmosCommonType.Binning.ONE,
                                    GmosCommonType.Binning.ONE,
                                    GmosCommonType.Order.ZERO,
                                    GmosCommonType.AmpGain.LOW
                            ),
                            500.0
                    )
            );
            Assert.assertNotNull(calibrations);
            Assert.assertTrue(calibrations.size() > 0);
            //System.out.println("successfully read GMOS-N calibrations");
        }

    }


}
