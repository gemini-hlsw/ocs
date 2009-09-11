#ifndef PCSUPDATER_H_
#define PCSUPDATER_H_

#include <tr1/memory>
#include <giapi/giapiexcept.h>
#include <giapi/giapi.h>

namespace giapi {

namespace gemini {

namespace pcs {

/**
 * Interface to define a class in charge of sending zernikes updates to
 * the primary control system using a particular communication
 * mechanism
 */
class PcsUpdater {
public:
	/**
	 * Virtual destructor
	 */
	virtual ~PcsUpdater() {};

	/**
	 * Publish updated zernikes corrections to the PCS
	 * to the GMP. The communication mechanism to interact with
	 * the GMP is left to the implementations of this interface.
	 * @param zernikes an array with the zerinkes coefficients we will send.
	 * @param number of zernikes coefficients being sent.
	 * @throw GiapiException in case a problem occurs while posting
	 *        the zernikes updates
	 */
	virtual int postPcsUpdate(double zernikes[], int size)
			throw (GiapiException) = 0;
};

/**
 * A smart pointer definition for the PcsUpdater class.
 */
typedef std::tr1::shared_ptr<PcsUpdater> pPcsUpdater;

}

}

}
#endif /* PCSUPDATER_H_ */
