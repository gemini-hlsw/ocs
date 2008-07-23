################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
OBJS += \
./gmp/ConnectionManager.o \
./gmp/GMPKeys.o \
./gmp/JmsUtil.o \
./gmp/SequenceCommandConsumer.o 

CPP_DEPS += \
./gmp/ConnectionManager.d \
./gmp/GMPKeys.d \
./gmp/JmsUtil.d \
./gmp/SequenceCommandConsumer.d

# Each subdirectory must supply rules for building sources it contributes
gmp/%.o: ../gmp/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"../$(@:%.o=%.d)" -MT"../$(@:%.o=%.d)" -o"../$@" "$<"
	@echo 'Finished building: $<'
	@echo ' ' 