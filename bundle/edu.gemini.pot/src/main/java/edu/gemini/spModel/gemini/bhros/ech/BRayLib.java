package edu.gemini.spModel.gemini.bhros.ech;


/**
 * This class is derived from C library bRayLub.c shipped with bHROS, which was
 * itself derived from Matlab libraries which are Copyright R. G. Bingham 2001.
 * <p>
 * This class is optimized for performance and does a number of Bad Things in
 * the interest of speed. Most significantly, it re-uses matrices between method
 * calls, in effect mimicing static variables in C.
 * <p>
 * The class also is entirely static and maintains variable and function
 * identifiers from the C code wherever possible, to make it a little 
 * easier to refer back to the original code if required.
 */
public final class BRayLib implements HROSHardwareConstants {

	/* Angle CCD rotated in its own plane */
	public static final double CCDROT = -1.593;

	/* Maximum allowable number of iterations */
	private static final int MAXITER = 40;

	private static final double PI_DIV_180 = Math.PI / 180.0;
	private static final double PI_DIV_2 = Math.PI / 2.0;

	/**
	 * Computes direction cosines in the collimated beam for a given object
	 * position on the slit.
	 * <p>
	 * Adapted from Matlab code CopyrightR. G. Bingham October 2001.
	 * <p>
	 * @param goni_r
	 *            the off-centre distance along the slit. The sign is
	 *            significant. goni_r will normally be -2, 0 or 2 (mm).
	 * @param goni_ang
	 *            is the rotation angle of the goniometer measured from a
	 *            reference where the slit is horizontal with respect to the
	 *            optical bench. There is a "normal" position when the slit is
	 *            in the plane of the cross-dispersion and in this case, the
	 *            goniometer angle will read -12 degrees. (The routine
	 *            internally adds 12 degrees to this, in order to refer the
	 *            angle to the dispersion plane of the prisms and the off-axis
	 *            direction of the collimator.) goni_ang is in degrees and will
	 *            be between -20 and 20. The direction which is towards the
	 *            echelle is positive. This also gives a positive value for the
	 *            y image position on the CCD.
	 * @paran coll_output (out) consists of three direction cosines in a column
	 *        vector.
	 */
	private static void coll(final double goni_r, final double goni_ang, Matrix out) {

		double fcoll = 2800.0; // mm.
		double H = Math.atan(goni_r / fcoll); // field angle in coll.
		double G = (goni_ang + 12.0) * PI_DIV_180; // rotation of goniometer

		// Direction cosines of collimated beam
		// Negative image height gives positive direction cosines.
		double ycos = Math.sin(H) * Math.cos(G);
		double zcos = Math.cos(H);
		double x = Math.asin(Math.sin(H) * Math.sin(G));
		double xcos = Math.cos(PI_DIV_2 + x);
		out.set3(-xcos, -ycos, zcos);
		
	}

	/*
	 * Copyright R. G. Bingham October 2001. Malitson's formula for the
	 * refractive index of fused silic at 20 degrees C. See JOSA 55, 1205, 1965.
	 * wavelength is in microns. Results have been checked by comparison with
	 * Zemax.
	 */
	private static double malitson_silica(double wavelength) {
		double w2, n2_1;
		w2 = wavelength * wavelength;
		n2_1 = 0.6961663 * w2 / (w2 - 0.0684043 * 0.0684043) + 0.4079426 * w2 / (w2 - 0.1162414 * 0.1162414) + 0.8974794 * w2
				/ (w2 - 9.896161 * 9.896161);
		return (Math.sqrt(n2_1 + 1.0));
	}

	public static double interp(double x1, double y1, double x2, double y2, double x) {
		return (x1 == x2) ? y1 : (y1 + (y2 - y1) / (x2 - x1) * (x - x1));
	}

	static final Matrix xy = new Matrix(2, 1);

	private static void position(Matrix coll_output, Matrix system, int rows, int order, double wavelength, double ech_alt,
			double ech_az) {
		double refr_index = malitson_silica(wavelength);
		raytrace(coll_output, system, rows, order, wavelength, refr_index, ech_alt, ech_az, xy);
		rotate(xy, camera_output);
	}

	private static interface raytrace_statics {
		Matrix ray = new Matrix(6, 1);
		Matrix srow = new Matrix(1, 7);
	}

	private static void raytrace(Matrix coll_output, Matrix system, int rows, int order, double wavelength, double refr_index,
			double ech_alt, double ech_az, Matrix xy) {

		Matrix ray = raytrace_statics.ray;
		Matrix srow = raytrace_statics.srow;

		ray.set6(0, 0, 0, coll_output.cell(0, 0), coll_output.cell(1, 0), coll_output.cell(2, 0));

		for (int srow_num = 0; srow_num < rows; srow_num++) {
			srow.copyRowFrom(system, srow_num, 0);
			spacemove(ray, srow, order, wavelength, refr_index, ech_alt, ech_az, ray);
		}

		xy.set2(ray.cell(0, 0), ray.cell(1, 0));

	}

	private static interface rotate_statics {
		Matrix rotmatrix = new Matrix(2, 2);
	}

	static {
		double rotdeg = CCDROT;
		double rotrad = rotdeg * PI_DIV_180;
		double cr = Math.cos(rotrad);
		double sr = Math.sin(rotrad);
		rotate_statics.rotmatrix.set4(cr, -sr, sr, cr);
	}

	private static void rotate(Matrix xy, Matrix newxy) {
		// Matrix rotmatrix = rotate_statics.rotmatrix;
		rotate_statics.rotmatrix.multiplyInto(xy, newxy);
	}

	private static interface spacemove_statics {
		Matrix ray1 = new Matrix(6, 1);
		Matrix ray2 = new Matrix(6, 1);
		Matrix ray2a = new Matrix(6, 1);
		Matrix ray3 = new Matrix(6, 1);
		Matrix ray4 = new Matrix(6, 1);
		Matrix ray5 = new Matrix(6, 1);
		Matrix ray6 = new Matrix(6, 1);
		Matrix raylmn = new Matrix(3, 1);
		Matrix inter1 = new Matrix(3, 1);
		Matrix surface_normal = new Matrix(3, 1);
	}

	static void spacemove(Matrix rayin, Matrix srow, int order, double wavelength, double refr_index, double ech_alt,
			double ech_az, Matrix rayout) {
		double d, curve, index_ratio, srow1, srow6;

		Matrix ray1 = spacemove_statics.ray1;
		Matrix ray2 = spacemove_statics.ray2;
		Matrix ray2a = spacemove_statics.ray2a;
		Matrix ray3 = spacemove_statics.ray3;
		Matrix ray4 = spacemove_statics.ray4;
		Matrix ray5 = spacemove_statics.ray5;
		Matrix ray6 = spacemove_statics.ray6;
		Matrix raylmn = spacemove_statics.raylmn;
		Matrix inter1 = spacemove_statics.inter1;
		Matrix surface_normal = spacemove_statics.surface_normal;

		/* ray1 = flat_transfer(rayin,srow(2)); % go to first surface xy plane */
		d = srow.cell(0, 1);
		flat_transfer(rayin, d, ray1);

		/*
		 * ray2 = rotate_axes(ray1,(pi/180)*[-srow(3); 0; 0]); % rotate axes to
		 * tangent plane
		 */
		inter1.set3(-srow.cell(0, 2), 0.0, 0.0);
		inter1.scale(PI_DIV_180);
		rotate_axes(ray1, inter1, ray2);

		/* ray2a = flat_transfer(ray2,-ray2(3)); % transfer ray to tangent plane */
		d = -1.0 * ray2.cell(2, 0);
		flat_transfer(ray2, d, ray2a);

		/*
		 * if srow(4)~=0; curve = 1/srow(4); else curve = 0; end % ~0 means
		 * not=0
		 */
		double foo = srow.cell(0, 3);
		if (foo != 0) {
			curve = 1.0 / foo;
		} else {
			curve = 0.0;
		}

		/*
		 * [ray3, surface_normal] = flat_to_curve(ray2a,curve); % transfer ray
		 * to curve
		 */
		flat_to_curve(ray2a, curve, ray3, surface_normal);

		/*
		 * index_ratio is the second refractive index if the first is 1 or is -1
		 * if the two srow values have opp. signs index_ratio =
		 * (srow(6)*refr_index -abs(srow(6)) +1)/(srow(1)*refr_index
		 * -abs(srow(1)) +1);
		 */
		srow1 = srow.cell(0, 0);
		srow6 = srow.cell(0, 5);
		// assert (((srow1 * refr_index) - Math.abs(srow1) + 1) != 0.0);
		index_ratio = ((srow6 * refr_index) - Math.abs(srow6) + 1) / ((srow1 * refr_index) - Math.abs(srow1) + 1);

		// inter1.set3(ray3.cell(3, 0), ray3.cell(4, 0), ray3.cell(5, 0));
		inter1.set(ray3, 3);

		if (srow.cell(0, 6) == 0) {

			/* raylmn = refract(ray3(4:6), surface_normal, index_ratio); */
			refract(inter1, surface_normal, index_ratio, raylmn);

			/*
			 * ray4 = [ray3(1:3); raylmn]; % use refraction. Still in surface
			 * coords.
			 */
			// if (ray4 == null) ray4 = new Matrix(6,1);
			ray4.set6(ray3.cell(0, 0), ray3.cell(1, 0), ray3.cell(2, 0), raylmn.cell(0, 0), raylmn.cell(1, 0), raylmn.cell(2, 0));

			/*
			 * ray5 = flat_transfer(ray4,-ray4(3)); % Step back to tangent plane
			 * in surface coords
			 */
			d = -1.0 * ray4.cell(2, 0);
			flat_transfer(ray4, d, ray5);

			/*
			 * ray6 = derotate_axes(ray5,(pi/180)*[-srow(5); 0; 0]); % rotate
			 * axes to the output xy plane
			 */
			inter1.set3(-srow.cell(0, 4), 0.0, 0.0);
			inter1.scale(PI_DIV_180);
			derotate_axes(ray5, inter1, ray6);
			/*
			 * rayout = flat_transfer(ray6,-ray6(3)); % transfer ray to back to
			 * the output xy plane
			 */
			flat_transfer(ray6, -1.0 * ray6.cell(2, 0), rayout);

		} else {

			/* raylmn = echelle(ray3(4:6), order, wavelength, ech_az, ech_alt); */
			echelle(inter1, order, wavelength, ech_az, ech_alt, raylmn);

			/* rayout = [ray3(1:2); 0; raylmn]; */

			rayout.set6(ray3.cell(0, 0), ray3.cell(1, 0), 0.0, raylmn.cell(0, 0), raylmn.cell(1, 0), raylmn.cell(2, 0));

		}
	}

	static Matrix shift = new Matrix(6, 1);

	static void flat_transfer(Matrix rayin, double d, Matrix ray) {
		double L = rayin.cell(3, 0);
		double M = rayin.cell(4, 0);
		double N = rayin.cell(5, 0);

		shift.set(0, 0, d * L / N);
		shift.set(1, 0, d * M / N);

		rayin.addInto(shift, ray);
		ray.set(2, 0, 0.0);
	}

	static void flat_to_curve(Matrix rayin, double curve, Matrix ray, Matrix surface_normal) {

		double x0 = rayin.cell(0, 0);
		double y0 = rayin.cell(1, 0);
		double L = rayin.cell(3, 0);
		double M = rayin.cell(4, 0);
		double N = rayin.cell(5, 0);
		double F = curve * (x0 * x0 + y0 * y0);
		double G = N - curve * (L * x0 + M * y0);
		double denom = (G + Math.sqrt(G * G - curve * F));
		double delta = F / denom;
		double x = x0 + L * delta;
		double y = y0 + M * delta;
		double z = N * delta;

		ray.set6(x, y, z, L, M, N);
		surface_normal.set3(-curve * x, -curve * y, 1 - (curve * z));

	}

	private interface refract_statics {
		Matrix inter1 = new Matrix(3, 1);
		Matrix inter2 = new Matrix(3, 1);
	}

	static void refract(Matrix rayin, Matrix surface_normal, double index_ratio, Matrix ray) {

		double tsum = rayin.cell(0, 0) * surface_normal.cell(0, 0) + (rayin.cell(1, 0) * surface_normal.cell(1, 0));
		double cos_incidence = tsum + (rayin.cell(2, 0) * surface_normal.cell(2, 0));
		double incidence = Math.acos(cos_incidence);

		Matrix inter1 = refract_statics.inter1;
		Matrix inter2 = refract_statics.inter2;

		// if index_ratio ~= -1 (i.e., if not a mirror)
		if (Math.abs(index_ratio + 1.0) > 1e-6) {

			double refraction = Math.asin(Math.sin(incidence) / index_ratio);
			double k = index_ratio * Math.cos(refraction) - cos_incidence; 

			inter1.set(surface_normal);
			inter1.scale(k);
			inter1.addInto(rayin, inter2);

			ray.set(inter2);
			ray.scale(1.0 / index_ratio);

		} else {

			inter1.set(surface_normal);
			inter1.scale(2.0 * cos_incidence);

			rayin.negate(); 

			inter1.addInto(rayin, ray);
			
		}
	}

	private static interface echelle_statics {
		Matrix ROT1 = new Matrix(3, 3);
		Matrix inc_ray = new Matrix(3, 1);
		Matrix pre_ray = new Matrix(3, 1);
		Matrix rots = new Matrix(3, 1);
		Matrix ray2 = new Matrix(3, 1);
	}

	static {
		double coll_cam_deg = 12.0;
		double coll_cam_rad = coll_cam_deg * PI_DIV_180;
		double sinccr = Math.sin(coll_cam_rad);
		double cosccr = Math.cos(coll_cam_rad);
		echelle_statics.ROT1.set(0, 0, cosccr);
		echelle_statics.ROT1.set(0, 1, 0.0);
		echelle_statics.ROT1.set(0, 2, sinccr);
		echelle_statics.ROT1.set(1, 0, 0.0);
		echelle_statics.ROT1.set(1, 1, 1.0);
		echelle_statics.ROT1.set(1, 2, 0.0);
		echelle_statics.ROT1.set(2, 0, -sinccr);
		echelle_statics.ROT1.set(2, 1, 0.0);
		echelle_statics.ROT1.set(2, 2, cosccr);
	}

	static void echelle(final Matrix input_ray, final int order, final double wavelength, final double ech_az,
			final double ech_alt, final Matrix ray_out) {

		Matrix ROT1 = echelle_statics.ROT1;
		Matrix inc_ray = echelle_statics.inc_ray;
		Matrix pre_ray = echelle_statics.pre_ray;
		Matrix rots = echelle_statics.rots;
		Matrix ray2 = echelle_statics.ray2;

		double ruling = 0.087; /* lines per micron. */
		double wgrat = -order * wavelength * ruling;

		/*
		 * Rotate axes by 12 degrees around y to refer incoming rays to camera
		 * axis
		 */
		ROT1.multiplyInto(input_ray, inc_ray);

		// Used in functions rot_in and rot_out. rots = pi*[-ech_az; -ech_alt; -90]/180;
		rots.set3(-ech_az, -ech_alt, -90.0);
		rots.scale(PI_DIV_180);

		/*
		 * Applies the rotation matrices (rots) to the incoming ray. Output
		 * consists of three direction cosines ray_in(1 to 3):
		 */
		rot_in(rots, inc_ray, pre_ray);

		ray2.zero(); 

		
		// Grating equation.
		ray2.set3(-1.0 * pre_ray.cell(0, 0), (-1.0 * pre_ray.cell(1, 0)) + wgrat, Math.sqrt((pre_ray.cell(2, 0) * pre_ray
				.cell(2, 0))
				+ (2.0 * pre_ray.cell(1, 0) * wgrat) - (wgrat * wgrat)));

		/*
		 * Undo rotations out of grating coords into bench coords ray_out =
		 * rot_out(-rots, ray2);
		 */
		rots.negate(); 
		rot_out(rots, ray2, ray_out);

	}

	private static interface rot_in_statics {
		Matrix LL = new Matrix(3, 3);
		Matrix MM = new Matrix(3, 3);
		Matrix NN = new Matrix(3, 3);
		Matrix inter1 = new Matrix(3, 1);
		Matrix inter2 = new Matrix(3, 1); /* Intermediate 3-vector results */
	}

	static void rot_in(Matrix rots, Matrix inc_ray, Matrix ray_in) {
		Matrix LL = rot_in_statics.LL; 
		Matrix MM = rot_in_statics.MM; 
		Matrix NN = rot_in_statics.NN; 
		Matrix inter1 = rot_in_statics.inter1;
		Matrix inter2 = rot_in_statics.inter2;

		form_matrix(rots, LL, MM, NN);

		LL.multiplyInto(inc_ray, inter1);
		MM.multiplyInto(inter1, inter2);
		NN.multiplyInto(inter2, ray_in);
	}

	private interface rot_out_statics {
		Matrix LL = new Matrix(3, 3);
		Matrix MM = new Matrix(3, 3);
		Matrix NN = new Matrix(3, 3);
		Matrix inter1 = new Matrix(3, 1);
		Matrix inter2 = new Matrix(3, 1); /* Intermediate 3-vector results */
	}

	static void rot_out(final Matrix rots, final Matrix ray2, Matrix ray_out) {
		Matrix LL = rot_out_statics.LL;
		Matrix MM = rot_out_statics.MM; 
		Matrix NN = rot_out_statics.NN; 
		Matrix inter1 = rot_out_statics.inter1;
		Matrix inter2 = rot_out_statics.inter2; 

		form_matrix(rots, LL, MM, NN);

		NN.multiplyInto(ray2, inter1);
		MM.multiplyInto(inter1, inter2);
		LL.multiplyInto(inter2, ray_out);
	}

	static interface rotate_axes_statics {
		Matrix LL = new Matrix(3, 3);
		Matrix MM = new Matrix(3, 3);
		Matrix NN = new Matrix(3, 3);
		Matrix rayin_a = new Matrix(3, 1);
		Matrix rayin_b = new Matrix(3, 1);
		Matrix inter1 = new Matrix(3, 1);
		Matrix inter2 = new Matrix(3, 1); 
		Matrix inter3 = new Matrix(3, 1);
	}

	static void rotate_axes(Matrix rayin, Matrix rots, Matrix ray) {
		Matrix LL = rotate_axes_statics.LL;
		Matrix MM = rotate_axes_statics.MM;
		Matrix NN = rotate_axes_statics.NN;
		
		form_matrix(rots, LL, MM, NN);

		Matrix rayin_a = rotate_axes_statics.rayin_a; 
		Matrix rayin_b = rotate_axes_statics.rayin_b; 
		Matrix inter1 = rotate_axes_statics.inter1;
		Matrix inter2 = rotate_axes_statics.inter2;
		Matrix inter3 = rotate_axes_statics.inter3;

		rayin_a.set(rayin, 0);
		rayin_b.set(rayin, 3);

		LL.multiplyInto(rayin_a, inter1);
		MM.multiplyInto(inter1, inter2);
		NN.multiplyInto(inter2, inter3);

		ray.set(0, 0, inter3.cell(0, 0));
		ray.set(1, 0, inter3.cell(1, 0));
		ray.set(2, 0, inter3.cell(2, 0));

		LL.multiplyInto(rayin_b, inter1);
		MM.multiplyInto(inter1, inter2);
		NN.multiplyInto(inter2, inter3);

		ray.set(3, 0, inter3.cell(0, 0));
		ray.set(4, 0, inter3.cell(1, 0));
		ray.set(5, 0, inter3.cell(2, 0));
	}

	static interface derotate_axes_statics {

		Matrix LL = new Matrix(3, 3);
		Matrix MM = new Matrix(3, 3);
		Matrix NN = new Matrix(3, 3);
		Matrix rayin_a = new Matrix(3, 1); /* rayin_a = rayin(1:3) */
		Matrix rayin_b = new Matrix(3, 1); /* rayin_b = rayin(4:6) */
		Matrix inter1 = new Matrix(3, 1);
		Matrix inter2 = new Matrix(3, 1); /* Intermediate 3-vector results */
		Matrix inter3 = new Matrix(3, 1);
	}

	static int derotate_axes(Matrix rayin, Matrix rots, Matrix ray) {
		Matrix LL = rotate_axes_statics.LL;
		Matrix MM = rotate_axes_statics.MM;
		Matrix NN = rotate_axes_statics.NN;
		
		form_matrix(rots, LL, MM, NN);

		Matrix rayin_a = rotate_axes_statics.rayin_a; 
		Matrix rayin_b = rotate_axes_statics.rayin_b; 
		Matrix inter1 = rotate_axes_statics.inter1;
		Matrix inter2 = rotate_axes_statics.inter2;
		Matrix inter3 = rotate_axes_statics.inter3;

		rayin_a.set(rayin, 0);
		rayin_b.set(rayin, 3);

		NN.multiplyInto(rayin_a, inter1);
		MM.multiplyInto(inter1, inter2);
		LL.multiplyInto(inter2, inter3);

		ray.set(0, 0, inter3.cell(0, 0));
		ray.set(1, 0, inter3.cell(1, 0));
		ray.set(2, 0, inter3.cell(2, 0));

		NN.multiplyInto(rayin_b, inter1);
		MM.multiplyInto(inter1, inter2);
		LL.multiplyInto(inter2, inter3);

		ray.set(3, 0, inter3.cell(0, 0));
		ray.set(4, 0, inter3.cell(1, 0));
		ray.set(5, 0, inter3.cell(2, 0));

		return (0);
	}

	private interface form_matrix_statics {

		Matrix sins = new Matrix(3, 1);
		Matrix coss = new Matrix(3, 1);
	}

	static void form_matrix(Matrix rots, Matrix LL, Matrix MM, Matrix NN) {
		Matrix sins = form_matrix_statics.sins;
		Matrix coss = form_matrix_statics.coss;

		sins.set3(Math.sin(rots.cell(0, 0)), Math.sin(rots.cell(1, 0)), Math.sin(rots.cell(2, 0)));
		coss.set3(Math.cos(rots.cell(0, 0)), Math.cos(rots.cell(1, 0)), Math.cos(rots.cell(2, 0)));

		LL.set(0, 0, 1.0);
		LL.set(0, 1, 0.0);
		LL.set(0, 2, 0.0);
		LL.set(1, 0, 0.0);
		LL.set(1, 1, coss.cell(0, 0));
		LL.set(1, 2, -1.0 * sins.cell(0, 0));
		LL.set(2, 0, 0.0);
		LL.set(2, 1, sins.cell(0, 0));
		LL.set(2, 2, coss.cell(0, 0));

		MM.set(0, 0, coss.cell(1, 0));
		MM.set(0, 1, 0.0);
		MM.set(0, 2, sins.cell(1, 0));
		MM.set(1, 0, 0.0);
		MM.set(1, 1, 1.0);
		MM.set(1, 2, 0.0);
		MM.set(2, 0, -1.0 * sins.cell(1, 0));
		MM.set(2, 1, 0.0);
		MM.set(2, 2, coss.cell(1, 0));

		NN.set(0, 0, coss.cell(2, 0));
		NN.set(0, 1, -1.0 * sins.cell(2, 0));
		NN.set(0, 2, 0.0);
		NN.set(1, 0, sins.cell(2, 0));
		NN.set(1, 1, coss.cell(2, 0));
		NN.set(1, 2, 0.0);
		NN.set(2, 0, 0.0);
		NN.set(2, 1, 0.0);
		NN.set(2, 2, 1.0);

	}

	private static Matrix camera_output = new Matrix(2, 1), coll_output = new Matrix(3, 1);;
	private static Matrix vec = new Matrix(2, 1), temp = new Matrix(2, 1);;

	/**
	 * Does something complicated.
	 * <p>
	 * Wavelength (nominal range 0.3 to 1.0), echelle data (order and angles in
	 * degrees) and the real slit rotation (the goniometer angle goni_ang in
	 * degrees).
	 * <p>
	 * (Spectrograph dimensions and angles are in the data file SYSTEM.dat.)
	 * <p>
	 * If flag is not=0, the routine computes x,y values on the CCD
	 * corresponding to three points along the f/16 entrance slit (centre and
	 * ends). From these x,y values, it also computes the rotation of the slit
	 * image.
	 * <p>
	 * If flag=0, it computes only one pair of x,y values, for the centre of the
	 * entrance slit. In that case it still outputs two more sets of x,y values
	 * and an angle but they will be zero.
	 * <p>
	 * The algorithms use a nominal collimator and ray-trace the cross-disperser
	 * prisms, echelle and camera.
	 * <p>
	 * The six input parameters are all scalars. They are in order: flag,
	 * goni_ang, order, wavelength, ech_alt, ech_az.
	 * <p>
	 * The outputs image_mid, image_top & image_bot are all vectors with two
	 * elements (x and y). image_ang is a scalar (degrees).
	 */
	public static double b_ray2(Matrix systemDat, int rot_flag, double goni_ang, int order, double wavelength, double ech_alt,
			double ech_az, Matrix ImageMid, Matrix ImageTop, Matrix ImageBot) {

		double hilim, lowlim, Image_ang, goni_r, vec1, vec2;

		ImageMid.zero();
		ImageTop.zero();
		ImageBot.zero();

		Image_ang = 0.0;

		/*
		 * Compute the image positions The position along the f/16 slit in mm,
		 * which straddles zero, measured from the centre of the slit. See
		 * collimator routine for more conventions. Normally -2, 2 & 2 (mm). The
		 * outer two are used to find the apparent slit rotation.
		 */
		if (rot_flag == 0) {
			lowlim = 0.0;
			hilim = 0.001;
		} else {
			lowlim = -2.0;
			hilim = 2.001;
		}

		for (goni_r = lowlim; goni_r <= hilim; goni_r = goni_r + 2.0) {

			/* Compute the image position.
			 * coll_output has 3 elements (direction cosines) in a column vector
			 * that will be the input to the raytrace through the prisms.
			 */
			coll(goni_r, goni_ang, coll_output);


			// modifieds camera_output
			position(coll_output, systemDat, nrows, order, wavelength, ech_alt, ech_az);

			/*
			 * camera_output has 2 elements x and y (mm). These are positions on
			 * the CCD.
			 */
			if (goni_r < -0.0001) {
				ImageBot.set(camera_output);
			} else if (goni_r > 0.0001) {
				ImageTop.set(camera_output);
			} else {
				ImageMid.set(camera_output);
			}
		}

		if (rot_flag != 0) {
			ImageBot.negate(); // scale(-1.0);
			temp.set(ImageBot); // matScale(*ImageBot, -1.0, temp);
			ImageTop.addInto(temp, vec); // matAdd(*ImageTop, temp, vec);
			vec1 = vec.cell(0, 0);
			vec2 = vec.cell(1, 0);

			Image_ang = Math.atan2(vec1, vec2);
			Image_ang = 180.0 * Image_ang / Math.PI;

			/*
			 * if Image_ang is > 90 degrees, subtract 180 degrees to bring into
			 * range -90 to +90. (rounded towards zero.)
			 */

			// get the signed fractional part of the image angle / 90
			double iaRound = Image_ang / 90.0 % 1.0; // t =
			Image_ang = Image_ang - (180.0 * iaRound);
		}

		return Image_ang;

	}

	/* Target accuracy of position on CCD for iteration */
	public static final double pixelPrec = (2 * PIXELSIZE) / 1000.0;

	/* Target accuracy (degs.) of goniometer angle */
	public static final double anglePrec = 0.02;

	private static final int nrows = 11;
	private static Matrix systemDat = new Matrix(nrows, 7);
	static {
		systemDat.read(BRayLib.class.getResourceAsStream("/resources/conf/system.dat"));
	}

	private static Matrix image_mid = new Matrix(2, 1);
	private static Matrix image_top = new Matrix(2, 1);
	private static Matrix image_bot = new Matrix(2, 1);

	public static double[] echellePos(double wavelength, int order, double xTarget, double yTarget, double goniOffset) {

//		System.out.println("echellePos: " + wavelength + ", " + order + ", " + xTarget + ", " + ", " + yTarget + " ...");

		double image_ang;
		double altResult;
		double goniAngResult = 0;
		double xpos = 1.0e9, ypos = 1.0e9;
		double xposA = 1.0e9, xposB = 1.0e9, altA = 1.0e9, altB = 1.0e9;
		double azCent, azBand;
		double yposA = 1.0e9, yposB = 1.0e9, azA = 1.0e9, azB = 1.0e9;
		double imAngA = 1.0e9, imAngB = 1.0e9;
		double goniBand = 1.0e9, goniCent = 1.0e9, goniAngA = 1.0e9, goniAngB = 1.0e9;
		double chip_xsize = CHIP_XSIZE;

		/* Basic validation of input values */
		if (wavelength > WAVELENMAX / 10000.0 || wavelength < WAVELENMIN / 10000.0)
			throw new IllegalArgumentException("Requested wavelength out of range: " + wavelength);

		if (order < 10 || order > 70)
			throw new IllegalArgumentException("Order out of range: " + order);

		
//		if (Math.abs(xTarget) > (chip_xsize / 2.0) || (yTarget > RED_CHIP_YSIZE) || (yTarget < -BLUE_CHIP_YSIZE))

// RCN: need to turn this off for engineering		
//		if (Math.abs(xTarget) > (chip_xsize / 2.0) || (yTarget > Y_MAX) || (yTarget < -Y_MIN))
//			throw new IllegalArgumentException("Requested X,Y position is off-chip.");

		/* Initial assumed values for ech_az & goni_ang */
		double azResult = 0.0;
		double goni_ang = 0.0;
		int iter;
		do {

			/* Initialize central altitude and band for iterations */
			double altCent = ECHALT_ZERO;
			double altBand = ECHALT_RANGE + ((47 - order) * 0.03);
			iter = 0;

			// Iterate using converging linear interpolation, reducing the
			// range by a factor of 20 each time round
			do {
				iter++;
				altA = altCent - altBand / 2.0;
				altB = altCent + altBand / 2.0;

				image_ang = b_ray2(systemDat, 0, goni_ang, order, wavelength, altA, azResult, image_mid, image_top, image_bot);

				xposA = image_mid.cell(0, 0);

				image_ang = b_ray2(systemDat, 0, goni_ang, order, wavelength, altB, azResult, image_mid, image_top, image_bot);

				xposB = image_mid.cell(0, 0);
				altResult = interp(xposA, altA, xposB, altB, xTarget);

				if (altResult < altA || altResult > altB)
					throw new IllegalArgumentException("Target X iteration range error");

				altBand = altBand / (20.0 + ((order - 68) * 0.1));
				altCent = altResult;

				/*
				 * Compute final X value using interpolated alt value
				 * (altResult)
				 */
				image_ang = b_ray2(systemDat, 0, goni_ang, order, wavelength, altResult, azResult, image_mid, image_top, image_bot);

				xpos = image_mid.cell(0, 0);

			} while ((Math.abs(xpos - xTarget)) > pixelPrec && iter <= MAXITER);

			if (iter > MAXITER)
				throw new IllegalStateException("Exceeded " + MAXITER + " iterations finding alt value");

			/*
			 * Iterate using converging linear interpolation, reducing the range
			 * by a factor of 20 each time round
			 */

			/* Initialize central azimuth and band for iterating in */
			azCent = ECHAZ_ZERO;
			azBand = ECHAZ_RANGE;
			iter = 0;
			do {
				iter++;
				azA = azCent - azBand / 2.0;
				azB = azCent + azBand / 2.0;
				image_ang = b_ray2(systemDat, 0, goni_ang, order, wavelength, altResult, azA, image_mid, image_top, image_bot);

				yposA = image_mid.cell(1, 0);

				image_ang = b_ray2(systemDat, 0, goni_ang, order, wavelength, altResult, azB, image_mid, image_top, image_bot);

				yposB = image_mid.cell(1, 0);
				azResult = interp(yposA, azA, yposB, azB, yTarget);

				if (azResult < azA || azResult > azB)
					throw new IllegalArgumentException("Target Y iteration range error");

				azBand = azBand / 20.0;
				azCent = azResult;

				image_ang = b_ray2(systemDat, 0, goni_ang, order, wavelength, altResult, azResult, image_mid, image_top, image_bot);

				ypos = image_mid.cell(1, 0);

			} while ((Math.abs(ypos - yTarget)) > pixelPrec && iter <= MAXITER);

			if (iter > MAXITER)
				throw new IllegalStateException("Exceeded " + MAXITER + "iterations finding az value");

			goniCent = IS_ROT_ZERO; 
			goniBand = 2.0 * IS_ROT_RANGE;
			iter = 0;

			/*
			 * Iterate using converging linear interpolation, reducing the range
			 * by a factor of 20 each time round
			 */
			do {
				iter++;
				
				goniAngA = goniCent - goniBand / 2.0;
				goniAngB = goniCent + goniBand / 2.0;
				
				imAngA = b_ray2(systemDat, 1, goniAngA, order, wavelength, altResult, azResult, image_mid, image_top, image_bot);
				imAngB = b_ray2(systemDat, 1, goniAngB, order, wavelength, altResult, azResult, image_mid, image_top, image_bot);

				goniAngResult = interp(imAngA, goniAngA, imAngB, goniAngB, 0.0);

				/* Target value not found in range - error */
				if (goniAngResult < goniAngA || goniAngResult > goniAngB)
					throw new IllegalArgumentException("Target Ang value (0.0) not in range " + imAngA + " to " + imAngB);

				goniBand = goniBand / 15.0;
				goniCent = goniAngResult;

				image_ang = b_ray2(systemDat, 1, goniAngResult, order, wavelength, altResult, azResult, image_mid, image_top, image_bot);
				
			} while (Math.abs(image_ang) > anglePrec && iter <= MAXITER);

			if (iter > MAXITER)
				throw new IllegalStateException("echellePos: Exceeded " + MAXITER + " iterations finding angle value");

			/* For terminal 'do-while' test, get latest X and Y positions */
			xpos = image_mid.cell(0, 0);
			ypos = image_mid.cell(1, 0);

		} while (((Math.abs(xpos - xTarget)) > pixelPrec) || ((Math.abs(ypos - yTarget)) > pixelPrec));

		return new double[] { altResult, azResult, goniAngResult + goniOffset };

	}
	
	/* ==================================================================================== */
	/*+
	*   Function name:
	*   Xwavelength
	*
	*   Description:
	*   This function returns the wavelength corresponding to a specified
	*   X position on the CCD, given the Echelle Alt and Az and
	*   Goniometer angles and the order
	*
	*   Invocation:
	*   status = Xwavelength(const int order, const double cenWavelength,
	*             const double xTarget, double altitude, double goniAng, 
	*             double *wavelength, double *xPos, char *errmess)
	*
	*   Parameters in:
	*      > order            int   Order containing wavelength
	*      > cenWavelength double   Current wavelength centred on CCD chip (microns)
	*      > xTarget       double   Central wavelength desired Y co-ordinate
	*      > altitude      double   Current Echelle Alt angle
	*      > azimuth       double   Current Echelle Az angle
	*	> goniAng       double   Current goniometer angle
	*
	*   Parameters out:
	*      < wavelength    double   Desired wavelength in microns
	*      < yPos          double   Y co-ordinate at this X position
	*	< errmess         char   Error message text
	*
	*   Return value:
	*      < status      long        Status value
	*
	*   Globals:
	*      External functions:
	*      None
	*
	*      External variables:
	*      None
	*
	*   Requirements:
	*
	*
	*   Author:
	*   Philip Taylor  (pbt@observatorysciences.co.uk)
	*
	*   History:
	*     31-Dec-2002: Original version.                          (pbt)
	*
	*-
	*/
	public static final void Xwavelength(final int order, final double cenWavelength, final double xTarget, double altitude,
			double azimuth, double goniAng, double[] wavelength, double[] yPos) {

		int iter;
//		int bray2Error = 0;
//		double image_ang;
		double xpos = 1.0E9, ypos = 1.0e9;
		double xposA = 1.0e9, xposB = 1.0e9;
		double lambdaA = 1.0e9, lambdaB = 1.0e9;
		double lambdaCent, lambdaBand, lambdaResult = 0;

		wavelength[0] = 0.0; /* Default return value */

		if (order < 10 || order > 70)
			throw new IllegalArgumentException("Order out of range (10 -> 70)");

		if (Math.abs(xTarget) > CHIP_XSIZE / 2.0)
			throw new IllegalArgumentException("Specified X position is off-chip");

		/* Initialize central wavelength value and band for X position iterations */
		lambdaCent = cenWavelength;
		lambdaBand = 0.02 * (70.0 / order);
		iter = 0;
		/*
		 Iterate using converging linear interpolation, reducing the
		 range by a factor of 5 each time round
		 */
		do {
			iter++;
			if (iter > MAXITER)
				break;
			lambdaA = lambdaCent - lambdaBand / 2.0;
			lambdaB = lambdaCent + lambdaBand / 2.0;
			/*image_ang = */b_ray2(systemDat, 0, goniAng, order, lambdaA, altitude, azimuth, image_mid, image_top, image_bot);

			xposA = image_mid.cell(0, 0);
			/*image_ang = */b_ray2(systemDat, 0, goniAng, order, lambdaB, altitude, azimuth, image_mid, image_top, image_bot);

			xposB = image_mid.cell(0, 0);
			lambdaResult = interp(xposA, lambdaA, xposB, lambdaB, xTarget);

			if (lambdaResult < lambdaA || lambdaResult > lambdaB)  /* Target wavelength not found in range - error */
				throw new IllegalArgumentException("Target X value " + xTarget + " not in range " + xposA + " to " + xpos);
			
			lambdaBand = lambdaBand / 5.0;
			lambdaCent = lambdaResult;

			/* Compute final Y position using interpolated wavelength value  */
			/*image_ang =*/ b_ray2(systemDat, 0, goniAng, order, lambdaResult, altitude, azimuth, image_mid, image_top, image_bot);
			xpos = image_mid.cell(0, 0);
		} while ((Math.abs(xpos - xTarget)) > pixelPrec && iter <= MAXITER);

		if (iter > MAXITER)
			throw new IllegalStateException("Xwavelength: Exceeded " + MAXITER + " iterations finding Y value");

		/* Get latest Y position */
		ypos = image_mid.cell(1, 0);

		if (ypos > RED_CHIP_YSIZE || ypos < -BLUE_CHIP_YSIZE || Math.abs(ypos) < CHIP_GAP / 2.0) {
			throw new IllegalArgumentException("Xwavelength: Y position off-chip");
		}

		wavelength[0] = lambdaResult;
		yPos[0] = ypos;

	}


}
