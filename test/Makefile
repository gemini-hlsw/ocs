#
# Makefile for the GIAPI C++ Language Glue test code
#

#include sources list and objects built from there
-include sources.mk
-include ../conf/config.mk
-include ../conf/common.mk
-include giapi/sources.mk


#Set the LD_LIBRARY_PATH to run the tests
LD_LIBRARY_PATH := ../:$(LOG4CXX_LIB):$(CPPUNIT_LIB):$(ACTIVEMQ_LIB)

#Includes to build
INC_DIRS := -I../  -I$(CPPUNIT_INCLUDE) 
# Libraries
LIB_DIRS := -L$(CPPUNIT_LIB) -L$(LOG4CXX_LIB) -L$(ACTIVEMQ_LIB) -L../
LIBS := -lcppunit -lgiapi-glue-cc -llog4cxx -lactivemq-cpp

EXE_NAME := libgiapi-test

# Tool invocations
all: libgiapi-test
	@ echo "Running unit tests"
	@ sh runtests.sh $(LD_LIBRARY_PATH)
	
libgiapi-test: $(OBJS) 
	@echo 'Building target: $@'
	@echo 'Invoking: $(OS) C++ Linker'
	$(CXX) $(LIB_DIRS) -o $(EXE_NAME) $(OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '

clean:
	-$(RM) $(OBJS)$(CPP_DEPS)$(LIBRARIES) $(EXE_NAME)
	-@echo ' '
	
%.o: %.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' ' 