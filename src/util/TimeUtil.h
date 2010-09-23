
#ifndef TIMEUTIL_H_
#define TIMEUTIL_H_
#include <stdexcept>

namespace giapi {

namespace util {

class TimeUtil {
private:
    timeval startTime, endTime;
    bool running,done;
public:
    enum TimeUnit{USEC,MSEC,SEC};
    TimeUtil();
    ~TimeUtil();

    void startTimer();
    void stopTimer() throw(std::logic_error);
    unsigned long long getElapsedTime(TimeUnit unit) throw(std::logic_error);
    unsigned long long getElapsedUSecs()throw(std::logic_error);
    unsigned long long getElapsedMSecs()throw(std::logic_error);
    unsigned long long getElapsedSecs()throw(std::logic_error);
};
}
}

#endif /* TIMEUTIL_H_ */
