/**
 * $Id: GmosTiltInfo.java 6526 2005-08-03 21:27:13Z brighton $
 */

package edu.gemini.mask;

/**
 * Used to determine gRequest, gTilt, Anamorphic factor, linear disperson
 */
class GmosTiltInfo {

    private static final GmosTiltInfo[] TILT = new GmosTiltInfo[]{
        //               gRequest        gTilt
        new GmosTiltInfo(1.64278760968657, 0),
        new GmosTiltInfo(1.62916808620627, 1),
        new GmosTiltInfo(1.61505230234479, 2),
        new GmosTiltInfo(1.60044455790666, 3),
        new GmosTiltInfo(1.58534930255234, 4),
        new GmosTiltInfo(1.56977113444283, 5),
        new GmosTiltInfo(1.55371479883906, 6),
        new GmosTiltInfo(1.53718518665639, 7),
        new GmosTiltInfo(1.52018733297482, 8),
        new GmosTiltInfo(1.50272641550524, 9),
        new GmosTiltInfo(1.48480775301225, 10),
        new GmosTiltInfo(1.46643680369405, 11),
        new GmosTiltInfo(1.44761916351974, 12),
        new GmosTiltInfo(1.42836056452483, 13),
        new GmosTiltInfo(1.40866687306512, 14),
        new GmosTiltInfo(1.38854408802982, 15),
        new GmosTiltInfo(1.36799833901417, 16),
        new GmosTiltInfo(1.34703588445236, 17),
        new GmosTiltInfo(1.32566310971111, 18),
        new GmosTiltInfo(1.30388652514467, 19),
        new GmosTiltInfo(1.28171276411163, 20),
        new GmosTiltInfo(1.25914858095441, 21),
        new GmosTiltInfo(1.23620084894178, 22),
        new GmosTiltInfo(1.21287655817523, 23),
        new GmosTiltInfo(1.18918281345965, 24),
        new GmosTiltInfo(1.16512683213922, 25),
        new GmosTiltInfo(1.14071594189888, 26),
        new GmosTiltInfo(1.11595757853228, 27),
        new GmosTiltInfo(1.09085928367673, 28),
        new GmosTiltInfo(1.06542870251599, 29),
        new GmosTiltInfo(1.03967358145141, 30),
        new GmosTiltInfo(1.01360176574239, 31),
        new GmosTiltInfo(0.98722119711654, 32),
        new GmosTiltInfo(0.96053991135062, 33),
        new GmosTiltInfo(0.93356603582274, 34),
        new GmosTiltInfo(0.90630778703669, 35),
        new GmosTiltInfo(0.87877346811911, 36),
        new GmosTiltInfo(0.85097146629028, 37),
        new GmosTiltInfo(0.82291025030926, 38),
        new GmosTiltInfo(0.79459836789429, 39),
        new GmosTiltInfo(0.76604444311901, 40),
        new GmosTiltInfo(0.73725717378552, 41),
        new GmosTiltInfo(0.70824532877493, 42),
        new GmosTiltInfo(0.67901774537626, 43),
        new GmosTiltInfo(0.64958332659456, 44),
        new GmosTiltInfo(0.61995103843892, 45),
        new GmosTiltInfo(0.59012990719137, 46),
        new GmosTiltInfo(0.56012901665738, 47),
        new GmosTiltInfo(0.52995750539882, 48),
        new GmosTiltInfo(0.49962456395030, 49),
        new GmosTiltInfo(0.46913943201963, 50),
        new GmosTiltInfo(0.43851139567332, 51),
        new GmosTiltInfo(0.40774978450792, 52),
        new GmosTiltInfo(0.37686396880820, 53),
        new GmosTiltInfo(0.34586335669282, 54),
        new GmosTiltInfo(0.31475739124854, 55),
        new GmosTiltInfo(0.28355554765376, 56),
        new GmosTiltInfo(0.25226733029230, 57),
        new GmosTiltInfo(0.22090226985827, 58),
        new GmosTiltInfo(0.18946992045291, 59),
        new GmosTiltInfo(0.15797985667434, 60),
        new GmosTiltInfo(0.12644167070104, 61),
        new GmosTiltInfo(0.09486496936998, 62),
        new GmosTiltInfo(0.06325937125028, 63),
        new GmosTiltInfo(0.03163450371328, 64),
        new GmosTiltInfo(0.00000000000000, 65),
    };

    double gRequest;
    int gTilt;

    GmosTiltInfo(double gRequest, int gTilt) {
        this.gRequest = gRequest;
        this.gTilt = gTilt;
    }

    /**
     * Calculate Gtilt from the the GRequest
     */
    static double calcGTilt(double gRequest) {
        double max = TILT[0].gRequest;
        double min = max;
        int i = 0;
        while (i < TILT.length) {
            min = TILT[i].gRequest;
            if (gRequest < min) {
                max = min;
            } else {
                break;
            }
            i++;
        }
        // Do a linear interpolation of the gTilt
        if (min == max) {
            return TILT[i].gTilt;
        }
        int gtiltMax = TILT[i].gTilt;
        int gtiltMin = TILT[i - 1].gTilt;
        double slope = (gtiltMin - gtiltMax) / (min - max);
        double xo = (gtiltMax - (slope * max));
        return (slope * gRequest) + xo;
    }
}
