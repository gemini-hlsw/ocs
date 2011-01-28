#ifndef PROPERTIESUTIL_H_
#define PROPERTIESUTIL_H_

#include <log4cxx/logger.h>
#include <log4cxx/logstring.h>
#include <log4cxx/helpers/properties.h>
#include <tr1/memory>
#include <set>
#include <string>

#include <giapi/giapiexcept.h>
#include <giapi/giapi.h>
#include <giapi/GiapiErrorHandler.h>


namespace giapi {

namespace util {

/**
 * This class is a singleton that loads a configuration file.
 *
 * If the environment variable GMP_CONFIGURATION is set, the file it points to is read,
 * otherwise, the file "gmp.properties" in the current directory is read.
 */
class PropertiesUtil {

	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

    log4cxx::helpers::Properties properties;
public:
    /**
     * Method to access the singleton instance.
     *
     * @return reference to the singleton instance.
     */
	static PropertiesUtil& Instance();

    /**
     * Method to get a property by name.
     *
     * @param propName name of the property
     * @return value of the property, empty string if undefined
     */
    log4cxx::LogString getProperty(const log4cxx::LogString& propName);
private:
    void load(const std::string& fileName);
	~PropertiesUtil();
	PropertiesUtil();
	PropertiesUtil(PropertiesUtil const &);
	PropertiesUtil &operator=(PropertiesUtil const &);
};

}

}

#endif /*PROPERTIESUTIL_H_*/
