package edu.gemini.spModel.gemini.bhros.ech;

public class SpectralOrder {

    private static final String RESOURCE = "/resources/conf/orders.dat";

    public final int order; /* Order number */
    public final double changeOver; /* Changeover wavelength (microns) from previous order */

    public static final SpectralOrder[] ELEMENTS = new ArrayReader<SpectralOrder>() {
        public SpectralOrder build(String line) {
            String[] parts = line.split("\\s+");
            return new SpectralOrder(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]));
        }
    }.readArray(SpectralOrder.class.getResourceAsStream(RESOURCE), new SpectralOrder[0]);

    private SpectralOrder(int order, double changeOver) {
        this.order = order;
        this.changeOver = changeOver;
    }

    public int compareTo(Object o) {
        return (order - ((SpectralOrder) o).order);
    }

}
