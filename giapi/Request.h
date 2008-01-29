#ifndef REQUEST_H_
#define REQUEST_H_

#include <giapi/giapi.h>

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

}

#endif /*REQUEST_H_*/
