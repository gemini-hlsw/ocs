package jsky.app.ot.gemini.bhros;

import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.bhros.BHROSParams.ISSPort;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsProbeArm;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;

import jsky.app.ot.gemini.gmos.GMOS_OIWFS_Feature;
import jsky.app.ot.tpe.TpeImageInfo;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * On-instrument wavefront sensor for bHROS. Because bHROS is really just a custom mask for GMOS,
 * it uses the same IOWFS. So we are subclassing here in a fairly poor manner, simply re-implementing
 * all of the methods in the superclass that downcast the instrument to GMOS.
 * TODO: refactor a common base class
 * @author rnorris
 */
public class BHROS_OIWFS_Feature extends GMOS_OIWFS_Feature {
	private ISSPort _port;
    private Double _patrolFieldXOffset;
    private Double _patrolFieldYOffset;

	private double _getPatrolFieldXOffset(InstBHROS inst) {
		return inst.getEntranceFibre().getPatrolFieldXOffset();
	}

	private double _getPatrolFieldYOffset(InstBHROS inst) {
		return inst.getEntranceFibre().getPatrolFieldYOffset();
	}

    /**
     * Update the list of figures to draw.
     *
     * @param guidePosX the X screen coordinate position for the OIWFS guide star
     * @param guidePosY the Y screen coordinate position for the OIWFS guide star
     * @param offsetPosX the X screen coordinate for the selected offset
     * @param offsetPosY the X screen coordinate for the selected offset
     * @param translateX translate resulting figure by this amount of pixels in X
     * @param translateY translate resulting figure by this amount of pixels in Y
     * @param basePosX the X screen coordinate for the base position (IGNORED)
     * @param basePosY the Y screen coordinate for the base position (IGNORED)
     * @param oiwfsDefined set to true if an OIWFS position is defined (otherwise
     *                     the xg and yg parameters are ignored)
     */
	protected void _updateFigureList(double guidePosX, double guidePosY,
									 double offsetPosX, double offsetPosY,
									 double translateX, double translateY,
									 double basePosX, double basePosY,
									 boolean oiwfsDefined) {
		final InstBHROS inst = _iw.getContext().instrument().orNull(InstBHROS.SP_TYPE);
        if (inst != null) {
            _port = inst.getISSPort();
            _patrolFieldXOffset = _getPatrolFieldXOffset(inst);
            _patrolFieldYOffset = _getPatrolFieldYOffset(inst);
            _figureList.clear();
            final boolean flip = (_port == ISSPort.SIDE_LOOKING);
            addOffsetConstrainedPatrolField(offsetPosX + translateX, offsetPosY + translateY);
            if (oiwfsDefined)
                _addProbeArm(guidePosX, guidePosY, offsetPosX, offsetPosY, translateX, translateY, flip);
        }
	}

	protected boolean _needsUpdate(SPInstObsComp inst, TpeImageInfo tii) {
		try {
			if (super._needsUpdate(inst, tii)) return true;
		} catch (ClassCastException cce) {
			// This is fairly poor, and may break in the future. The superclass does
			// a check of super() and then downcasts, which will always fail. But
			// in that case we can just keep going because we're replacing that code.
			// What I really want to do is call super.super._needsUpdate()
		}
		final InstBHROS instBHROS = (InstBHROS) inst;
		final double xOffset = _getPatrolFieldXOffset(instBHROS);
		final double yOffset = _getPatrolFieldYOffset(instBHROS);
		return (_patrolFieldXOffset != xOffset) || (_patrolFieldYOffset != yOffset) || (_port != instBHROS.getISSPort());
	}
}