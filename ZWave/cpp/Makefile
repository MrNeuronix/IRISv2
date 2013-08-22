#

.SUFFIXES:	.cpp .o .a .s

CC     := gcc
CXX    := g++
LD     := g++
AR     := ar rc
RANLIB := ranlib

DEBUG_CFLAGS    := -Wall -Wno-format -g -DDEBUG
RELEASE_CFLAGS  := -Wall -Wno-unknown-pragmas -Wno-format -O3

DEBUG_LDFLAGS	:= -g -L./

# Change for DEBUG or RELEASE
CFLAGS	:= -c $(DEBUG_CFLAGS) -fPIC
LDFLAGS	:= $(DEBUG_LDFLAGS)

INCLUDES	:=  -I./ -I/usr/include/openzwave -I/usr/include/openzwave/value_classes -I/usr/include/openzwave/platform -I/usr/include/openzwave/platform/unix

%.o : %.cpp
	$(CXX) $(CFLAGS) $(INCLUDES) -o $@ $<

all: libclient.so zwave

libclient.so: client.o CDataFile.o
	$(LD) -o $(@)$(LIB_VER) -g -shared -Wl,-soname,libclient.so $^ -lqpidmessaging -lqpidtypes -luuid -ljsoncpp

zwave: zwave.o ZWaveNode.o
	$(LD) -o $@ $(LDFLAGS) $^ -lclient -lopenzwave -ludev -lqpidtypes -lpthread

clean:
	rm -f zwave zwave.o client.o CDataFile.o libclient.so*
