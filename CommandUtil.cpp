#include "giapi/CommandUtil.h"

namespace giapi {

CommandUtil::CommandUtil()
{
}

CommandUtil::~CommandUtil()
{
}

int CommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::Activity activities[],
		std::tr1::shared_ptr<SequenceCommandHandler> handler) {
	//TODO: Implement this method.

	return giapi::status::OK;
	
}

}
