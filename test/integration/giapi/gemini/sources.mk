
SUBDIR = giapi/gemini

OBJS += $(patsubst %.cpp,%.o,$(wildcard ./$(SUBDIR)/*.cpp))

CPP_DEPS += $(patsubst %.cpp,%.d,$(wildcard ./$(SUBDIR)/*.cpp))
