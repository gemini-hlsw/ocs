//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor;
import static edu.gemini.catalog.skycat.table.CatalogValueExtractor.MagnitudeDescriptor;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;

import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.util.immutable.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Support for common {@link SkyObjectFactory} implementations.
 */
public final class FactorySupport implements SkyObjectFactory {

    /**
     * Builder for the FactorySupport.
     */
    public static final class Builder {
        private String idCol;
        private String raCol;
        private String decCol;

        private Option<String> epochCol = None.instance();
        private Option<String> pmRaCol  = None.instance();
        private Option<String> pmDecCol = None.instance();

        private ImList<MagnitudeDescriptor> magDescriptors;

        public Builder(String idCol, String raCol, String decCol) {
            if ((idCol == null) || (raCol == null) || (decCol == null)) {
                throw new IllegalArgumentException();
            }

            this.idCol  = idCol;
            this.raCol  = raCol;
            this.decCol = decCol;
            this.magDescriptors = ImCollections.emptyList();
        }

        public Builder epoch(String epochCol) {
            if (epochCol == null) throw new IllegalArgumentException();
            this.epochCol = new Some<String>(epochCol);
            return this;
        }

        public Builder epoch(Option<String> epochCol) {
            if (epochCol == null) throw new IllegalArgumentException();
            this.epochCol = epochCol;
            return this;
        }

        public Builder pmRa(String pmRaCol) {
            if (pmRaCol == null) throw new IllegalArgumentException();
            this.pmRaCol = new Some<String>(pmRaCol);
            return this;
        }

        public Builder pmRa(Option<String> pmRaCol) {
            if (pmRaCol == null) throw new IllegalArgumentException();
            this.pmRaCol = pmRaCol;
            return this;
        }

        public Builder pmDec(String pmDecCol) {
            if (pmDecCol == null) throw new IllegalArgumentException();
            this.pmDecCol = new Some<String>(pmDecCol);
            return this;
        }

        public Builder pmDec(Option<String> pmDecCol) {
            if (pmDecCol == null) throw new IllegalArgumentException();
            this.pmDecCol = pmDecCol;
            return this;
        }

        public Builder magDescriptors(ImList<MagnitudeDescriptor> magDescriptors) {
            this.magDescriptors = magDescriptors;
            return this;
        }

        public Builder add(MagnitudeDescriptor... desc) {
            magDescriptors = magDescriptors.append(DefaultImList.create(desc));
            return this;
        }

        public FactorySupport build() {
            return new FactorySupport(this);
        }
    }

    private final String idCol;
    private final String raCol;
    private final String decCol;
    private final Option<String> epochCol;
    private final Option<String> pmRaCol;
    private final Option<String> pmDecCol;
    private final ImList<MagnitudeDescriptor> magDescriptors;

    public FactorySupport(Builder b) {
        this.idCol    = b.idCol;
        this.raCol    = b.raCol;
        this.decCol   = b.decCol;
        this.epochCol = b.epochCol;
        this.pmRaCol  = b.pmRaCol;
        this.pmDecCol = b.pmDecCol;
        this.magDescriptors = b.magDescriptors;
    }

    @Override
    public Set<Magnitude.Band> bands() {
        Set set = new HashSet(magDescriptors.size());
        for(MagnitudeDescriptor md : magDescriptors) {
            set.add(md.getBand());
        }
        return set;
    }

    @Override
    public String getMagColumn(Magnitude.Band band) {
        for(MagnitudeDescriptor m : magDescriptors) {
            if (m.getBand() == band) {
                return m.getMagnitudeColumn();
            }
        }
        return null;
    }

    @Override
    public SkyObject create(CatalogHeader header, CatalogRow row) throws CatalogException {
        CatalogValueExtractor ext = new CatalogValueExtractor(header, row);
        String id = ext.getString(idCol);
        Angle ra  = ext.getRa(raCol);
        Angle dec = ext.getDec(decCol);

        ImList<Magnitude> mags = ImCollections.emptyList();
        if (magDescriptors.size() > 0) mags = ext.getMagnitudes(magDescriptors);

        HmsDegCoordinates.Builder b = new HmsDegCoordinates.Builder(ra, dec);
        b = b.epoch(HmsDegCoordinates.Epoch.J2000);

        if (!epochCol.isEmpty()) {
            Option<Double> epoch = ext.getOptionalDouble(epochCol.getValue());
            if (!epoch.isEmpty()) {
                double d = epoch.getValue();

                HmsDegCoordinates.Epoch.Type type = HmsDegCoordinates.Epoch.Type.JULIAN;
                if (d < 2000.0) {
                    throw new IllegalArgumentException("Unsupported epoch: " + epoch);
                }

                b = b.epoch(new HmsDegCoordinates.Epoch(type, d));
            }
        }

        if (!pmRaCol.isEmpty()) {
            Option<Double> pmRa = ext.getOptionalDouble(pmRaCol.getValue());
            if (!pmRa.isEmpty()) {
                b = b.pmRa(new Angle(pmRa.getValue(), Angle.Unit.MILLIARCSECS));
            }
        }
        if (!pmDecCol.isEmpty()) {
            Option<Double> pmDec = ext.getOptionalDouble(pmDecCol.getValue());
            if (!pmDec.isEmpty()) {
                b = b.pmDec(new Angle(pmDec.getValue(), Angle.Unit.MILLIARCSECS));
            }
        }
        HmsDegCoordinates coords = b.build();

        return (new SkyObject.Builder(id, coords)).magnitudes(mags).build();
    }
}
