#ifndef ALARMSTATUSITEM_H_
#define ALARMSTATUSITEM_H_

#include "StatusItem.h"

namespace giapi {
class AlarmStatusItem : public StatusItem
{
public:
	AlarmStatusItem(const char *name);
	virtual ~AlarmStatusItem();
};
}

#endif /*ALARMSTATUSITEM_H_*/
