/*
 * A test application to demonstrate how to send zernikes corrections
 * to the PCS via the GIAPI
 *
 *  Created on: Dec 10, 2009
 *      Author: anunez
 */

#include <iostream>
#include <iomanip>

#include <giapi/GeminiUtil.h>

using namespace giapi;


int main(int argc, char **argv) {

  try {

    std::cout << "Starting GPI M1 Zernikes Simulator" << std::endl;

    double zernikes[20];
    // Values taken from an actual offload event
    zernikes[0]  =  0.1;
    zernikes[1]  =  0.2;
    zernikes[2]  =  0.3;
    zernikes[3]  =  1.4269236999098212E-4;
    zernikes[4]  = -1.173403870780021E-5;
    zernikes[5]  =  7.754975740681402E-6;
    zernikes[6]  =  5.736726052418817E-6;
    zernikes[7]  = -2.4758485324127832E-6;
    zernikes[8]  = -1.2555369721667375E-6;
    zernikes[9]  = -2.3237418645294383E-5;
    zernikes[10] = -2.1918547645327635E-5;
    zernikes[11] = -1.1538284070411464E-6;
    zernikes[12] =  1.753231231305108E-6;
    zernikes[13] = -7.1751132963981945E-6;
    zernikes[14] =  1.630298538657371E-5;
    zernikes[15] =  1.4524449397868011E-5;
    zernikes[16] =  2.3826235064916546E-6;
    zernikes[17] =  4.152916233124415E-7;
    zernikes[18] =  7.609909971506568E-6;
    zernikes[19] =  0.0;

    if (GeminiUtil::postPcsUpdate(zernikes, 20) == status::ERROR) {
      std::cout << "Can't post zernikes to the PCS..." << std::endl;
      return 0;
    }

  } catch (GiapiException &e) {
    std::cerr << "Is the GMP up?" << std::endl;
  }
  return 0;
}
