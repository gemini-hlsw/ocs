/*
 * JmsEpicsManager.cpp
 *
 *  Created on: Feb 18, 2009
 *      Author: anunez
 */

#include <gemini/JmsEpicsManager.h>

#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>
#include "JmsEpicsConfiguration.h"

using namespace gmp;

namespace giapi {

log4cxx::LoggerPtr JmsEpicsManager::logger(log4cxx::Logger::getLogger(
		"giapi::gemini::JmsEpicsManager"));

JmsEpicsManager::JmsEpicsManager() throw (CommunicationException) {
	try {
		ConnectionManager& manager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = pSession(manager.createSession());

		_epicsConfiguration = JmsEpicsConfiguration::create(_session);
		
		//attempt to initialize the configuration 
		_epicsConfiguration->init();

	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing request producer :"
				+ e.getMessage());
	} catch (TimeoutException &e) {
		LOG4CXX_WARN(logger, "Can't get valid epics channel names, trying later...")
	}
}

void JmsEpicsManager::cleanup() {
	// Close open resources.
	try {
		_epicsConfiguration.release();
		if (_session.get() != 0)
		_session->close();
	} catch (CMSException& e) {
		e.printStackTrace();
	}
}

JmsEpicsManager::~JmsEpicsManager() {

}

pEpicsManager JmsEpicsManager::create() throw (CommunicationException) {
	pEpicsManager mgr(new JmsEpicsManager());
	return mgr;
}

int JmsEpicsManager::subscribeEpicsStatus(const std::string & name,
		pEpicsStatusHandler handler) throw (GiapiException) {

	if (!(_epicsConfiguration->isInitialized())) { 
		//attempt to initialize it
		_epicsConfiguration->init();
	}

	if (_epicsConfiguration->hasChannel(name)) {
		//epics channel found
		return status::OK;
	} else {
		//not found
		return status::ERROR;
	}
}

int JmsEpicsManager::unsubscribeEpicsStatus(const std::string & name)
		throw (GiapiException) {
	return 0;
}

}
