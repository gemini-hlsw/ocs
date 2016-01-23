package edu.gemini.wdba.tcc;

/**
 * The ITccInstrumentSupport interface must be implemented by every supported
 * Gemini instrument.  This is where the particulars of a specific instrument
 * are handled and given to the main code.
 */
public interface ITccInstrumentSupport {

    /**
     * Returns the appropriate wavelength for the observation for the TCC config.
     * The wavelength should be in microns!
     * @return String wavelength acceptable to TCC or null if not relevant.
     */
    String getWavelength();

    /**
     * Return the position angle from the OT
     * @return the position angle as a String
     */
    String getPositionAngle();

    /**
     * Return the guide configuration name
     * @return the guide configuration name or null if not relevant
     */
    String getTccConfigInstrument();

    /**
     * Return the name of the instrument origins configuration
     * @return a String that refers to a config file in the TCC
     */
    String getTccConfigInstrumentOrigin();

    /**
     * Return true if the instrument is using a fixed rotator position.  In this case the pos angle is used
     * in a special rotator config
     * @return String value that is the name of the fixed rotator config or null if no special name is needed
     */
    String getFixedRotatorConfigName();

    /**
     * Returns the TCC chop parameter value.
     * @return Chop value or null if there is no chop parameter for this instrument.
     */
    String getChopState();

    /**
     * Return the name of the guide wavelenght configuration
     * @param p the <cc>ParamSet</cc> that the instrument should use to add guide details
     */
    void addGuideDetails(ParamSet p);

    /**
     * A default class that implements all the methods ready for extending.
     */
    class DefaultInstrumentSupport implements ITccInstrumentSupport {

        protected ObservationEnvironment _oe;

        // The private constructor
        private DefaultInstrumentSupport(ObservationEnvironment oe) {
            _oe = oe;
        }

        /**
         * The create method returns a new instance, the oe is saved for use
         * by subclasses.
         * @param oe the ObservationEnvironement
         * @return the new instance
         */
        static public ITccInstrumentSupport create(ObservationEnvironment oe) {
            return new DefaultInstrumentSupport(oe);
        }

        /**
         * The default getWavelength returns null, which is probably not cool.
         * @return null
         */
        public String getWavelength() {
            return null;
        }

        /**
         * The default getPositionAngle returns 0 position angle in degrees
         * @return 0.0 as a String
         */
        public String getPositionAngle() {
            return "0.0";
        }

        /**
         * The default getTccConfigInstrument returns null.
         * @return null
         */
        public String getTccConfigInstrument() {
            return null;
        }

        /**
         * The default getTccConfigInstrumentOrigin returns null;
         */
        public String getTccConfigInstrumentOrigin() {
            return null;
        }

        /**
         * The default fixed state is null meaning no special config name.
         * @return the String name of the fixed rotator config or null if no special name is needed
         */
        public String getFixedRotatorConfigName() {
            return null;
        }

        /**
         * Returns the TCC chop parameter value.
         *
         * @return Chop value or null if there is no chop parameter for this instrument.
         */
        public String getChopState() {
            return null;
        }

        /**
         * The default addGuideDetails does nothing
         * @param p a ParamSet that is ignored.
         */
        public void addGuideDetails(ParamSet p) {
        }
    }
}
