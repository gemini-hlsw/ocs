#ifndef STATUSDATABASE_H_
#define STATUSDATABASE_H_
#include <ext/hash_map>

#include <log4cxx/logger.h>

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
	StatusItem* getOrMakeStatusItem(const char *name);
public:
	static StatusDatabase & Instance(); //TODO: Synchronize
	StatusDatabase();
	virtual ~StatusDatabase();

	//void makeStatusItem(const char *name, Type type);
	int setStatusValueAsInt(const char *name, int value);
	int setStatusValueAsString(const char *name, const char *value);

	//get a status item from the database
	StatusItem* getStatusItem(const char *name);

};
}
#endif /*STATUSDATABASE_H_*/
