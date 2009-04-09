/**
 * This class offers some utility methods used to facilitate the use of
 * the GIAPI.
 */

#ifndef GIAPIUTIL_H_
#define GIAPIUTIL_H_

#include <giapi/giapiexcept.h>
#include <giapi/giapi.h>
#include <giapi/GiapiErrorHandler.h>

namespace giapi {

/**
 * Auxiliary methods to use the GIAPI.
 */
class GiapiUtil {

public:

	/**
	 * Register an error handler that will be invoked when a GMP
	 * connection problem happens. This is useful to provide
	 * a fault tolerance mechanisms when the GMP connection needs to
	 * be restored.
	 * <p/>
	 * When a communication problem to the GMP is found, the GIAPI
	 * library will attempt to restore the connection to the GMP. Once
	 * the connection is re-established, all the handlers registered
	 * through this method will be invoked. The same handler can't be
	 * registered twice.
	 * <p/>
	 * This is a C-style version of this method, using a function
	 * pointer. There is also a C++ version that uses an interface.
	 *
	 * @param handler a user specified function that will be
	 * invoked when a connection to the GMP is re-established.
	 */
	static void registerGmpErrorHandler(giapi_error_handler handler)
			throw (CommunicationException);

	/**
	 * Register an error handler object that will be invoked when a GMP
	 * connection problem happens. This is useful to provide
	 * a fault tolerance mechanisms when the GMP connection needs to
	 * be restored.
	 * <p/>
	 * When a communication problem to the GMP is found, the GIAPI
	 * library will attempt to restore the connection to the GMP. Once
	 * the connection is re-established, all the handlers registered
	 * through this method will be invoked. The same handler can't be
	 * registered twice.
	 * <p/>
	 * This is the C++ style version of this method, using an interface.
	 * There is also a C version that uses a function pointer
	 *
	 * @param handler Smart pointer to an object representing the
	 * error handler to be invoked.
	 */
	static void registerGmpErrorHandler(pGiapiErrorHandler handler)
			throw (CommunicationException);

private:
	GiapiUtil();
	virtual ~GiapiUtil();
};

}

#endif /* GIAPIUTIL_H_ */
