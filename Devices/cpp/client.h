#include <string>

#include <malloc.h>
#include <stdio.h>
#include <unistd.h>
#include <syslog.h>
#include <iostream>

#include <qpid/messaging/Connection.h>
#include <qpid/messaging/Message.h>
#include <qpid/messaging/Receiver.h>
#include <qpid/messaging/Sender.h>
#include <qpid/messaging/Session.h>
#include <qpid/messaging/Address.h>

#include <jsoncpp/json/value.h>

#include <uuid/uuid.h>

#include "CDataFile.h"

#define CONFIG_FILE "./config.ini"

namespace agocontrol {

	// these will convert back and forth between a Variant type and JSON
	std::string variantMapToJSONString(qpid::types::Variant::Map map);
	std::string variantListToJSONString(qpid::types::Variant::List list);
	qpid::types::Variant::Map jsonToVariantMap(Json::Value value);
	qpid::types::Variant::Map jsonStringToVariantMap(std::string jsonstring);

	// helper to generate a string containing a uuid
	std::string generateUuid();

	// fetch a value from the config file
	std::string getConfigOption(const char *section, const char *option, const char *defaultvalue);

	// connection class
	class AgoConnection {
		protected:
			qpid::messaging::Connection connection;
			qpid::messaging::Sender sender;
			qpid::messaging::Receiver receiver;
			qpid::messaging::Session session;
			qpid::types::Variant::Map deviceMap; // this holds the internal device list
			qpid::types::Variant::Map uuidMap; // this holds the permanent uuid to internal id mapping
			bool storeUuidMap(); // stores the map on disk
			bool loadUuidMap(); // loads it
			string uuidMapFile;
			string instance;
			std::string uuidToInternalId(std::string uuid); // lookup in map
			std::string internalIdToUuid(std::string internalId); // lookup in map
			void reportDevices();
			std::string (*commandHandler)(qpid::types::Variant::Map);
			bool filterCommands;
			void (*eventHandler)(std::string, qpid::types::Variant::Map);
			bool emitDeviceAnnounce(const char *internalId, const char *deviceType);
			bool emitDeviceRemove(const char *internalId);
		public:
			AgoConnection(const char *interfacename);
			~AgoConnection();
			void run();
			bool addDevice(const char *internalId, const char *deviceType);
			bool removeDevice(const char *internalId);
			string getDeviceType(const char *internalId);
			bool addHandler(std::string (*handler)(qpid::types::Variant::Map));
			bool addEventHandler(void (*eventHandler)(std::string, qpid::types::Variant::Map));
			bool setFilter(bool filter);
			bool sendMessage(const char *subject, qpid::types::Variant::Map content);
			bool sendMessage(qpid::types::Variant::Map content);
			bool emitEvent(const char *internalId, const char *eventType, const char *level, const char *units);
			qpid::types::Variant::Map getInventory();
	};


	enum LogPriority {
	    kLogEmerg   = LOG_EMERG,   // system is unusable
	    kLogAlert   = LOG_ALERT,   // action must be taken immediately
	    kLogCrit    = LOG_CRIT,    // critical conditions
	    kLogErr     = LOG_ERR,     // error conditions
	    kLogWarning = LOG_WARNING, // warning conditions
	    kLogNotice  = LOG_NOTICE,  // normal, but significant, condition
	    kLogInfo    = LOG_INFO,    // informational message
	    kLogDebug   = LOG_DEBUG    // debug-level message
	};

	std::ostream& operator<< (std::ostream& os, const LogPriority& log_priority);

	class Log : public std::basic_streambuf<char, std::char_traits<char> > {
	public:
	    explicit Log(std::string ident, int facility);

	protected:
	    int sync();
	    int overflow(int c);

	private:
	    friend std::ostream& operator<< (std::ostream& os, const LogPriority& log_priority);
	    std::string buffer_;
	    int facility_;
	    int priority_;
	    char ident_[50];
	};

}


