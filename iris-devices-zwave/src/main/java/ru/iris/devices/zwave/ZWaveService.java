/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.iris.devices.zwave;

import com.avaje.ebean.Ebean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zwave4j.*;
import ru.iris.common.Config;
import ru.iris.common.Utils;
import ru.iris.common.database.model.SensorData;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;
import ru.iris.common.helpers.DBLogger;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.JsonNotification;
import ru.iris.common.messaging.model.devices.GenericAdvertisement;
import ru.iris.common.modulestatus.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZWaveService {
    private final Logger LOGGER = LogManager.getLogger(ZWaveService.class.getName());
    private final Gson gson = new GsonBuilder().create();
    private long homeId;
    private boolean ready = false;
    private JsonMessaging messaging;

    public ZWaveService() {
        Status status = new Status("ZWave");

        if (status.checkExist()) {
            status.running();
        } else {
            status.addIntoDB("ZWave", "Service that comminicate with ZWave devices");
        }

        try {

            Config config = Config.getInstance();

            messaging = new JsonMessaging(UUID.randomUUID(), "devices-zwave");

            NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

            final Options options = Options.create(config.get("openzwaveCfgPath"), "", "");
            options.addOptionBool("ConsoleOutput", Boolean.parseBoolean(config.get("zwaveDebug")));
            options.addOptionString("UserPath", "conf/", true);
            options.lock();

            final Manager manager = Manager.create();

            final NotificationWatcher watcher = new NotificationWatcher() {

                @Override
                public void onNotification(Notification notification, Object context) {

                    short node = notification.getNodeId();
                    Device device = null;
                    Map<String, Object> data = new HashMap<>();

                    switch (notification.getType()) {
                        case DRIVER_READY:
                            homeId = notification.getHomeId();
                            LOGGER.info("Driver ready. Home ID: " + homeId);
                            messaging.broadcast("event.devices.zwave.driver.ready", new GenericAdvertisement("ZWaveDriverReady", homeId));
                            break;
                        case DRIVER_FAILED:
                            LOGGER.info("Driver failed");
                            messaging.broadcast("event.devices.zwave.driver.failed", new GenericAdvertisement("ZWaveDriverFailed"));
                            break;
                        case DRIVER_RESET:
                            LOGGER.info("Driver reset");
                            messaging.broadcast("event.devices.zwave.driver.reset", new GenericAdvertisement("ZWaveDriverReset"));
                            break;
                        case AWAKE_NODES_QUERIED:
                            LOGGER.info("Awake nodes queried");
                            ready = true;
                            messaging.broadcast("event.devices.zwave.awakenodesqueried", new GenericAdvertisement("ZWaveAwakeNodesQueried"));
                            break;
                        case ALL_NODES_QUERIED:
                            LOGGER.info("All node queried");
                            manager.writeConfig(homeId);
                            ready = true;
                            messaging.broadcast("event.devices.zwave.allnodesqueried", new GenericAdvertisement("ZWaveAllNodesQueried"));
                            break;
                        case ALL_NODES_QUERIED_SOME_DEAD:
                            LOGGER.info("All node queried, some dead");
                            manager.writeConfig(homeId);
                            messaging.broadcast("event.devices.zwave.allnodesqueriedsomedead", new GenericAdvertisement("ZWaveAllNodesQueriedSomeDead"));
                            break;
                        case POLLING_ENABLED:
                            LOGGER.info("Polling enabled");
                            messaging.broadcast("event.devices.zwave.polling.enabled", new GenericAdvertisement("ZWavePollingEnabled", notification.getNodeId()));
                            break;
                        case POLLING_DISABLED:
                            LOGGER.info("Polling disabled");
                            messaging.broadcast("event.devices.zwave.polling.disabled", new GenericAdvertisement("ZWavePollingDisabled", notification.getNodeId()));
                            break;
                        case NODE_NEW:
                            messaging.broadcast("event.devices.zwave.node.new", new GenericAdvertisement("ZWaveNodeNew", notification.getNodeId()));
                            break;
                        case NODE_ADDED:
                            messaging.broadcast("event.devices.zwave.node.added", new GenericAdvertisement("ZWaveNodeAdded", notification.getNodeId()));
                            break;
                        case NODE_REMOVED:
                            messaging.broadcast("event.devices.zwave.node.removed", new GenericAdvertisement("ZWaveNodeRemoved", notification.getNodeId()));
                            break;
                        case ESSENTIAL_NODE_QUERIES_COMPLETE:
                            messaging.broadcast("event.devices.zwave.essentialnodequeriscomplete", new GenericAdvertisement("ZWaveEssentialsNodeQueriesComplete"));
                            break;
                        case NODE_QUERIES_COMPLETE:
                            messaging.broadcast("event.devices.zwave.queriescomplete", new GenericAdvertisement("ZWaveNodeQueriesComplete"));
                            break;
                        case NODE_EVENT:
                            LOGGER.info("Update info for node " + node);
                            manager.refreshNodeInfo(homeId, node);
                            messaging.broadcast("event.devices.zwave.node.event", new GenericAdvertisement("ZWaveNodeEvent", notification.getNodeId()));
                            break;
                        case NODE_NAMING:
                            messaging.broadcast("event.devices.zwave.node.naming", new GenericAdvertisement("ZWaveNodeNaming", notification.getNodeId()));
                            break;
                        case NODE_PROTOCOL_INFO:
                            messaging.broadcast("event.devices.zwave.node.protocolinfo", new GenericAdvertisement("ZWaveNodeProtocolInfo", notification.getNodeId()));
                            break;
                        case VALUE_ADDED:

                            // check empty label
                            if (Manager.get().getValueLabel(notification.getValueId()).isEmpty())
                                break;

                            String nodeType = manager.getNodeType(homeId, node);

                            switch (nodeType) {
                                case "Portable Remote Controller":
                                    device = addZWaveDeviceOrValue("controller", notification);
                                    break;

                                //////////////////////////////////

                                case "Multilevel Power Switch":
                                    device = addZWaveDeviceOrValue("dimmer", notification);
                                    break;

                                //////////////////////////////////

                                case "Routing Alarm Sensor":
                                    device = addZWaveDeviceOrValue("alarmsensor", notification);
                                    break;

                                case "Binary Power Switch":
                                    device = addZWaveDeviceOrValue("switch", notification);
                                    break;

                                case "Routing Binary Sensor":
                                    device = addZWaveDeviceOrValue("binarysensor", notification);
                                    break;

                                //////////////////////////////////

                                case "Routing Multilevel Sensor":
                                    device = addZWaveDeviceOrValue("multilevelsensor", notification);
                                    break;

                                //////////////////////////////////

                                case "Simple Meter":
                                    device = addZWaveDeviceOrValue("metersensor", notification);
                                    break;

                                //////////////////////////////////

                                case "Simple Window Covering":
                                    device = addZWaveDeviceOrValue("drapes", notification);
                                    break;

                                //////////////////////////////////

                                case "Setpoint Thermostat":
                                    device = addZWaveDeviceOrValue("thermostat", notification);
                                    break;

                                //////////////////////////////////
                                //////////////////////////////////

                                default:
                                    LOGGER.info("Unassigned value for node" +
                                                    node +
                                                    " type " +
                                                    manager.getNodeType(notification.getHomeId(), notification.getNodeId()) +
                                                    " class " +
                                                    notification.getValueId().getCommandClassId() +
                                                    " genre " +
                                                    notification.getValueId().getGenre() +
                                                    " label " +
                                                    manager.getValueLabel(notification.getValueId()) +
                                                    " value " +
                                                    Utils.getValue(notification.getValueId()) +
                                                    " index " +
                                                    notification.getValueId().getIndex() +
                                                    " instance " +
                                                    notification.getValueId().getInstance()
                                    );

                                    data.put("uuid", device.getUuid());
                                    data.put("label", Manager.get().getValueLabel(notification.getValueId()));
                                    data.put("data", String.valueOf(Utils.getValue(notification.getValueId())));

                                    messaging.broadcast("event.devices.zwave.value.added", new GenericAdvertisement("ZWaveValueAdded", data));
                            }

                            // enable value polling TODO
                            //Manager.get().enablePoll(notification.getValueId());

                            break;
                        case VALUE_REMOVED:

                            device = Device.getDeviceByNode(node);

                            if (device == null) {
                                LOGGER.info("While save remove value, node " + node + " not found");
                                break;
                            }

                            device.removeValue(manager.getValueLabel(notification.getValueId()));

                            data.put("uuid", device.getUuid());
                            data.put("label", Manager.get().getValueLabel(notification.getValueId()));
                            data.put("data", String.valueOf(Utils.getValue(notification.getValueId())));

                            messaging.broadcast("event.devices.zwave.value.removed", new GenericAdvertisement("ZWaveValueRemoved", data));

                            if (!manager.getValueLabel(notification.getValueId()).isEmpty()) {
                                LOGGER.info("Node " + device.getNode() + ": Value " + manager.getValueLabel(notification.getValueId()) + " removed");
                            }

                            break;
                        case VALUE_CHANGED:

                            device = Device.getDeviceByNode(node);

                            if (device == null) {
                                break;
                            }

                            // Check for awaked after sleeping nodes
                            if (manager.isNodeAwake(homeId, device.getNode()) && device.getStatus().equals("Sleeping")) {
                                LOGGER.info("Setting node " + device.getNode() + " to LISTEN state");
                                device.setStatus("Listening");
                            }

                            LOGGER.info("Node " +
                                    device.getNode() + ": " +
                                    "Value for label \"" + manager.getValueLabel(notification.getValueId()) + "\" changed --> " +
                                    "\"" + Utils.getValue(notification.getValueId()) + "\"");

                            DeviceValue udvChg = device.getValue(manager.getValueLabel(notification.getValueId()));

                            if (udvChg != null)
                                device.removeValue(udvChg);
                            else
                                udvChg = new DeviceValue();

                            udvChg.setLabel(manager.getValueLabel(notification.getValueId()));
                            udvChg.setValueType(Utils.getValueType(notification.getValueId()));
                            udvChg.setValueId(notification.getValueId());
                            udvChg.setValueUnits(Manager.get().getValueUnits(notification.getValueId()));
                            udvChg.setValue(String.valueOf(Utils.getValue(notification.getValueId())));
                            udvChg.setReadonly(Manager.get().isValueReadOnly(notification.getValueId()));

                            device.addValue(udvChg);
                            device.save();

                            DBLogger.info("Value " + manager.getValueLabel(notification.getValueId()) + " changed: " + Utils.getValue(notification.getValueId()), device.getUuid());
                            SensorData.log(device.getUuid(), Manager.get().getValueLabel(notification.getValueId()), String.valueOf(Utils.getValue(notification.getValueId())));

                            data.put("uuid", device.getUuid());
                            data.put("label", Manager.get().getValueLabel(notification.getValueId()));
                            data.put("data", String.valueOf(Utils.getValue(notification.getValueId())));

                            messaging.broadcast("event.devices.zwave.value.changed", new GenericAdvertisement("ZWaveValueChanged", data));

                            break;
                        case VALUE_REFRESHED:
                            LOGGER.info("Node " + node + ": Value refreshed (" +
                                    " command class: " + notification.getValueId().getCommandClassId() + ", " +
                                    " instance: " + notification.getValueId().getInstance() + ", " +
                                    " index: " + notification.getValueId().getIndex() + ", " +
                                    " value: " + Utils.getValue(notification.getValueId()));
                            break;
                        case GROUP:
                            break;
                        case SCENE_EVENT:
                            break;
                        case CREATE_BUTTON:
                            break;
                        case DELETE_BUTTON:
                            break;
                        case BUTTON_ON:
                            break;
                        case BUTTON_OFF:
                            break;
                        case NOTIFICATION:
                            break;
                        default:
                            LOGGER.info(notification.getType().name());
                            break;
                    }
                }
            };

            manager.addWatcher(watcher, null);
            manager.addDriver(config.get("zwavePort"));

            LOGGER.info("Waiting while ZWave finish initialization");

            // Ждем окончания инициализации
            while (!ready) {
                Thread.sleep(1000);
                LOGGER.info("Still waiting");
            }

            // set polling interval 60 sec TODO
            //Manager.get().setPollInterval(60000, true);

            for (Device ZWaveDevice : Ebean.find(Device.class).where().eq("source", "zwave").findList()) {
                // Check for dead nodes
                if (Manager.get().isNodeFailed(homeId, ZWaveDevice.getNode())) {
                    LOGGER.info("Setting node " + ZWaveDevice.getNode() + " to DEAD state");
                    ZWaveDevice.setStatus("dead");
                }

                // Check for sleeping nodes
                if (!Manager.get().isNodeAwake(homeId, ZWaveDevice.getNode())) {
                    LOGGER.info("Setting node " + ZWaveDevice.getNode() + " to SLEEP state");
                    ZWaveDevice.setStatus("sleeping");

                    Manager.get().refreshNodeInfo(homeId, ZWaveDevice.getNode());
                }

                ZWaveDevice.save();
            }

            LOGGER.info("Initialization complete.");

            messaging.subscribe("event.devices.setvalue");
            messaging.subscribe("event.devices.zwave.value.set");
            messaging.subscribe("event.devices.zwave.node.add");
            messaging.subscribe("event.devices.zwave.node.remove");
            messaging.subscribe("event.devices.zwave.cancel");
            messaging.start();

            messaging.setNotification(new JsonNotification() {

                @Override
                public void onNotification(JsonEnvelope envelope) {

                    if (envelope.getObject() instanceof GenericAdvertisement) {

                        GenericAdvertisement advertisement = envelope.getObject();

                        switch (advertisement.getLabel()) {

                            case "DeviceOn":
                                deviceSetLevel(advertisement);
                                break;

                            case "DeviceOff":
                                deviceSetLevel(advertisement);
                                break;

                            case "DeviceSetLevel":
                                deviceSetLevel(advertisement);
                                break;

                            case "ZWaveAddNode":
                                LOGGER.info("Set controller into AddDevice mode");
                                Manager.get().beginControllerCommand(homeId, ControllerCommand.ADD_DEVICE, new CallbackListener(ControllerCommand.ADD_DEVICE), null, true);
                                break;
                            case "ZWaveRemoveNode":
                                LOGGER.info("Set controller into RemoveDevice mode");
                                Manager.get().beginControllerCommand(homeId, ControllerCommand.REMOVE_DEVICE, new CallbackListener(ControllerCommand.REMOVE_DEVICE), null, true, (short) advertisement.getFirstData());
                                break;
                            case "ZWaveCancelCommand":
                                LOGGER.info("Canceling controller command");
                                Manager.get().cancelControllerCommand(homeId);
                                break;
                        }

                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        LOGGER.info("Received request "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            });

            // Close JSON messaging.
            messaging.start();
        } catch (final Throwable t) {
            LOGGER.error("Error in ZWave: " + t);
            status.crashed();
        }
    }

    private void setTypedValue(ValueId valueId, String value) {

        LOGGER.debug("Set type " + valueId.getType() + " to label " + Manager.get().getValueLabel(valueId));

        switch (valueId.getType()) {
            case BOOL:
                LOGGER.debug("Set value type BOOL to " + value);
                Manager.get().setValueAsBool(valueId, Boolean.valueOf(value));
                break;
            case BYTE:
                LOGGER.debug("Set value type BYTE to " + value);
                Manager.get().setValueAsByte(valueId, Short.valueOf(value));
                break;
            case DECIMAL:
                LOGGER.debug("Set value type FLOAT to " + value);
                Manager.get().setValueAsFloat(valueId, Float.valueOf(value));
                break;
            case INT:
                LOGGER.debug("Set value type INT to " + value);
                Manager.get().setValueAsInt(valueId, Integer.valueOf(value));
                break;
            case LIST:
                LOGGER.debug("Set value type LIST to " + value);
                break;
            case SCHEDULE:
                LOGGER.debug("Set value type SCHEDULE to " + value);
                break;
            case SHORT:
                LOGGER.debug("Set value type SHORT to " + value);
                Manager.get().setValueAsShort(valueId, Short.valueOf(value));
                break;
            case STRING:
                LOGGER.debug("Set value type STRING to " + value);
                Manager.get().setValueAsString(valueId, value);
                break;
            case BUTTON:
                LOGGER.debug("Set value type BUTTON to " + value);
                break;
            case RAW:
                LOGGER.debug("Set value RAW to " + value);
                break;
            default:
                break;
        }
    }

    private void setValue(String uuid, String label, String value) {
        Device device = Device.getDeviceByUUID(uuid);
        DeviceValue zv = device.getValue(label);

        if (zv != null) {
            if (!Manager.get().isValueReadOnly(gson.fromJson(zv.getValueId(), ValueId.class))) {
                setTypedValue(gson.fromJson(zv.getValueId(), ValueId.class), value);
            } else {
                LOGGER.info("Value \"%s\" is read-only! Skip.", label);
            }
        }
    }

    private Device addZWaveDeviceOrValue(String type, Notification notification) {

        Device ZWaveDevice;
        String label = Manager.get().getValueLabel(notification.getValueId());
        String state = "not responding";
        String productName = Manager.get().getNodeProductName(notification.getHomeId(), notification.getNodeId());
        String manufName = Manager.get().getNodeManufacturerName(notification.getHomeId(), notification.getNodeId());

        if (Manager.get().requestNodeState(homeId, notification.getNodeId())) {
            state = "listening";
        }

        if ((ZWaveDevice = Ebean.find(Device.class).where().eq("internalname", "zwave/" + type + "/" + notification.getNodeId()).findUnique()) == null) {

            String uuid = UUID.randomUUID().toString();

            ZWaveDevice = new Device();

            ZWaveDevice.setInternalType(type);
            ZWaveDevice.setSource("zwave");
            ZWaveDevice.setInternalName("zwave/" + type + "/" + notification.getNodeId());
            ZWaveDevice.setType(Manager.get().getNodeType(notification.getHomeId(), notification.getNodeId()));
            ZWaveDevice.setNode(notification.getNodeId());
            ZWaveDevice.setUuid(uuid);
            ZWaveDevice.setManufName(manufName);
            ZWaveDevice.setProductName(productName);
            ZWaveDevice.setStatus(state);

            ZWaveDevice.addValue(new DeviceValue(
                    label,
                    uuid,
                    String.valueOf(Utils.getValue(notification.getValueId())),
                    Utils.getValueType(notification.getValueId()),
                    Manager.get().getValueUnits(notification.getValueId()),
                    notification.getValueId(),
                    Manager.get().isValueReadOnly(notification.getValueId())
            ));

            // Check if it is beaming device
            DeviceValue beaming = new DeviceValue();

            beaming.setLabel("beaming");
            beaming.setValueId("{ }");
            beaming.setValue(String.valueOf(Manager.get().isNodeBeamingDevice(homeId, ZWaveDevice.getNode())));
            beaming.setReadonly(true);

            ZWaveDevice.addValue(beaming);

            ZWaveDevice.save();

            LOGGER.info("Adding device " + type + " (node: " + notification.getNodeId() + ") to system");
        } else {
            ZWaveDevice.setManufName(manufName);
            ZWaveDevice.setProductName(productName);
            ZWaveDevice.setStatus(state);

            // check empty label
            if (label.isEmpty())
                return ZWaveDevice;

            LOGGER.info("Node " + ZWaveDevice.getNode() + ": Add \"" + label + "\" value \"" + Utils.getValue(notification.getValueId()) + "\"");

            DeviceValue udv = ZWaveDevice.getValue(label);

            // remove
            if (udv != null) {
                ZWaveDevice.removeValue(udv);
            } else {
                udv = new DeviceValue();
            }

            udv.setLabel(label);
            udv.setValueType(Utils.getValueType(notification.getValueId()));
            udv.setValueId(notification.getValueId());
            udv.setValueUnits(Manager.get().getValueUnits(notification.getValueId()));
            udv.setValue(String.valueOf(Utils.getValue(notification.getValueId())));
            udv.setReadonly(Manager.get().isValueReadOnly(notification.getValueId()));

            ZWaveDevice.addValue(udv);
            ZWaveDevice.save();
        }

        return ZWaveDevice;
    }

    private void deviceSetLevel(GenericAdvertisement advertisement) {
        String level = (String) advertisement.getValue("data");
        String label = (String) advertisement.getValue("label");
        String uuid = (String) advertisement.getValue("uuid");

        Device ZWaveDevice = Device.getDeviceByUUID(uuid);

        if (ZWaveDevice != null && !ZWaveDevice.getSource().equals("zwave")) {
            // not zwave
            return;
        }

        if (ZWaveDevice == null) {
            LOGGER.info("Cant find device with UUID " + uuid);
            return;
        }

        int node = ZWaveDevice.getNode();

        if (!label.isEmpty() && !level.isEmpty() && !ZWaveDevice.getStatus().equals("Dead")) {
            LOGGER.info("Setting value: " + level + " to label \"" + label + "\" on node " + node + " (UUID: " + uuid + ")");
            setValue(uuid, label, level);
        } else {
            LOGGER.info("Node: " + node + " Cant set empty value or node dead");
        }
    }

    private class CallbackListener implements ControllerCallback {
        private ControllerCommand ctl;

        public CallbackListener(ControllerCommand ctl) {
            this.ctl = ctl;
        }

        @Override
        public void onCallback(ControllerState state, ControllerError err, Object context) {
            LOGGER.debug("ZWave Command Callback: {} , {}", state, err);

            if (ctl == ControllerCommand.REMOVE_DEVICE && state == ControllerState.COMPLETED) {
                LOGGER.info("Remove ZWave device from network");
                Manager.get().softReset(homeId);
                Manager.get().testNetwork(homeId, 5);
                Manager.get().healNetwork(homeId, true);
            }

            if (ctl == ControllerCommand.ADD_DEVICE && state == ControllerState.COMPLETED) {
                LOGGER.info("Add ZWave device to network");
                Manager.get().testNetwork(homeId, 5);
                Manager.get().healNetwork(homeId, true);
            }
        }
    }
}
