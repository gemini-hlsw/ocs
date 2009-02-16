/*
 * BenchmarkBase.h
 *
 *  Created on: Feb 13, 2009
 *      Author: anunez
 */

#ifndef BENCHMARKBASE_H_
#define BENCHMARKBASE_H_

#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>

#include <iostream>


namespace benchmark {

	template < class NAME, class TARGET, int ITERATIONS = 100>
	class BenchmarkBase: public CppUnit::TestFixture
	{

		CPPUNIT_TEST_SUITE( NAME );
		CPPUNIT_TEST( runBenchmark );
		CPPUNIT_TEST_SUITE_END();


	public:

		void runBenchmark() {
			clock_t start, finish;
			start = clock();
			for (int i = 0; i < ITERATIONS; i++) {
				this->run();
			}
			finish = clock();

			double elapsed = ( ( (double)(finish - start) ) /CLOCKS_PER_SEC );

			std::cout << std::endl << typeid (TARGET).name() << " Benchmark Time = "
					<< elapsed << " seconds " << std::endl;

			if (this->getOps() != 0) {
				std::cout << typeid (TARGET).name() << " Benchmark Operations/second = "
						<< ((double)this->getOps() / elapsed) << std::endl;
			}

		}

		virtual void run() = 0;

		virtual int getOps() {
			return 0;
		}
	};


}



#endif /* BENCHMARKBASE_H_ */
