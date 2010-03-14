/*
 * JmsTcsFetcher.cpp
 *
 *  Created on: Dec 10, 2009
 *      Author: anunez
 */

#include "JmsTcsFetcher.h"
#include <gmp/GMPKeys.h>

using namespace gmp;

namespace giapi {

namespace gemini {

namespace tcs {

namespace jms {

/* Size of the TCS Context */
int const JmsTcsFetcher::TCS_CTX_SIZE = 39;

JmsTcsFetcher::JmsTcsFetcher() throw (CommunicationException) :
	JmsProducer(GMPKeys::GMP_TCS_CONTEXT_DESTINATION) {

}

pTcsFetcher JmsTcsFetcher::create() throw (CommunicationException) {
	pTcsFetcher fetcher(new JmsTcsFetcher());
	return fetcher;
}

int JmsTcsFetcher::fetch(TcsContext & ctx, long timeout)
		throw (CommunicationException, TimeoutException) {

	Message * request = NULL;
	try {
		//an empty message to make the request. We don't need to provide any data.
		request = _session->createMessage();
		//create temporary objects to get the answer
		TemporaryQueue * tmpQueue = _session->createTemporaryQueue();
		MessageConsumer * tmpConsumer = _session->createConsumer(tmpQueue);

		//define the destination for the service to provide an answer
		request->setCMSReplyTo(tmpQueue);
		//send the request
		_producer->send(request);
		//delete the request, not needed anymore
		delete request;

		//and wait for the response, timing out if necessary.
		Message *reply = (timeout > 0) ? tmpConsumer->receive(timeout)
				: tmpConsumer->receive();

		tmpConsumer->close();
		delete tmpConsumer;

		tmpQueue->destroy();
		delete tmpQueue;

		if (reply != NULL) {
			return _buildTcsContext(ctx, reply);
		} else { //timeout .Throw an exception
			throw TimeoutException("Time out while waiting for TCSContext");
		}
	} catch (CMSException &e) {
		if (request != NULL) {
			delete request;
		}
		throw CommunicationException("Problem fetching the TCS Context "
				+ e.getMessage());
	}
	return status::OK;
}

int JmsTcsFetcher::_buildTcsContext(TcsContext &ctx, Message *message)
		throw (CMSException) {

	const BytesMessage* bytesMessage =
			dynamic_cast<const BytesMessage*> (message);

	if (bytesMessage == NULL) {
		return status::ERROR;
	}

	//get the size.
	int size = bytesMessage->readInt();

	if (size < TCS_CTX_SIZE) {
		return status::ERROR;
	}

	double * values;
	values = (double *) malloc(size * sizeof(double));
	/* Get all the values from the message */
	for (int i = 0; i < size; i++) {
		values[i] = bytesMessage->readDouble();
	}

	/* Copy the raw time */
	ctx.time = values[0];

	/* Copy the cartesian elements of mount pre-flexure az/el */
	ctx.x = values[1];
	ctx.y = values[2];
	ctx.z = values[3];

	/* Copy the telescope parameters */
	ctx.tel.fl = values[4];
	ctx.tel.rma = values[5];
	ctx.tel.an = values[6];
	ctx.tel.aw = values[7];
	ctx.tel.pnpae = values[8];
	ctx.tel.ca = values[9];
	ctx.tel.ce = values[10];
	ctx.tel.pox = 0.0; /* For safety: no bearing on WCS
	                      (See astGetSet.c in slalib)*/
	ctx.tel.poy = 0.0; /* For safety: no bearing on WCS
	                      (See astGetSet.c in slalib)*/

	/* Copy the apparent to observed parameters */
	ctx.aoprms[0] = values[11];
	ctx.aoprms[1] = values[12];
	ctx.aoprms[2] = values[13];
	ctx.aoprms[3] = values[14];
	ctx.aoprms[4] = values[15];
	ctx.aoprms[5] = values[16];
	ctx.aoprms[6] = values[17];
	ctx.aoprms[7] = values[18];
	ctx.aoprms[8] = values[19];
	ctx.aoprms[9] = values[20];
	ctx.aoprms[10] = values[21];
	ctx.aoprms[11] = values[22];
	ctx.aoprms[12] = 0.0; /* For safety: no bearing on WCS
							 (See astGetSet.c in slalib)*/
	ctx.aoprms[13] = values[23];
	ctx.aoprms[14] = values[24];

	/* Copy the m2 tip tilts */
	ctx.m2xy[0][0] = values[25];
	ctx.m2xy[0][1] = values[26];
	ctx.m2xy[1][0] = values[27];
	ctx.m2xy[1][1] = values[28];
	ctx.m2xy[2][0] = values[29];
	ctx.m2xy[2][1] = values[30];

	/* Copy the current pointing origin */
	ctx.po.mx = values[31];
	ctx.po.my = values[32];
	ctx.po.ax = values[33];
	ctx.po.ay = values[34];
	ctx.po.bx = values[35];
	ctx.po.by = values[36];
	ctx.po.cx = values[37];
	ctx.po.cy = values[38];

	/* The distortion coefficients are not available. Put
	 * harmless values instead */
	ctx.ao2t[0] = 0.0;
	ctx.ao2t[1] = 1.0;
	ctx.ao2t[2] = 0.0;
	ctx.ao2t[3] = 0.0;
	ctx.ao2t[4] = 0.0;
	ctx.ao2t[5] = 1.0;

	free(values);

	return status::OK;
}

}

}

}

}
