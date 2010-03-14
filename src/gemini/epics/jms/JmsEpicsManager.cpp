#include "JmsEpicsManager.h"

#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>
#include <gemini/epics/jms/JmsEpicsConfiguration.h>


namespace giapi {

log4cxx::LoggerPtr JmsEpicsManager::logger(log4cxx::Logger::getLogger(
		"giapi::gemini::JmsEpicsManager"));

JmsEpicsManager::JmsEpicsManager() throw (CommunicationException) {
	try {
		_connectionManager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

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
	LOG4CXX_DEBUG(logger, "Destroying JMS Epics Manager")
	//cleaning epics maps
	_epicsConsumersMap.clear();
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
		//epics channel found, let's create a consumer
		pEpicsConsumer consumer = EpicsConsumer::create(name, handler);
		//and store it for further reference.(otherwise it would just die immediately)
		_epicsConsumersMap[name] =  consumer;
		return status::OK;
	} else {
		//not found
		LOG4CXX_WARN(logger, "Requested subscription to unauthorized EPICS channel: " + name);
		return status::ERROR;
	}
}

int JmsEpicsManager::unsubscribeEpicsStatus(const std::string & name)
		throw (GiapiException) {

	if (_epicsConfiguration->hasChannel(name)) {
		_epicsConsumersMap.erase(name);
		return status::OK;
	} else {
		LOG4CXX_WARN(logger, "Requested un-subscription from unauthorized EPICS channel: " + name);
		return status::ERROR;
	}


}

}
