#ifndef JMSFILEEVENTSPRODUCER_H_
#define JMSFILEEVENTSPRODUCER_H_

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>
#include <util/jms/JmsProducer.h>

namespace giapi {

class JmsFileEventsProducer;

typedef std::auto_ptr<JmsFileEventsProducer> pJmsFileEventsProducer;

/**
 * This class send JMS messages containing notification
 * events for ancillary files and intermediate files produced
 * by the instrument.
 */
class JmsFileEventsProducer: public util::jms::JmsProducer {
public:

	/**
	 * Factory method to create a producer of File Events.
	 * @return a smart pointer to the newly created file
	 *         event producer
	 */
	static pJmsFileEventsProducer create() throw (CommunicationException);

	/**
	 * Destructor
	 */
	virtual ~JmsFileEventsProducer();

	/**
	 * Post an Ancillary File Event to the GMP, associated with the
	 * given dataset label
	 * @param filename name of the ancillary file
	 * @param dataLabel the data-label associated to this ancillary file
	 * @return status::OK if the event was sent via JMS properly or
	 *         status::ERROR if there is a problem.
	 *
	 * @throws CommunicationException if there is an error sending the
	 *         event to the GMP via JMS
	 */
	int postAncillaryFileEvent(const std::string & filename,
			const std::string & dataLabel) throw (CommunicationException);

	/**
	 * Post an Intermediate File Event to the GMP, associated with the
	 * given dataset label
	 * @param filename name of the ancillary file
	 * @param dataLabel the data-label associated to this ancillary file
	 * @hint is an optional argument that indicates additional information
	 * about this event
	 * @return status::OK if the event was sent via JMS properly or
	 *         status::ERROR if there is a problem.
	 *
	 * @throws CommunicationException if there is an error sending the
	 *         event to the GMP via JMS
	 */

	int postIntermediateFileEvent(const std::string & filename,
			const std::string & datalabel, const std::string & hint)
			throw (CommunicationException);

private:
	JmsFileEventsProducer() throw (CommunicationException);

	/**
	 * A type for the different File Events supported.
	 */
	enum EventFileType {
		ANCILLARY_TYPE    = 0,
		INTERMEDIATE_TYPE = 1
	};

	/**
	 * Auxiliary method to send a File Event Message for the given type of
	 * File Event and the given parameters
	 */
	int sendFileEventMessage(EventFileType type, const std::string & filename,
			const std::string & dataLabel, const std::string &hint);

	/**
	 * Auxiliary method to send a File Event Message when the hint is not required
	 */
	int sendFileEventMessage(EventFileType type, const std::string & filename,
			const std::string & dataLabel);

};

}

#endif /* JMSFILEEVENTSPRODUCER_H_ */
