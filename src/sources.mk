################################################################################
# Include here all the source.mk files from directories with source files
# for this project
################################################################################
-include src/commands/sources.mk
-include src/services/sources.mk
-include src/status/sources.mk
-include src/gmp/sources.mk


# Add inputs and outputs from these tool invocations to the build variables 
OBJS += $(patsubst %.cpp,%.o,$(wildcard ./src/*.cpp))

CPP_DEPS += $(patsubst %.cpp,%.d,$(wildcard ./src/*.cpp))



