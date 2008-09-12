#ifndef GEMINISERVICESUTIL_H_
#define GEMINISERVICESUTIL_H_
#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

namespace giapi {

class ServicesUtil {
public:
	/**
	 * Logs a message that will be merged into an instrument-wide
	 * log, using the given log level.
	 *
	 * @param level The logging Level identifier for this log message, such as
	 *        log::WARNING.
	 * @param msg  The string message to log
	 *
	 * @throws GiapiException if there is an error accessing the GMP to log
	 *         the message.
	 */
	static void systemLog(log::Level level, const char *msg) throw (GiapiException);

	/**
	 * Returns the current observatory time in milliseconds. The granularity
	 * of the value depends on the underlying operating system and may be
	 * larger.
	 *
	 * @return the number of milliseconds between the current observatory
	 *         time and midnight, January 1, 1970 UTC as a 64-bit long integer
	 *
	 * @throws GiapiException if there is an error accessing the GMP to get the
	 *         observatory time
	 */
	static long64 getObservatoryTime() throw (GiapiException);

	/**
	 * Gets the GIAPI property indicated by the specified key.
	 *
	 * @param key the name of the GIAPI property
	 *
	 * @return the string value of the GIAPI property, or the default value
	 *         if there is no property with that key.
	 *
	 * @throws GiapiException if there is an error accessing the GMP to get the
	 *         property
	 */
	static const char * getProperty(const char *key) throw (GiapiException);

private:
	ServicesUtil();
	virtual ~ServicesUtil();
};

}

#endif /*GEMINISERVICESUTIL_H_*/
