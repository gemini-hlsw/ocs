#ifndef SMARTPOINTERDEFS_H_
#define SMARTPOINTERDEFS_H_

#endif /*SMARTPOINTERDEFS_H_*/

#include <cms/Session.h>
#include <cms/Destination.h>
#include <cms/MessageProducer.h>
#include <cms/Connection.h>
#include <tr1/memory>

using namespace cms;
/**
 * A few util typedefs for smart pointers to the JMS data structures
 */
typedef std::tr1::shared_ptr<Session> pSession;
typedef std::tr1::shared_ptr<Destination> pDestination;
typedef std::tr1::shared_ptr<MessageProducer> pMessageProducer;
typedef std::tr1::shared_ptr<MessageConsumer> pMessageConsumer;
typedef std::tr1::shared_ptr<Connection> pConnection;
