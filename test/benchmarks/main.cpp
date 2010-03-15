#include <cppunit/extensions/TestFactoryRegistry.h>
#include <cppunit/ui/text/TestRunner.h>
#include <cppunit/TestResult.h>
#include <iostream>

int main(int argc, char **argv) {
	try {

		CppUnit::TextUi::TestRunner runner;
		CppUnit::TestFactoryRegistry &registry =
				CppUnit::TestFactoryRegistry::getRegistry();

		runner.addTest(registry.makeTest());

		std::cout << "=====================================================\n";
		std::cout << "Starting the Benchmarks:" << std::endl;
		std::cout << "-----------------------------------------------------\n";

		bool wasSuccessful = runner.run("", false);

		std::cout << "-----------------------------------------------------\n";
		std::cout << "Finished with the Benchmarks." << std::endl;
		std::cout << "=====================================================\n";

		return !wasSuccessful;

	} catch (...) {
		std::cout << "----------------------------------------" << std::endl;
		std::cout << "- AN ERROR HAS OCCURED:                -" << std::endl;
		std::cout << "----------------------------------------" << std::endl;
	}
}
