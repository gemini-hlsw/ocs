################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
OBJS += $(patsubst %.cpp,%.o,$(wildcard ./src/gmp/*.cpp))

CPP_DEPS += $(patsubst %.cpp,%.d,$(wildcard ./src/gmp/*.cpp))

# Each subdirectory must supply rules for building sources it contributes
src/gmp/%.o: ../src/gmp/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"../$(@:%.o=%.d)" -MT"../$(@:%.o=%.d)" -o"../$@" "$<"
	@echo 'Finished building: $<'
	@echo ' ' 
