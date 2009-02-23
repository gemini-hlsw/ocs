#include <cppunit/extensions/TestFactoryRegistry.h>
#include <cppunit/ui/text/TestRunner.h>
#include <cppunit/BriefTestProgressListener.h>
#include <cppunit/TestResult.h>
#include <iostream>

int main( int argc, char **argv)
{
    try
    {
    	CppUnit::TextUi::TestRunner runner;
        CppUnit::TestFactoryRegistry &registry = CppUnit::TestFactoryRegistry::getRegistry();
        runner.addTest( registry.makeTest() );

        // Shows a message as each test starts
        CppUnit::BriefTestProgressListener listener;
        runner.eventManager().addListener( &listener );

        bool wasSuccessful = runner.run( "", false );
        return !wasSuccessful;
    }
    catch(...) {
        std::cout << "----------------------------------------" << std::endl;
        std::cout << "- AN ERROR HAS OCCURED:                -" << std::endl;
        std::cout << "- Do you have a GMP Broker Running?    -" << std::endl;
        std::cout << "----------------------------------------" << std::endl;
    }
}
