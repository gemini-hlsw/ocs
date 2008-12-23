
OBJS += $(patsubst %.cpp,%.o,$(wildcard ./giapi/*.cpp))

CPP_DEPS += $(patsubst %.cpp,%.d,$(wildcard ./giapi/*.cpp))
