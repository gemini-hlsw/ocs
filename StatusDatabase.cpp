#include "StatusDatabase.h"
#include <giapi/giapi.h>
namespace giapi {

log4cxx::LoggerPtr StatusDatabase::logger(log4cxx::Logger::getLogger("giapi.StatusDatabase"));

StatusDatabase* StatusDatabase::INSTANCE = 0;

StatusDatabase::StatusDatabase() {
}

StatusDatabase::~StatusDatabase() {
	//TODO: Remove all the objects
}

StatusDatabase& StatusDatabase::Instance() {
	if (INSTANCE == 0) {
		INSTANCE = new StatusDatabase();
	}
	return *INSTANCE;
}

int StatusDatabase::setStatusValueAsInt(const char * name, int value) {
	StatusItem* statusItem = getOrMakeStatusItem(name);
	if (statusItem == 0) {
		return status::GIAPI_NOK;
	}
	return statusItem->setValueAsInt(value);
}

int StatusDatabase::setStatusValueAsString(const char *name, const char * value) {
	StatusItem* statusItem = getOrMakeStatusItem(name);
	if (statusItem == 0) {
		return status::GIAPI_NOK;
	}
	return statusItem->setValueAsString(value);
}

StatusItem * StatusDatabase::getStatusItem(const char * name) {
	if (name == 0) {
		return (StatusItem *)0; //NULL
	}
	return _map[name];
}

StatusItem * StatusDatabase::getOrMakeStatusItem(const char * name) {

	if (name == 0) {
		return (StatusItem *)0;  //We can't make a null status item
	}
	
	StatusItem * statusItem = getStatusItem(name);

	if (statusItem == 0) {
		//No status item found. Make a new one
		LOG4CXX_INFO(logger, "StatusDatabase::updateStatusItem: No Status item associated to: "
				<< name << "...making new instance");
		statusItem = new StatusItem(name); 
		_map[name] = statusItem;
	}
	return statusItem;

}

}