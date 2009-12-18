/*
 * EpicsHandlerDemo.cpp
 *
 *  Created on: Mar 24, 2009
 *      Author: anunez
 */

#include "EpicsHandlerDemo.h"
#include <iostream>
#include <list>
#include <giapi/giapi.h>

EpicsHandlerDemo::EpicsHandlerDemo() {

}

EpicsHandlerDemo::~EpicsHandlerDemo() {

}

pEpicsStatusHandler EpicsHandlerDemo::create() {
	pEpicsStatusHandler handler(new EpicsHandlerDemo());
	return handler;
}

void EpicsHandlerDemo::channelChanged(pEpicsStatusItem item) {
	std::cout << "Channel Changed " << item->getName() << std::endl;

	int nElements = item->getCount();


	if (item->getType() == type::DOUBLE) {
		for (int i = 0; i < nElements; i++) {
			std::cout << "Value [" << i << "] = " << item->getDataAsDouble(i) << std::endl;
		}

	} else if (item->getType() == type::INT) {
		for (int i = 0; i < nElements; i++) {
			std::cout << "Value [" << i << "] = " << item->getDataAsDouble(i) << std::endl;
		}

	} else if (item->getType() == type::STRING) {

		for (int i = 0; i < nElements; i++) {
			std::cout << "Value [" << i << "] = " << item->getDataAsString(i) << std::endl;
		}

	} else if (item->getType() == type::BYTE) {
		std::cout << "Value: {" ;
		for (int i = 0; i < nElements; i++) {
			std::cout << (int)item->getDataAsByte(i) << " ";
		}
		std::cout << "}" << std::endl;

	}



}
