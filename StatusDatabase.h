#ifndef STATUSDATABASE_H_
#define STATUSDATABASE_H_
#include <ext/hash_map>

#include <log4cxx/logger.h>

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>
#include "StatusItem.h"

//hash_map is an extension of STL. Will make its namespace visible here. 
using namespace __gnu_cxx;

namespace giapi {

struct eqstr {
	bool operator()(const char *s1, const char *s2) const {
		return strcmp(s1, s2) == 0;
	}
};

typedef hash_map<const char *, StatusItem *, hash<const char *>, eqstr>
		StringStatusMap;

class StatusDatabase {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

private:
	StringStatusMap _map;
	static StatusDatabase * INSTANCE;
public:
	static StatusDatabase & Instance(); //TODO: Synchronize
	StatusDatabase();
	virtual ~StatusDatabase();

	/**
	 * Create an status item in the database
	 *  
	 * @param name The name of the status item that will be 
	 * 	           created. If an status item with the same name already exists, 
	 *             the method will return giapi::status::NOK
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::NOK if there is an error. 
	 */
	int createStatusItem(const char *name);

	/**
	 * Create an alarm status item in the status database
	 * 
	 * @param name The name of the alarm status item that will be 
	 * 	           created. If an status item with the same name already exists, 
	 *             the method will return giapi::status::NOK
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::NOK if there is an error. 
	 */
	int createAlarmStatusItem(const char *name);

	int setStatusValueAsInt(const char *name, int value);
	int setStatusValueAsString(const char *name, const char *value);

	/**
	 * Set the alarm for the specified status alarm item. 
	 * 
	 * @param name Name of the alarm item. The alarm items should have been 
	 *             initialized by a call to {@link #createAlarmStatusItem()}
	 *             
	 * @param severity the alarm severity.
	 * @param cause the cause of the alarm 
	 * @param message Optional message to describe the alarm
	 * 
	 * @return giapi::status::OK if alarm was sucessfully set 
	 *         giapi::status::NOK if there was an error setting the alarm. This 
	 *         happens for instance if the alarm status item has not been 
	 *         created or the name is associated to an status item without 
	 *         alarms).
	 *        
	 * @see alarm::Severity
	 * @see alarm::Cause
	 */
	int setAlarm(const char *name, alarm::Severity severity,
			alarm::Cause cause, const char *message = 0);

	//get a status item from the database
	StatusItem* getStatusItem(const char *name);

};
}
#endif /*STATUSDATABASE_H_*/
