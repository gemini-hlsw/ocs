/*
 * A test application to demonstrate how to get the TCS context
 * from Gemini.
 *
 *  Created on: Dec 10, 2009
 *      Author: anunez
 */

#include <iostream>
#include <iomanip>

#include <giapi/GeminiUtil.h>


using namespace giapi;


void showTcsContext(TcsContext &ctx) {
	int const FORMAT_WIDTH = 14;
	std::cout << "=================" << std::endl;
	std::cout << "Raw Time        : " << std::setw(FORMAT_WIDTH) << (long)ctx.time << std::endl;
	std::cout << "Mount Flexure X : " << std::setw(FORMAT_WIDTH) << ctx.x << std::endl;
	std::cout << "Mount Flexure Y : " << std::setw(FORMAT_WIDTH) << ctx.y << std::endl;
	std::cout << "Mount Flexure Z : " << std::setw(FORMAT_WIDTH) << ctx.z << std::endl;
	std::cout << "=================\nTelescope Parameters " << std::endl;
	std::cout << "Telescope focal length (mm)                   : " << std::setw(FORMAT_WIDTH) << ctx.tel.fl << std::endl;
	std::cout << "Rotator mechanical angle (rads)               : " << std::setw(FORMAT_WIDTH) << ctx.tel.rma << std::endl;
	std::cout << "Azimuth axis tilt NS (rads)                   : " << std::setw(FORMAT_WIDTH) << ctx.tel.an << std::endl;
	std::cout << "Azimuth axis tilt EW (rads)                   : " << std::setw(FORMAT_WIDTH) << ctx.tel.aw << std::endl;
	std::cout << "Az/El nonperpendicularity (rads)              : " << std::setw(FORMAT_WIDTH) << ctx.tel.pnpae << std::endl;
	std::cout << "Net left-right(horizontal) collimation (rads) : " << std::setw(FORMAT_WIDTH) << ctx.tel.ca << std::endl;
	std::cout << "Net up-down(vertical) collimation (rads)      : " << std::setw(FORMAT_WIDTH) << ctx.tel.ce << std::endl;
	std::cout << "Pointing origin x-component (mm)              : " << std::setw(FORMAT_WIDTH) << ctx.tel.pox << std::endl;
	std::cout << "Pointing origin y-component (mm)              : " << std::setw(FORMAT_WIDTH) << ctx.tel.poy << std::endl;

	std::cout << "=================\nApparent to Observed Parameters ";
	for (int i = 0; i < 15; i++) {
		if (i % 5 == 0) std::cout << std::endl;
		std::cout << std::setw(FORMAT_WIDTH) << ctx.aoprms[i] << " ";
	}
	std::cout << std::endl;
	std::cout << "=================\nM2 tip/tilt (3 chop states) " << std::endl;

	for (int x = 0; x < 3; x++) {
		for (int y = 0; y < 2; y++) {
			std::cout << std::setw(FORMAT_WIDTH) << ctx.m2xy[x][y] << " ";
		}
		std::cout << std::endl;
	}
	std::cout << "=================\nPoint Origins " << std::endl;
	std::cout << "Mount point origin in X            : " << std::setw(FORMAT_WIDTH) << ctx.po.mx << std::endl;
	std::cout << "Mount point origin in Y            : " << std::setw(FORMAT_WIDTH) << ctx.po.my << std::endl;
	std::cout << "Source chop A pointing origin in X : " << std::setw(FORMAT_WIDTH) << ctx.po.ax << std::endl;
	std::cout << "Source chop A pointing origin in Y : " << std::setw(FORMAT_WIDTH) << ctx.po.ay << std::endl;
	std::cout << "Source chop B pointing origin in X : " << std::setw(FORMAT_WIDTH) << ctx.po.bx << std::endl;
	std::cout << "Source chop B pointing origin in Y : " << std::setw(FORMAT_WIDTH) << ctx.po.by << std::endl;
	std::cout << "Source chop C pointing origin in X : " << std::setw(FORMAT_WIDTH) << ctx.po.cx << std::endl;
	std::cout << "Source chop C pointing origin in Y : " << std::setw(FORMAT_WIDTH) << ctx.po.cy << std::endl;

	std::cout << "=================\nOptical Distortions " << std::endl;
	for (int i = 0; i < 6; i++) {
		std::cout << std::setw(FORMAT_WIDTH / 2) << ctx.ao2t[i] << " ";
	}
	std::cout << std::endl;
}

int main(int argc, char **argv) {

	try {

		std::cout << "Starting TCS Context Example" << std::endl;

		TcsContext ctx;

		if (GeminiUtil::getTcsContext(ctx, 1000) == status::ERROR) {
			std::cout << "Can't get TCS Context..." << std::endl;
			return 0;
		}

		std::cout << "TCS Context: " << std::endl;
		showTcsContext(ctx);

	} catch (TimeoutException &e) {
		std::cout << "Timeout while trying to get TCS Context from Gemini " << std::endl;
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?" << std::endl;
	}
	return 0;
}
