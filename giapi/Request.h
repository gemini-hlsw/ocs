#ifndef REQUEST_H_
#define REQUEST_H_

#include <giapi/giapi.h>
#include <tr1/memory>

namespace giapi
{

class Request
{
public:
	Request();
	virtual ~Request();
	const command::SequenceCommand getSequenceCommand() const;
	const command::Activity getActivity() const;
};

typedef std::tr1::shared_ptr<Request> pRequest;

}

#endif /*REQUEST_H_*/
