/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeGuideStarDialog.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package jsky.app.ot.tpe;

import edu.gemini.shared.cat.*;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.gui.CatalogNavigator;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A dialog to choose a method for selecting a guide star. The choices
 * are to use a guide star catalog to get the best choice based on the
 * current observation, or to select the guide star position manually.
 */
public class TpeGuideStarDialog extends TpeGuideStarDialogForm
        implements ActionListener, TreeModelListener {

    // Key used to save catalog choice between sessions
    private static final String _CATALOG_KEY = TpeGuideStarDialog.class.getName() + ".catName";

    // Key used to save guide tag choice between sessions
    private static final String _GUIDE_TAG_KEY = TpeGuideStarDialog.class.getName() + ".guideTag";

    private static final String PWFS = "PWFS";
    private static final String PWFS1 = "PWFS1";
    private static final String PWFS2 = "PWFS2";
    private static final String OIWFS = "OIWFS";
    private static final String AOWFS = "AOWFS";
    private static final String AOWFS_NGS = "AOWFS NGS";
    private static final String AOWFS_LGS = "AOWFS LGS";

    private static final String CATALOG_2MASS = "2MASS";
    private static final String CATALOG_UCAC4 = "UCAC4";

    private static final String OPTICAL_DEFAULT_CATALOG = CATALOG_UCAC4;

    private static final String EMPTY_TEXT = " ";
    /**
     * The available Guide Tags
     */
    private static final String[] GUIDE_TAGS = {
            PWFS1,
            PWFS2,
            OIWFS,
            AOWFS_NGS,
            AOWFS_LGS
    };

    private static final String ACQCAM = InstAcqCam.SP_TYPE.readableStr;
    private static final String BHROS = InstBHROS.SP_TYPE.readableStr;
    private static final String FLAMINGOS2 = Flamingos2.SP_TYPE.readableStr;
    private static final String GPI = Gpi.SP_TYPE.readableStr;
    private static final String NICI = InstNICI.SP_TYPE.readableStr;
    private static final String GMOS_S = InstGmosSouth.SP_TYPE.readableStr;
    private static final String GMOS_N = InstGmosNorth.SP_TYPE.readableStr;
    private static final String GNIRS = InstGNIRS.SP_TYPE.readableStr;
    private static final String GSAOI = Gsaoi.SP_TYPE.readableStr;
    private static final String MICHELLE = InstMichelle.SP_TYPE.readableStr;
    private static final String NIRI = InstNIRI.SP_TYPE.readableStr;
    private static final String NIFS = InstNIFS.SP_TYPE.readableStr;

    private static final String NIFS_NO_FIELD = "NIFS+Altair w/o field lens";
    private static final String NIFS_FIELD = "NIFS+Altair w/ field lens";

    private static final String PHOENIX = InstPhoenix.SP_TYPE.readableStr;
    private static final String TEXES = InstTexes.SP_TYPE.readableStr;
    private static final String TRECS = InstTReCS.SP_TYPE.readableStr;

    /** The available instruments to choose from */
    public static final String[] INSTRUMENTS = {
        ACQCAM,
        BHROS,
        NICI,
        FLAMINGOS2,
        GPI,
        GMOS_N,
        GMOS_S,
        GNIRS,
        GSAOI,
        MICHELLE,
        NIFS,
        NIFS_NO_FIELD,
        NIFS_FIELD,
        NIRI,
        PHOENIX,
        TEXES,
        TRECS,
    };

    // Singleton instance of this class
    private static TpeGuideStarDialog _instance;

    // Parent JFrame or JInternalFrame
    private Component _parentFrame;

    // Link to the TPE image widget
    private TpeImageWidget _iw;

    // Create a dialog for choosing the guide star selection method.
    private TpeGuideStarDialog(TpeImageWidget iw) {
        _iw = iw;

        _makeInstMenu();
        _makeGuideTagMenu();
        _makeCatalogMenu();

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
        typeComboBox.addActionListener(this);
        instComboBox.addActionListener(this);
        catalogComboBox.addActionListener(this);
        guideStarWarning.setText(EMPTY_TEXT);
        catalogWarning.setText(EMPTY_TEXT);

    }

    // Make the menu of available guide star tags
    private void _makeGuideTagMenu() {
        // restore selected catalog from previous session
        String savedTag = Preferences.get(_GUIDE_TAG_KEY);
        for (int i = 0; i < GUIDE_TAGS.length; i++) {
            String tag = GUIDE_TAGS[i];
            typeComboBox.addItem(tag);
            if (savedTag != null && savedTag.equals(tag)) {
                _setSelectedIndex(typeComboBox, i);
            }
        }
    }

    /**
     * Check for potential conflicts between an instrument and a
     * given guide star.
     * <p/>
     * Currently, the conflicts are related to NIFS only (SCT-156):
     * <p/>
     * If NIFS+Altair w/ field lens is selected, all the
     * guide stars type can be used but PWFS2.
     * <p/>
     * If NIFS+Altair w/o field lens is selected, PWFS2 and AOWFS LGS
     * are not allowed
     * <p/>
     * If NIFS is selected only PWFS1 and PWFS2 are allowed
     */
    private void _checkInstrumentGuideStar() {
        String instrument = instComboBox.getItemAt(instComboBox.getSelectedIndex());
        String tag = typeComboBox.getItemAt(typeComboBox.getSelectedIndex());
        boolean problem = false;

        if (instrument.equals(NIFS)) {
            if (tag.equals(OIWFS) || tag.equals(AOWFS_LGS) || tag.equals(AOWFS_NGS)) {
                problem = true;
            }
        } else if (instrument.equals(NIFS_FIELD) || instrument.equals(NIFS_NO_FIELD)) {
            if (tag.equals(PWFS2)) {
                problem = true;
            }
            if (instrument.equals(NIFS_NO_FIELD)) {
                if (tag.equals(AOWFS_LGS)) {
                    problem = true;
                }
            }
        }

        if (problem) {
            if (instrument.equals(NIFS)) {
                guideStarWarning.setText(instrument + " can't be used with " + tag + " without Altair");
            } else {
                guideStarWarning.setText(instrument + " can't be used with " + tag);
            }
            okButton.setEnabled(false);
            return;
        }
        //if nothing matches, do not show a message.
        guideStarWarning.setText(EMPTY_TEXT);
        okButton.setEnabled(true);
    }

    /**
     * Select default catalogs for a given set of instrument
     * and guide star types (SCT-156)
     * <p/>
     * The logic is the following:
     * <p/>
     * If GNIRS OIWFS, NIRI OIWFS or NIFS OIWFS is chosen the catalog
     * should be 2MASS, no matter what was the default. The system automatically
     * will select 2MASS in these cases. If the user decides to use a different
     * catalog, a warning will be displayed.
     * <p/>
     * If the Guide Star type is PWFS1, PWFS2, AOWFS LGS/NGS or OIWFS (for F2 and
     * GMOS only) a warning is displayed if the catalog is 2MASS.
     * <p>
     * Change: See REL-159:
     * - UCAC3 for PWFS1, PWFS2, and Altair AOWFS, GeMS CWFS, GMOS OIWFS
     * - 2MASS for NIFS OIWFS, GNIRS OIWFS, and NIRI OIWFS, GSAOI ODGW
     * </p>
     *
     *
     * @param changeOption true means we are allowed to change
     * the catalog option chosen by the user. Otherwise, only
     * will warn the users, but won't change what the user has selected
     */

    private void _selectDefaultCatalog(boolean changeOption) {
        String instrument = instComboBox.getItemAt(instComboBox.getSelectedIndex());
        String tag = typeComboBox.getItemAt(typeComboBox.getSelectedIndex());
        catalogWarning.setForeground(Color.BLUE);
        if (tag.equals(OIWFS)) {
            if (instrument.equals(GNIRS) || instrument.equals(NIRI) || instrument.startsWith(NIFS) || instrument.equals(GSAOI)) {
                if (_selectDefaultCatalog(CATALOG_2MASS, changeOption)) {
                    return;
                }
            } else if (instrument.equals(GMOS_N) || instrument.equals(GMOS_S)) {
                if (_selectDefaultCatalog(CATALOG_UCAC4, changeOption)) {
                    return;
                }
            } else if (instrument.equals(FLAMINGOS2)) {
                Catalog c = catalogComboBox.getItemAt(catalogComboBox.getSelectedIndex());
                if (c.getName().startsWith(CATALOG_2MASS)) {
                    catalogWarning.setText(c.getName() + " in use");
                    return;
                }
            }
        } else if (tag.startsWith(PWFS) || tag.startsWith(AOWFS)) {
             if (_selectDefaultCatalog(CATALOG_UCAC4, changeOption)) {
                 return;
             }
        }
        catalogWarning.setText(EMPTY_TEXT);
    }

    // Refactored from original code, returns true if the caller should return immediately
    private boolean _selectDefaultCatalog(String catalogName, boolean changeOption) {
        Catalog c = catalogComboBox.getItemAt(catalogComboBox.getSelectedIndex());
        if (c == null || !c.getName().startsWith(catalogName)) {
            if (changeOption) {
                catalogWarning.setText(EMPTY_TEXT);
                //select the given catalog for the user.
                int n = catalogComboBox.getItemCount();
                for (int i = 0; i < n; i++) {
                    c = catalogComboBox.getItemAt(i);
                    if (c.getName().startsWith(catalogName)) {
                        catalogComboBox.setSelectedItem(c);
                        return true;
                    }
                }
            } else {
                catalogWarning.setText(catalogName + " catalog is recommended");
                return true;
            }
        } else {
            catalogWarning.setText(EMPTY_TEXT);
            return true;
        }
        return false;
    }


    // Make the menu of available instruments
    private void _makeInstMenu() {
        for (String inst : INSTRUMENTS) {
            instComboBox.addItem(inst);
        }
    }

    // Set the parent JFrame or JInternalFrame
    private void _setParentFrame(Component c) {
        _parentFrame = c;
    }

    // return the catalog directory used by the catalog window
    private CatalogDirectory _getCatalogDirectory() {
        CatalogDirectory dir;
        try {
            dir = CatalogNavigator.getCatalogDirectory();
        } catch (Exception e) {
            DialogUtil.error(e);
            return null;
        }
        if (dir == null) {
            DialogUtil.error("No catalog config file was found");
        }
        return dir;
    }

    // Make the menu of available catalogs
    private void _makeCatalogMenu() {
        catalogComboBox.removeAllItems();

        CatalogDirectory dir = _getCatalogDirectory();
        if (dir == null) {
            return;
        }

        // restore selected catalog from previous session
        String savedCatName = Preferences.get(_CATALOG_KEY);

        int comboIndex = -1;
        int n = dir.getNumCatalogs();
        for (int i = 0; i < n; i++) {
            Catalog cat = dir.getCatalog(i);
            if (cat.getType().equals(Catalog.CATALOG)) {
                ++comboIndex;
                catalogComboBox.addItem(cat);
                if (savedCatName != null && savedCatName.equals(cat.getName())) {
                    _setSelectedIndex(catalogComboBox, comboIndex);
                } else if (cat.getName().startsWith(OPTICAL_DEFAULT_CATALOG)) {
                    _setSelectedIndex(catalogComboBox, comboIndex);
                }
            }
        }

        // update menu when the config file changes
        dir.removeTreeModelListener(this);
        dir.addTreeModelListener(this);
    }

    // -- implement the TreeModelListener interface
    // (so we can update the menus whenever the catalog tree is changed)

    public void treeNodesChanged(TreeModelEvent e) {
        _makeCatalogMenu();
    }

    public void treeNodesInserted(TreeModelEvent e) {
        _makeCatalogMenu();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        _makeCatalogMenu();
    }

    public void treeStructureChanged(TreeModelEvent e) {
        _makeCatalogMenu();
    }

    // Called when one of the dialog buttons is pushed
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src.equals(cancelButton)) {
            _cancel();
        } else if (src.equals(okButton)) {
            _ok();
        } else if (src.equals(typeComboBox)) {
            _checkInstrumentGuideStar();
            _selectDefaultCatalog(true);
        } else if (src.equals(instComboBox)) {
            _checkInstrumentGuideStar();
            _selectDefaultCatalog(true);
        } else if (src .equals(catalogComboBox)) {
            _selectDefaultCatalog(false);
        }
    }


    private void _setSelectedIndex(JComboBox<?> cb, int i) {
        if (i != cb.getSelectedIndex()) {
            cb.setSelectedIndex(i);
        } else {
            // was previously being called, even when there was no change, to make the warnings
            _checkInstrumentGuideStar();
            _selectDefaultCatalog(false);
        }
    }

    // Called when the OK button is pressed
    private void _ok() {
        try {
            _findGuideStar();
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
        _parentFrame.setVisible(false);
    }

    // Called when the Cancel button is pressed
    private void _cancel() {
        _parentFrame.setVisible(false);
    }

    // Return the instrument for the current observation (or null, if there is none)
    private static SPInstObsComp _getInst(TpeContext ctx) {
        return ctx.instrument().dataObject().isDefined() ?
                ctx.instrument().dataObject().get() : null;
    }

    //Check the AO system.
    //For Altair, will return true if the Field Lens is IN
    private String _checkAOSystem(SPInstObsComp inst) {
        if (_iw.getContext().altair().isDefined()) {
            InstAltair altair = _iw.getContext().altair().get();
            AltairParams.FieldLens fl = altair.getFieldLens();
            if (fl == AltairParams.FieldLens.IN) {
                return NIFS_FIELD;
            } else {
                return NIFS_NO_FIELD;
            }
        }
        return inst.getReadable();
    }

    // Return the name of the selected instrument (in the instComboBox)
    private String _getSelectedInst() {
        Object o = instComboBox.getSelectedItem();
        if (o != null) {
            return (String)o;
        }
        return null;
    }

    // Set the selected instrument name ("NIRI", "GMOS-N", etc...)
    private void _setSelectedInst(SPInstObsComp inst) {
        String instName = inst.getReadable();

        //If nifs, we need to check the AO System to decide what
        //will be the name to use.
        if (instName.equals(NIFS)) {
            instName = _checkAOSystem(inst);
        }

        for (int i = 0; i < INSTRUMENTS.length; i++) {
            if (instName.equals(INSTRUMENTS[i])) {
                _setSelectedIndex(instComboBox, i);
                break;
            }
        }
    }

    // Search the given catalog for the best guide star
    private void _findGuideStar() {
        Catalog cat = catalogComboBox.getItemAt(catalogComboBox.getSelectedIndex());
        Preferences.set(_CATALOG_KEY, cat.getName());

        String guideTag = typeComboBox.getItemAt(typeComboBox.getSelectedIndex());
        Preferences.set(_GUIDE_TAG_KEY, guideTag);

        SPInstObsComp inst = _getInst(_iw.getContext());
        String selectedInst = _getSelectedInst();

        _iw.setCatalogAlgorithm(_getCatalogAlgorithm(guideTag, inst, selectedInst));
        _iw.openCatalogWindow(cat);
    }


    // Return the algorithm to use for the given instrument and guide tag.
    // guideTag should be PWFS[1,2], OIWFS, AOWFS NGS or AOWFS LGS.
    // inst is the current instrument, or null if there isn't one.
    // selectedInst is the name of the instrument selected in the comboBox.
    private ICatalogAlgorithm _getCatalogAlgorithm(String guideTag, SPInstObsComp inst,
                                                   String selectedInst) {
        String instName = null;
        if (selectedInst != null) {
            instName = selectedInst;
        } else if (inst != null) {
            instName = inst.getNarrowType();
        }

        if (instName != null && guideTag != null) {
            if (guideTag.startsWith(PWFS)) { //P1 or P2
                if (inst != null && inst.isChopping()) {
                    if (guideTag.equals(PWFS1)) {
                        return new ChoppingPWFSCatalogAlgorithm();
                    } else {
                        return new ChoppingPWFS2CatalogAlgorithm();
                    }
                } else if (guideTag.equals(PWFS2) && instName.equals(GNIRS)) {//OT-615/SCT-152: GNIRS PWFS2 algorithm
                    return new GnirsPWFSCatalogAlgorithm();
                }
                return new PWFSCatalogAlgorithm();
            } else if (guideTag.equals(OIWFS)) {
                if (instName.equals(NIRI)) {
                    return new NiriOIWFSCatalogAlgorithm();
                } else if (instName.equals(NIFS_NO_FIELD)) { //OT-615/SCT-159: New algorithms for NIFS
                    return new NifsOIWFSCatalogAlgorithm();
                } else if (instName.equals(NIFS_FIELD)) {
                    return new NifsOIWFSFieldCatalogAlgorithm();
                } else if (instName.equals(GMOS_N) || instName.equals(GMOS_S) || instName.equals(BHROS)) {
                    return new GmosOIWFSCatalogAlgorithm();
                } else if (instName.equals(GNIRS)) {
                    return new GnirsOIWFSCatalogAlgorithm();
                } else if (instName.equals(FLAMINGOS2)) {
                    return new F2OIWFSCatalogAlgorithm();
                } else if (instName.equals(NICI)) {
                    return new NiciDefaultCatalogAlgorithm();
                }
//                REL-346:  MICHELLE+OIWFS, 4.25, 7.0, 13.5 ## Remove or make undefined, no Michelle OIWFS
//                else if (instName.equals(MICHELLE)) {
//                    return new MichelleOIWFSCatalogAlgorithm();
//                }
            } else if (guideTag.equals(AOWFS_NGS)) {
                if (instName.equals(NICI)) {
                    return new NiciWFSCatalogAlgorithm();
                }
                return new AltairWFSCatalogAlgorithm();
            } else if (guideTag.equals(AOWFS_LGS)) {
                return new AltairLGSCatalogAlgorithm();
            }
        }

        return new DefaultCatalogAlgorithm();
    }

    /**
     * Display the dialog
     */
    public static void showDialog(TpeImageWidget iw) {
        if (_instance != null) {
            SwingUtil.showFrame(_instance._parentFrame);
        } else {
            _instance = new TpeGuideStarDialog(iw);
            TpeGuideStarDialogFrame f = new TpeGuideStarDialogFrame(_instance);
            _instance._setParentFrame(f);
        }

        SPInstObsComp inst = _getInst(iw.getContext());
        if (inst != null) {
            _instance._setSelectedInst(inst);
        }
    }
}
