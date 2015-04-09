
#ifndef TIMEUTIL_H_
#define TIMEUTIL_H_
#include <stdexcept>
#include <sys/time.h>

namespace giapi {

namespace util {

/**
* Class to provide a way to measure elapsed time.
*/
class TimeUtil {
private:
    timeval startTime, endTime;
    bool running,done;
public:
    enum TimeUnit{USEC,MSEC,SEC};
    TimeUtil();
    ~TimeUtil();

    /**
    * Start the timer.
    */
    void startTimer();

    /**
    * Stop the timer.
    *
    * @throws logic_error if this method is called before calling <code>startTimer()</code>
    */
    void stopTimer() throw(std::logic_error);

    /**
    * Gets the elapsed time between subsequent calls to <code>startTimer()</code>
    * and <code>stopTimer()</code>
    *
    * @param unit the time unit of the return value. Either Seconds, milliseconds or microseconds.
    * @return the elapsed time
    * @throws logic_error if this method is called before calling <code>stopTimer()</code>
    */
    unsigned long long getElapsedTime(TimeUnit unit) throw(std::logic_error);

    /**
    * Gets the elapsed time in microseconds between subsequent calls to <code>startTimer()</code>
    * and <code>stopTimer()</code>
    *
    * @return the elapsed time
    * @throws logic_error if this method is called before calling <code>stopTimer()</code>
    */
    unsigned long long getElapsedUSecs()throw(std::logic_error);
    /**
    * Gets the elapsed time in milliseconds between subsequent calls to <code>startTimer()</code>
    * and <code>stopTimer()</code>
    *
    * @return the elapsed time
    * @throws logic_error if this method is called before calling <code>stopTimer()</code>
    */
    unsigned long long getElapsedMSecs()throw(std::logic_error);
    /**
    * Gets the elapsed time in seconds between subsequent calls to <code>startTimer()</code>
    * and <code>stopTimer()</code>
    *
    * @return the elapsed time
    * @throws logic_error if this method is called before calling <code>stopTimer()</code>
    */
    unsigned long long getElapsedSecs()throw(std::logic_error);
};
}
}

#endif /* TIMEUTIL_H_ */
