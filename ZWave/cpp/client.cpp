#include <string>
#include <string.h>

#include <stdio.h>
#include <unistd.h>

#include <jsoncpp/json/reader.h>
#include "client.h"

using namespace std;
using namespace qpid::messaging;
using namespace qpid::types;

// helper to determine last element
template <typename Iter>
Iter next(Iter iter)
{
    return ++iter;
}

std::string agocontrol::variantMapToJSONString(qpid::types::Variant::Map map) {
	string result;
	result += "{";
	for (Variant::Map::const_iterator it = map.begin(); it != map.end(); ++it) {
		result += "\""+ it->first + "\":";
		switch (it->second.getType()) {
			case VAR_MAP:
				result += variantMapToJSONString(it->second.asMap());
				break;
			case VAR_LIST:
				result += variantListToJSONString(it->second.asList());
				break;
			case VAR_STRING:
				result += "\"" +  it->second.asString() + "\"";
				break;
			default:
				if (it->second.asString().size() != 0) {
					result += it->second.asString();
				} else {
					result += "null";
				}
		}
		if ((it != map.end()) && (next(it) != map.end())) result += ",";
	}
	result += "}";

	return result;
}

std::string agocontrol::variantListToJSONString(qpid::types::Variant::List list) {
	string result;
	result += "[";
	for (Variant::List::const_iterator it = list.begin(); it != list.end(); ++it) {
		switch(it->getType()) {
			case VAR_MAP:
				result += variantMapToJSONString(it->asMap());
				break;
			case VAR_LIST:
				result += variantListToJSONString(it->asList());
				break;
			case VAR_STRING:
				result += "\"" + it->asString()+ "\"";
				break;
			default:
				if (it->asString().size() != 0) {
					result += it->asString();
				} else {
					result += "null";
				}
		}
		if ((it != list.end()) && (next(it) != list.end())) result += ",";
	}
	result += "]";
	return result;
}

qpid::types::Variant::Map agocontrol::jsonToVariantMap(Json::Value value) {
	Variant::Map map;
	for (Json::ValueIterator it = value.begin(); it != value.end(); it++) {
		// printf("%s\n",it.key().asString().c_str());
		// printf("%s\n", (*it).asString().c_str());
		if ((*it).size() > 0) {
			map[it.key().asString()] = jsonToVariantMap((*it));
		} else {
			if ((*it).isString()) map[it.key().asString()] = (*it).asString();
			if ((*it).isBool()) map[it.key().asString()] = (*it).asBool();
			if ((*it).isInt()) map[it.key().asString()] = (*it).asInt();
			if ((*it).isUInt()) map[it.key().asString()] = (*it).asUInt();
			if ((*it).isDouble()) map[it.key().asString()] = (*it).asDouble();
		}
	}
	return map;
}

qpid::types::Variant::Map agocontrol::jsonStringToVariantMap(std::string jsonstring) {
	Json::Value root;
	Json::Reader reader;
	Variant::Map result;

	if ( reader.parse(jsonstring, root)) {
		result = jsonToVariantMap(root);
	}/* else {
		printf("warning, could not parse json to Variant::Map: %s\n",jsonstring.c_str());
	}*/
	return result;
}

// generates a uuid as string via libuuid
std::string agocontrol::generateUuid() {
	string strUuid;
	char *name;
	if ((name=(char*)malloc(38)) != NULL) {
		uuid_t tmpuuid;
		name[0]=0;
		uuid_generate(tmpuuid);
		uuid_unparse(tmpuuid,name);
		strUuid = string(name);
		free(name);
	}
	return strUuid;
}

std::string agocontrol::getConfigOption(const char *section, const char *option, const char *defaultvalue) {
	std::string result;
	t_Str value = t_Str("");
	CDataFile ExistingDF(CONFIG_FILE);

	value = ExistingDF.GetString(option, section);
	if (value.size() == 0)
		result = defaultvalue;
	else
		result = value;
	return result;
}

agocontrol::AgoConnection::AgoConnection(const char *interfacename) {
	Variant::Map connectionOptions;
	connectionOptions["username"] = getConfigOption("system", "username", "admin");
	connectionOptions["password"] = getConfigOption("system", "password", "admin");
	connectionOptions["reconnect"] = "true";

	filterCommands = true; // only pass commands for child devices to handler by default
	commandHandler = NULL;
	eventHandler = NULL;
	instance = interfacename;

	uuidMapFile = "./";
	uuidMapFile += interfacename;
	uuidMapFile += ".json";
	loadUuidMap();

	connection = Connection(getConfigOption("system", "broker", "localhost:5672"),connectionOptions);
	try {
		connection.open();
		session = connection.createSession();
		receiver = session.createReceiver("iris; {create: always, node: {type: topic}}");
		sender = session.createSender("iris; {create: always, node: {type: topic}}");
	} catch(const std::exception& error) {
		std::cerr << error.what() << std::endl;
		connection.close();
		printf("could not connect to broker\n");
		_exit(1);
	}
}

agocontrol::AgoConnection::~AgoConnection() {
	try {
		connection.close();
	} catch(const std::exception& error) {
		std::cerr << error.what() << std::endl;
	}
}


void agocontrol::AgoConnection::run() {
	// reportDevices(); // this is obsolete as it is handled by addDevice
	while( true ) {
		try{
			Variant::Map content;
			Message message = receiver.fetch(Duration::SECOND * 3);

			// workaround for bug qpid-3445
			if (message.getContent().size() < 4) {
				throw qpid::messaging::EncodingException("message too small");
			}

			decode(message, content);
			// std::cout << content << std::endl;
			session.acknowledge();

			if (content["command"] == "discover") {
				reportDevices(); // make resolver happy and announce devices on discover request
			} else {
				if (message.getSubject().size() == 0) {
					// no subject, this is a command
					// lets see if this is for one of our devices or if we shall pass everything unfiltered
					string internalid = uuidToInternalId(content["uuid"].asString());
					if (
						(
							((internalid.size() > 0) && (deviceMap.find(internalIdToUuid(internalid)) != deviceMap.end()))
							|| (!(filterCommands))
						)
						&& commandHandler != NULL
					) {
						// found a match, reply to sender and pass the command to the assigned handler method
						const Address& replyaddress = message.getReplyTo();
						if (replyaddress) {
							Session replysession = connection.createSession();
							try {
								Sender replysender = replysession.createSender(replyaddress);
								Message response("ACK");
								replysender.send(response);
								replysession.close();
							} catch(const std::exception& error) {
								printf("can't send reply\n");
								replysession.close();
							}
						}

						// printf("command for id %s found, calling handler\n", internalid.c_str());
						if (internalid.size() > 0) content["internalid"] = internalid;
						string status = commandHandler(content);
						if (status != "") {
							Variant::Map state;
							state["level"] = status;
							state["uuid"] = content["uuid"];
							sendMessage("event.device.statechanged", state);
						}
					}
				} else if (eventHandler != NULL) {
					eventHandler(message.getSubject(), content);
				}
			}
		} catch(const NoMessageAvailable& error) {

		} catch(const std::exception& error) {
			std::cerr << error.what() << std::endl;
			if (session.hasError()) {
				clog << agocontrol::kLogCrit << "Session has error, recreating" << std::endl;
				session.close();
				session = connection.createSession();
				receiver = session.createReceiver("iris; {create: always, node: {type: topic}}");
				sender = session.createSender("iris; {create: always, node: {type: topic}}");
			}

			usleep(50);
		}
	}
}
bool agocontrol::AgoConnection::emitDeviceAnnounce(const char *internalId, const char *deviceType) {
	Variant::Map content;
	Message event;

	content["devicetype"] = deviceType;
	content["internalid"] = internalId;
	content["handled-by"] = instance;
	content["uuid"] = internalIdToUuid(internalId);
	encode(content, event);
	event.setSubject("event.device.announce");
	try {
		sender.send(event);
	} catch(const std::exception& error) {
		std::cerr << error.what() << std::endl;
		return false;
	}
	return true;
}

bool agocontrol::AgoConnection::emitDeviceRemove(const char *internalId) {
	Variant::Map content;
	Message event;

	content["uuid"] = internalIdToUuid(internalId);
	encode(content, event);
	event.setSubject("event.device.remove");
	try {
		sender.send(event);
	} catch(const std::exception& error) {
		std::cerr << error.what() << std::endl;
		return false;
	}
	return true;
}
bool agocontrol::AgoConnection::addDevice(const char *internalId, const char *deviceType) {
	if (internalIdToUuid(internalId).size()==0) {
		// need to generate new uuid
		uuidMap[generateUuid()] = internalId;
		storeUuidMap();
	}
	Variant::Map device;
	device["devicetype"] = deviceType;
	device["internalid"] = internalId;
	deviceMap[internalIdToUuid(internalId)] = device;
	emitDeviceAnnounce(internalId, deviceType);
	return true;
}

bool agocontrol::AgoConnection::removeDevice(const char *internalId) {
	if (internalIdToUuid(internalId).size()!=0) {
		emitDeviceRemove(internalId);
		Variant::Map::const_iterator it = deviceMap.find(internalIdToUuid(internalId));
		if (it != deviceMap.end()) deviceMap.erase(it->first);
		// deviceMap[internalIdToUuid(internalId)] = device;
		return true;
	} else return false;
}

std::string agocontrol::AgoConnection::uuidToInternalId(std::string uuid) {
	return uuidMap[uuid].asString();
}

std::string agocontrol::AgoConnection::internalIdToUuid(std::string internalId) {
	string result;
	for (Variant::Map::const_iterator it = uuidMap.begin(); it != uuidMap.end(); ++it) {
		if (it->second.asString() == internalId) return it->first;
	}
	return result;
}

void agocontrol::AgoConnection::reportDevices() {
	for (Variant::Map::const_iterator it = deviceMap.begin(); it != deviceMap.end(); ++it) {
		Variant::Map device;
		Variant::Map content;
		Message event;

		// printf("uuid: %s\n", it->first.c_str());
		device = it->second.asMap();
		// printf("devicetype: %s\n", device["devicetype"].asString().c_str());
		emitDeviceAnnounce(device["internalid"].asString().c_str(), device["devicetype"].asString().c_str());
	}
}

bool agocontrol::AgoConnection::storeUuidMap() {
	ofstream mapfile;
	mapfile.open(uuidMapFile.c_str());
	mapfile << variantMapToJSONString(uuidMap);
	mapfile.close();
	return true;
}

bool agocontrol::AgoConnection::loadUuidMap() {
	string content;
	ifstream mapfile (uuidMapFile.c_str());
	if (mapfile.is_open()) {
		while (mapfile.good()) {
			string line;
			getline(mapfile, line);
			content += line;
		}
		mapfile.close();
	}
	uuidMap = jsonStringToVariantMap(content);
	return true;
}

bool agocontrol::AgoConnection::addHandler(std::string (*handler)(qpid::types::Variant::Map)) {
	commandHandler = handler;
	return true;
}

bool agocontrol::AgoConnection::sendMessage(const char *subject, qpid::types::Variant::Map content) {
	Message message;

	try {
		encode(content, message);
		message.setSubject(subject);
		sender.send(message);
	} catch(const std::exception& error) {
		std::cerr << error.what() << std::endl;
		return false;
	}

	return true;
}

bool agocontrol::AgoConnection::sendMessage(qpid::types::Variant::Map content) {
	return sendMessage("",content);
}

bool agocontrol::AgoConnection::emitEvent(const char *internalId, const char *eventType, const char *level, const char *unit) {
	Variant::Map content;
	content["level"] = level;
	content["unit"] = unit;
	content["uuid"] = internalIdToUuid(internalId);

	printf("EventType: %s", eventType);
	printf("Level: %s", level);
	printf("Unit: %s", unit);


	return sendMessage(eventType, content);
}


string agocontrol::AgoConnection::getDeviceType(const char *internalId) {
	string uuid = internalIdToUuid(internalId);
	if (uuid.size() > 0) {
		Variant::Map device = deviceMap[internalIdToUuid(internalId)].asMap();
		return device["devicetype"];
	} else return "";

}
bool agocontrol::AgoConnection::setFilter(bool filter) {
	filterCommands = filter;
	return filterCommands;
}

bool agocontrol::AgoConnection::addEventHandler(void (*handler)(std::string, qpid::types::Variant::Map)) {
	eventHandler = handler;
	return true;
}

qpid::types::Variant::Map agocontrol::AgoConnection::getInventory() {
	Variant::Map content;
	Variant::Map responseMap;
	content["command"] = "inventory";
	Message message;
	encode(content, message);
	Address responseQueue("#response-queue; {create:always, delete:always}");
	Receiver responseReceiver = session.createReceiver(responseQueue);
	message.setReplyTo(responseQueue);
	sender.send(message);
	try {
		Message response = responseReceiver.fetch(Duration::SECOND * 3);
		if (response.getContentSize() > 3) {
			decode(response,responseMap);
		}
	} catch (qpid::messaging::NoMessageAvailable) {
		printf("WARNING, no reply message to fetch\n");
	}
	return responseMap;
}

agocontrol::Log::Log(std::string ident, int facility) {
    facility_ = facility;
    priority_ = LOG_DEBUG;
    strncpy(ident_, ident.c_str(), sizeof(ident_));
    ident_[sizeof(ident_)-1] = '\0';

    openlog(ident_, LOG_PID, facility_);
}

int agocontrol::Log::sync() {
    if (buffer_.length()) {
        syslog(priority_, buffer_.substr(0,buffer_.length()-1).c_str());
        buffer_.erase();
        priority_ = LOG_DEBUG; // default to debug for each message
    }
    return 0;
}

int agocontrol::Log::overflow(int c) {
    if (c != EOF) {
        buffer_ += static_cast<char>(c);
    } else {
        sync();
    }
    return c;
}

std::ostream& agocontrol::operator<< (std::ostream& os, const agocontrol::LogPriority& log_priority) {
    static_cast<agocontrol::Log *>(os.rdbuf())->priority_ = (int)log_priority;
    return os;
}

