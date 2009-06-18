/*
 * JmsObsEventProducer.h
 *
 *  Created on: Jun 10, 2009
 *      Author: anunez
 */

#ifndef JMSOBSEVENTPRODUCER_H_
#define JMSOBSEVENTPRODUCER_H_

#include <giapi/giapi.h>

#include <util/jms/JmsProducer.h>


namespace giapi {

class JmsObsEventProducer;

typedef std::auto_ptr<JmsObsEventProducer> pJmsObsEventProducer;


class JmsObsEventProducer : public util::jms::JmsProducer {
public:

	static pJmsObsEventProducer create() throw (CommunicationException);

	virtual ~JmsObsEventProducer();

	int postEvent(data::ObservationEvent event, const std::string &fileName) throw (CommunicationException);

private:
	JmsObsEventProducer() throw (CommunicationException);
};

}

#endif /* JMSOBSEVENTPRODUCER_H_ */
