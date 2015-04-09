/*
 * EpicsStatusItemImpl.cpp
 *
 *  Created on: Mar 30, 2009
 *      Author: anunez
 */

#include "EpicsStatusItemImpl.h"
#include <stdlib.h>
#include <string.h>
#include <iostream>
namespace giapi {

EpicsStatusItemImpl::EpicsStatusItemImpl(const std::string &name,
		type::Type type,
		int count,
		const void * data,
		int size) {

	_name = name;
	_type = type;
	_nElements = count;
	_size = size;
	_data = malloc(size);
	memcpy(_data, data, size);
}

EpicsStatusItemImpl::~EpicsStatusItemImpl() {
	free(_data);
}

pEpicsStatusItem EpicsStatusItemImpl::create(const std::string &name,
		type::Type type,
		int count,
		const void * data,
		int size) {
	pEpicsStatusItem item(new EpicsStatusItemImpl(name, type, count, data, size));
	return item;

}

int EpicsStatusItemImpl::getCount() const {
	return _nElements;
}

const std::string EpicsStatusItemImpl::getName() const {
	return _name;
}

const void * EpicsStatusItemImpl::getData() const {
	return _data;
}

type::Type EpicsStatusItemImpl::getType() const {
	return _type;
}


const std::string  EpicsStatusItemImpl::getDataAsString(int index) const
		throw (InvalidOperation) {

	if (_type != type::STRING)
		throw InvalidOperation("EPICS status item does not contain string data");

	validateIndex(index);

	int curpos = 0;
	for (int i = 0; i < index; i++) {
		curpos += strlen((char *)_data + curpos) + 1;
	}

	return std::string((char *)_data + curpos);
}

int EpicsStatusItemImpl::getDataAsInt(int index) const throw (InvalidOperation) {
	if (_type != type::INT)
		throw InvalidOperation("EPICS status item does not contain integer data");

	validateIndex(index);

	int * data = (int *)_data;
	return data[index];
}

float EpicsStatusItemImpl::getDataAsFloat(int index) const throw (InvalidOperation) {
	if (_type != type::FLOAT)
		throw InvalidOperation("EPICS status item does not contain float data");

	validateIndex(index);

	float * data = (float *)_data;
	return data[index];
}

double EpicsStatusItemImpl::getDataAsDouble(int index) const throw (InvalidOperation) {
	if (_type != type::DOUBLE)
		throw InvalidOperation("EPICS status item does not contain double data");

	validateIndex(index);

	double * data = (double *)_data;
	return data[index];
}

unsigned char EpicsStatusItemImpl::getDataAsByte(int index) const throw (InvalidOperation) {
	if (_type != type::BYTE)
		throw InvalidOperation("EPICS status item does not contain byte data");

	validateIndex(index);

	unsigned char * data = (unsigned char *)_data;
	return data[index];
}





void EpicsStatusItemImpl::validateIndex(int index) const throw (InvalidOperation) {
	if (index >= _nElements || index < 0)
		throw InvalidOperation("Index out of range to get element from EPICS status item");
}



}
