/*
 * StatusPostBenchmark.h
 *
 *  Created on: Feb 13, 2009
 *      Author: anunez
 */

#ifndef STATUSPOSTBENCHMARK_H_
#define STATUSPOSTBENCHMARK_H_


#include <benchmark/BenchmarkBase.h>
#include <giapi/StatusUtil.h>

namespace giapi {

class StatusPostBenchmark :
	public benchmark::BenchmarkBase<
		giapi::StatusPostBenchmark, StatusUtil, 1>{
private:
	static const int NUM_MESSAGES = 10000;

public:
	StatusPostBenchmark();
	virtual ~StatusPostBenchmark();

	void run();

	void setUp();

	int getOps();
};

}

#endif /* STATUSPOSTBENCHMARK_H_ */
