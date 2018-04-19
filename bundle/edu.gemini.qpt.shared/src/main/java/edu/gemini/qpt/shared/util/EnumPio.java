package edu.gemini.qpt.shared.util;

import edu.gemini.qpt.shared.sp.Inst;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

// Pulled from Schedule to be reused in serializing Pio information.

public final class EnumPio {
    private static final Logger LOGGER = Logger.getLogger(EnumPio.class.getName());

    private EnumPio() {}

    public static final String PROP_MEMBER_CLASS = "class";
    public static final String PROP_MEMBER_NAME  = "name";

    public static ParamSet getEnumParamSet(final PioFactory factory, final String name, final Enum<?> e) {
        final ParamSet params = factory.createParamSet(name);
        Pio.addParam(factory, params, PROP_MEMBER_CLASS, e.getClass().getName());
        Pio.addParam(factory, params, PROP_MEMBER_NAME,  e.name());
        return params;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Enum<?> getEnum(final ParamSet params) {
        try {
            String cname = Pio.getValue(params, PROP_MEMBER_CLASS);
            String ename = Pio.getValue(params, PROP_MEMBER_NAME);

            // For backward-compatability from when the sp package was
            // called sp101. This was an annoying mistake, sorry.
            cname = cname.replace("sp101", "sp");

            // Patch for Gmos change
            if (cname.equals("edu.gemini.spModel.gemini.gmos.GmosNorthType$DetectorManufacturerNorth")
                    || cname.equals("edu.gemini.spModel.gemini.gmos.GmosSouthType$DetectorManufacturerSouth")) {
                cname = "edu.gemini.spModel.gemini.gmos.GmosCommonType$DetectorManufacturer";
            }

            // Patch for package change
            if (cname.equals("edu.gemini.qpt.core.sp.Inst")) cname = Inst.class.getName();

            // We need to do this for subclassed enum constants like
            // GMOSNorthType.FilterNorth.u_G0308, which has a runtime type of
            // GMOSNorthType$FilterNorth$1, which is not actually an enum type.
            // This is a little puzzling.
            Class c = Class.forName(cname);
            while (c != null) {
                try {
                    return Enum.valueOf(c, ename);
                } catch (IllegalArgumentException iae) {
                    c = c.getSuperclass();
                }
            }

            throw new Exception(cname + " doesn't appear to be an enum class, or " + ename + " is a bogus token.");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble deserializing enum type.", e);
            return null;
        }
    }

}
