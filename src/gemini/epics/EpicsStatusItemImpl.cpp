/*
 * EpicsStatusItemImpl.cpp
 *
 *  Created on: Mar 30, 2009
 *      Author: anunez
 */

#include "EpicsStatusItemImpl.h"
#include <stdlib.h>
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





}
