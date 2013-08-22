#include <stdio.h>
#include <unistd.h>
#include <string>
#include <list>
#include <map>

#include <openzwave/value_classes/ValueStore.h>
#include <openzwave/value_classes/Value.h>
#include <openzwave/value_classes/ValueBool.h>

class ZWaveNode {
	protected:
		std::string devicetype;
		std::string id;	
		std::map<std::string, OpenZWave::ValueID> values;
	public:
		ZWaveNode(std::string id, std::string devicetype);
		~ZWaveNode();
		std::string getId();
		std::string getDevicetype();
		void setDevicetype(std::string devicetype);
		bool hasValue(OpenZWave::ValueID valueID);
		bool addValue(std::string label, OpenZWave::ValueID valueID);
		OpenZWave::ValueID *getValueID(std::string label);
};

class ZWaveNodes {
	public:
		std::list<ZWaveNode*> nodes;
		ZWaveNodes();
		~ZWaveNodes();
		ZWaveNode *findValue(OpenZWave::ValueID valueID);
		ZWaveNode *findId(std::string id);
		bool add(ZWaveNode *node);
		bool remove(std::string id);
};
