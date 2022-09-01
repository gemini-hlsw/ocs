package edu.gemini.itc.ghost;

import edu.gemini.itc.base.DatFile;
import edu.gemini.itc.base.GratingOptics;
import scala.Option;
import scala.collection.Iterator;
import edu.gemini.itc.base.DatFile;

public class GhostGratingOptics extends GratingOptics {

    public GhostGratingOptics(String directory, String gratingName, String gratingsName, double centralWavelength, int detectorPixels, int spectralBinning) {
        super(directory, gratingName, gratingsName, centralWavelength, detectorPixels, spectralBinning);
        System.out.println("######## keySet ");
        System.out.println(data.keySet());
        data.keySet();
        //for (Iterator it = data.iterator();  it.hasNext();)
        //   System.out.println(it);

    }

    public double getStart() {
        return get_trans().getStart();
    }

    public double getEnd() {
        return get_trans().getEnd();
    }
}
