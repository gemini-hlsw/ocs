package jsky.app.ot.gemini.bhros;

import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.bhros.BHROSParams.EntranceFibre;

import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Science area feature for bHROS. The science field has two modes, either a single 0.9 arcsec
 * square or dual 0.7 arcsec squares offset by 18 arcsec. Because they are so small, we draw
 * larger boxes around them.
 * @author rnorris
 */
public class BHROS_SciAreaFeature extends SciAreaFeatureBase {

	private static final double FOV_SIZE_SINGLE = 0.9;
	private static final double FOV_SIZE_DUAL = 0.7;
	private static final double FOV_ANNULUS_SIZE = 5.0;
	private static final double FOV_DUAL_X_SEPARATION = 12.83f;
	private static final double FOV_DUAL_Y_SEPARATION = 0.03f;

	private AffineTransform _trans;

	// The superclass provides a transform for the position angle, but that's it.
	// We build up the full transform by adding the base position translation and
	// scale factor and store it here.
	protected boolean _calc(TpeImageInfo tii)  {
		if (super._calc(tii)) {
			_trans = new AffineTransform();
			_trans.concatenate(_posAngleTrans);
			_trans.translate(_baseScreenPos.x, _baseScreenPos.y);
			_trans.scale(tii.getPixelsPerArcsec(), tii.getPixelsPerArcsec());
			return true;
		}
		return false;
	}

    private EntranceFibre getEntranceFibre() {
        InstBHROS inst = _iw.getContext().instrument().orNull(InstBHROS.SP_TYPE);
        return (inst == null) ? EntranceFibre.OBJECT_ONLY : inst.getEntranceFibre();
    }

	protected void _updateFigureList() {
		super._updateFigureList();
		EntranceFibre fibre = getEntranceFibre();
		GeneralPath gp = new GeneralPath();
		if (fibre == EntranceFibre.OBJECT_ONLY) {

			// Single
			Shape one = createCenteredSquare(FOV_SIZE_SINGLE);
			gp.append(one, false);

			// Plus annulus
			Shape an1 = createAnnulus(FOV_ANNULUS_SIZE, FOV_SIZE_SINGLE);
			gp.append(an1, false);

		} else if (fibre == EntranceFibre.OBJECT_SKY) {

			// Offset transform
			AffineTransform offsetTransform = AffineTransform.getTranslateInstance(FOV_DUAL_X_SEPARATION, FOV_DUAL_Y_SEPARATION);

			// Dual, one of them offset horizontally.
			Shape one = createCenteredSquare(FOV_SIZE_DUAL);
			Shape two = offsetTransform.createTransformedShape(one);
			gp.append(one, false);
			gp.append(two, false);

			// Plus annulus x 2
			Shape an1 = createAnnulus(FOV_ANNULUS_SIZE, FOV_SIZE_DUAL);
			Shape an2 = offsetTransform.createTransformedShape(an1);
			gp.append(an1, false);
			gp.append(an2, false);

		} else {
			throw new IllegalArgumentException("Can't deal with entrance fibre: " + fibre);
		}

		gp.transform(_trans);
		_figureList.add(gp);
	}

	// The tick mark (grab control for rotation) needs to be pushed outside the annulus,
	// otherwise it's very hard to see. So we just push it up.
    protected Point2D.Double _getTickMarkOffset() {
		AffineTransform trans = new AffineTransform();
		trans.translate(_baseScreenPos.x, _baseScreenPos.y);
		trans.scale(_tii.getPixelsPerArcsec(), _tii.getPixelsPerArcsec());
		Point2D.Double offset = new Point2D.Double(0.0, -FOV_ANNULUS_SIZE / 1.9); // a little more than a half
		return (Point2D.Double) trans.transform(offset, offset);
    }

	// The field is very small, so I think it's worth it to draw it as accurately as
	// possible, without jaggies. Could be wrong. I'm not sure it helps much.
	public void draw(Graphics g, TpeImageInfo tii) {
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		super.draw(g, tii);
	}

	private Shape createCenteredSquare(double side) {
		return new Rectangle2D.Double(-side / 2.0, -side / 2.0, side, side);
	}

	private Shape createAnnulus(double _side, double _centre) {
		GeneralPath gp = new GeneralPath();
		gp.append(createCenteredSquare(_side), false);
		float centre = (float) _centre, side = (float) _side;
		gp.moveTo(-side / 2, -side / 2); gp.lineTo(-centre / 2, -centre / 2);
		gp.moveTo(-side / 2, +side / 2); gp.lineTo(-centre / 2, +centre / 2);
		gp.moveTo(+side / 2, -side / 2); gp.lineTo(+centre / 2, -centre / 2);
		gp.moveTo(+side / 2, +side / 2); gp.lineTo(+centre / 2, +centre / 2);
		return gp;
	}

}
