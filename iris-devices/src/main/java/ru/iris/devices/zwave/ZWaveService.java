package ru.iris.devices.zwave;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.qpid.AMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwave4j.*;
import ru.iris.common.I18N;
import ru.iris.common.Utils;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.*;
import ru.iris.common.messaging.model.zwave.*;
import ru.iris.devices.Service;

import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class ZWaveService implements Runnable {

    private static Logger log = LoggerFactory.getLogger(ZWaveService.class.getName());
    private static long homeId;
    private static boolean ready = false;
    private static HashMap<String, ZWaveDevice> zDevices = new HashMap<>();
    private static final I18N i18n = new I18N();
    private boolean initComplete = false;
    private boolean shutdown = false;
    private static JsonMessaging messaging;
    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    public ZWaveService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        try {
            messaging = new JsonMessaging(UUID.randomUUID());
        } catch (JMSException | URISyntaxException | AMQException e) {
            e.printStackTrace();
        }

        NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

        final Options options = Options.create(Service.config.get("openzwaveCfgPath"), "", "");
        options.addOptionBool("ConsoleOutput", Boolean.parseBoolean(Service.config.get("zwaveDebug")));
        options.addOptionString("UserPath", "conf/", true);
        options.lock();

        final Manager manager = Manager.create();

        final NotificationWatcher watcher = new NotificationWatcher() {

            @Override
            public void onNotification(Notification notification, Object context) {

                short node = notification.getNodeId();

                switch (notification.getType()) {
                    case DRIVER_READY:
                        log.info(i18n.message("zwave.driver.ready.home.id.0", homeId));
                        homeId = notification.getHomeId();
                        messaging.broadcast("event.devices.zwave.driver.ready", new ZWaveDriverReady(homeId));
                        break;
                    case DRIVER_FAILED:
                        log.info(i18n.message("zwave.driver.failed"));
                        messaging.broadcast("event.devices.zwave.driver.failed", new ZWaveDriverFailed());
                        break;
                    case DRIVER_RESET:
                        log.info(i18n.message("zwave.driver.reset"));
                        messaging.broadcast("event.devices.zwave.driver.reset", new ZWaveDriverReset());
                        break;
                    case AWAKE_NODES_QUERIED:
                        log.info(i18n.message("zwave.awake.nodes.queried"));
                        ready = true;
                        manager.writeConfig(homeId);
                        messaging.broadcast("event.devices.zwave.awakenodesqueried", new ZWaveAwakeNodesQueried());
                        break;
                    case ALL_NODES_QUERIED:
                        log.info(i18n.message("zwave.all.nodes.queried"));
                        manager.writeConfig(homeId);
                        ready = true;
                        messaging.broadcast("event.devices.zwave.allnodesqueried", new ZWaveAllNodesQueried());
                        break;
                    case ALL_NODES_QUERIED_SOME_DEAD:
                        log.info(i18n.message("zwave.all.nodes.queried.some.dead"));
                        manager.writeConfig(homeId);
                        messaging.broadcast("event.devices.zwave.allnodesqueriedsomedead", new ZWaveAllNodesQueriedSomeDead());
                        break;
                    case POLLING_ENABLED:
                        log.info(i18n.message("zwave.polling.enabled"));
                        messaging.broadcast("event.devices.zwave.polling.disabled", new ZWavePolling(getZWaveDeviceByNode(notification.getNodeId()), true));
                        break;
                    case POLLING_DISABLED:
                        log.info(i18n.message("zwave.polling.disabled"));
                        messaging.broadcast("event.devices.zwave.polling.enabled", new ZWavePolling(getZWaveDeviceByNode(notification.getNodeId()), false));
                        break;
                    case NODE_NEW:
                        messaging.broadcast("event.devices.zwave.node.new", new ZWaveNodeNew(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_ADDED:
                        messaging.broadcast("event.devices.zwave.node.added", new ZWaveNodeAdded(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_REMOVED:
                        messaging.broadcast("event.devices.zwave.node.removed", new ZWaveNodeRemoved(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case ESSENTIAL_NODE_QUERIES_COMPLETE:
                        messaging.broadcast("event.devices.zwave.essentialnodequeriscomplete", new ZWaveEssentialNodeQueriesComplete());
                        break;
                    case NODE_QUERIES_COMPLETE:
                        messaging.broadcast("event.devices.zwave.node.queriescomplete", new ZWaveNodeQueriesComplete());
                        break;
                    case NODE_EVENT:
                        log.info(i18n.message("zwave.update.info.for.node.0", node));
                        manager.refreshNodeInfo(homeId, node);
                        messaging.broadcast("event.devices.zwave.node.event", new ZWaveNodeEvent(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_NAMING:
                        messaging.broadcast("event.devices.zwave.node.naming", new ZWaveNodeNaming(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_PROTOCOL_INFO:
                        messaging.broadcast("event.devices.zwave.node.protocolinfo", new ZWaveNodeProtocolInfo(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case VALUE_ADDED:

                        String nodeType = manager.getNodeType(homeId, node);
                        ZWaveDevice zw;

                        switch (nodeType) {
                            case "Portable Remote Controller":

                                zw = addZWaveDeviceOrValue("controller", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Multilevel Power Switch":

                                zw = addZWaveDeviceOrValue("dimmer", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Routing Alarm Sensor":

                                zw = addZWaveDeviceOrValue("alarmsensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            case "Binary Power Switch":

                                zw = addZWaveDeviceOrValue("switch", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            case "Routing Binary Sensor":

                                zw = addZWaveDeviceOrValue("binarysensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Routing Multilevel Sensor":

                                zw = addZWaveDeviceOrValue("multilevelsensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Simple Meter":

                                zw = addZWaveDeviceOrValue("metersensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Simple Window Covering":

                                zw = addZWaveDeviceOrValue("drapes", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Setpoint Thermostat":

                                zw = addZWaveDeviceOrValue("thermostat", notification);
                                messaging.broadcast("event.devices.value.added",
                                        new ZWaveDeviceValueAdded(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////
                            //////////////////////////////////

                            default:
                                log.info(i18n.message("zwave.unassigned.value.node.0.type.1.class.2.genre.3.label.4.value.5.index.6.instance.7",
                                        node,
                                        manager.getNodeType(notification.getHomeId(), notification.getNodeId()),
                                        notification.getValueId().getCommandClassId(),
                                        notification.getValueId().getGenre(),
                                        manager.getValueLabel(notification.getValueId()),
                                        Utils.getValue(notification.getValueId()),
                                        notification.getValueId().getIndex(),
                                        notification.getValueId().getInstance())
                                );
                        }

                        break;
                    case VALUE_REMOVED:

                        ZWaveDevice zrZWaveDevice = getZWaveDeviceByNode(node);

                        if (zrZWaveDevice == null) {
                            log.info(i18n.message("zwave.error.while.save.value.remove.cannot.find.device.with.node.id.0", node));
                            break;
                        }

                        zrZWaveDevice.removeValueID(manager.getValueLabel(notification.getValueId()));

                        messaging.broadcast("event.devices.value.removed",
                                new ZWaveDeviceValueRemoved(
                                        zrZWaveDevice,
                                        Manager.get().getValueLabel(notification.getValueId()),
                                        String.valueOf(Utils.getValue(notification.getValueId()))));

                        if (!manager.getValueLabel(notification.getValueId()).isEmpty())
                            log.info(i18n.message("zwave.node.0.value.1.removed", zrZWaveDevice.getNode(), manager.getValueLabel(notification.getValueId())));

                        break;
                    case VALUE_CHANGED:

                        ZWaveDevice zcZWaveDevice = getZWaveDeviceByNode(node);

                        // Check for awaked after sleeping nodes
                        if (manager.isNodeAwake(homeId, zcZWaveDevice.getNode()) && zcZWaveDevice.getStatus().equals("Sleeping")) {
                            log.info("Setting node " + zcZWaveDevice.getNode() + " to LISTEN state");
                            zcZWaveDevice.setStatus("Listening");
                            try {
                                zcZWaveDevice.save();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }

                        // Check for sleeping after awake nodes
                        if (!manager.isNodeAwake(homeId, zcZWaveDevice.getNode()) && zcZWaveDevice.getStatus().equals("Listening")) {
                            log.info("Setting node " + zcZWaveDevice.getNode() + " to SLEEP state");
                            zcZWaveDevice.setStatus("Sleeping");
                            try {
                                zcZWaveDevice.save();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }

                        if (zcZWaveDevice == null) {
                            log.info(i18n.message("zwave.error.while.save.value.change.cannot.find.device.with.node.id.0", node));
                            break;
                        }

                        // break if same value
                        try {
                            if (Utils.getValue((ValueId) zcZWaveDevice.getValue(manager.getValueLabel(notification.getValueId()))) == Utils.getValue(notification.getValueId()))
                                break;
                        } catch (NullPointerException e) {
                            break;
                        }

                        log.info(i18n.message("zwave.node.0.value.for.label.1.changed.2.3",
                                zcZWaveDevice.getNode(),
                                manager.getValueLabel(notification.getValueId()),
                                Utils.getValue((ValueId) zcZWaveDevice.getValue(manager.getValueLabel(notification.getValueId()))),
                                Utils.getValue(notification.getValueId())));

                        zcZWaveDevice.updateValueID(manager.getValueLabel(notification.getValueId()), notification.getValueId());

                        messaging.broadcast("event.devices.value.changed",
                                new ZWaveDeviceValueChanged(
                                        zcZWaveDevice,
                                        Manager.get().getValueLabel(notification.getValueId()),
                                        String.valueOf(Utils.getValue(notification.getValueId()))));

                        break;
                    case VALUE_REFRESHED:
                        log.info(i18n.message("value.refreshed.node.id.0.command.class.1.instance.2.index.3.value.4",
                                node,
                                notification.getValueId().getCommandClassId(),
                                notification.getValueId().getInstance(),
                                notification.getValueId().getIndex(),
                                Utils.getValue(notification.getValueId())));
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
                        log.info(notification.getType().name());
                        break;
                }
            }
        };

        manager.addWatcher(watcher, null);
        manager.addDriver(Service.config.get("zwavePort"));

        log.info(i18n.message("zwave.waiting.ready.state.from.zwave"));

        // Ждем окончания инициализации
        while (!ready) {
            try {
                Thread.sleep(1000);
                log.info(i18n.message("zwave.still.waiting"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info(i18n.message("zwave.initialization.complete.found.0.device.s", zDevices.size()));


        Service.serviceChecker.setAdvertisment(new ServiceAdvertisement("Devices", Service.serviceId, ServiceStatus.AVAILABLE,
                new ServiceCapability[]{ServiceCapability.CONTROL, ServiceCapability.SENSE}));


        for (ZWaveDevice ZWaveDevice : zDevices.values()) {
            try {

                // Check for dead nodes
                if (manager.isNodeFailed(homeId, ZWaveDevice.getNode())) {
                    log.info("Setting node " + ZWaveDevice.getNode() + " to DEAD state");
                    ZWaveDevice.setStatus("Dead");
                }

                // Check for sleeping nodes
                if (!manager.isNodeAwake(homeId, ZWaveDevice.getNode())) {
                    log.info("Setting node " + ZWaveDevice.getNode() + " to SLEEP state");
                    ZWaveDevice.setStatus("Sleeping");
                }

                ZWaveDevice.save();
                initComplete = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

            jsonMessaging.subscribe("event.devices.setvalue");
            jsonMessaging.subscribe("event.devices.getinventory");

            jsonMessaging.start();

            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof SetDeviceLevelAdvertisement) {
                        // We know of service advertisement
                        final SetDeviceLevelAdvertisement advertisement = envelope.getObject();

                        String uuid = advertisement.getDeviceUUID();
                        String label = advertisement.getLabel();
                        String level = advertisement.getValue();

                        ZWaveDevice ZWaveDevice = getZWaveDeviceByUUID(uuid);

                        if (ZWaveDevice == null) {
                            log.info(i18n.message("zwave.cannot.find.device.with.uuid.0", uuid));
                            continue;
                        }

                        int node = ZWaveDevice.getNode();

                        if (!label.isEmpty() && !level.isEmpty() && !ZWaveDevice.getStatus().equals("Dead")) {
                            log.info("Setting value: " + level + " to label \"" + label + "\" on node " + node + " (UUID: " + uuid + ")");
                            setValue(uuid, label, level);
                        } else {
                            log.info("Node: " + node + " Cant set empty value or node dead");
                        }

                    } else if (envelope.getObject() instanceof GetInventoryAdvertisement) {

                        final GetInventoryAdvertisement advertisement = envelope.getObject();

                        ArrayList<String> inventory = new ArrayList<>();

                        if (advertisement.getDeviceUUID().equals("all")) {

                            Iterator it = zDevices.entrySet().iterator();
                            while (it.hasNext()) {

                                Map.Entry pairs = (Map.Entry) it.next();

                                ZWaveDevice zwd = (ZWaveDevice) pairs.getValue();
                                JsonElement jsonElement = gson.toJsonTree(zwd);
                                JsonObject jsValues = new JsonObject();

                                Iterator itDigits = zwd.getValueIDs().entrySet().iterator();
                                while (itDigits.hasNext()) {

                                    Map.Entry pair = (Map.Entry) itDigits.next();

                                    String olabel = String.valueOf(pair.getKey());
                                    ValueId ovalue = (ValueId) pair.getValue();

                                    jsValues.addProperty(olabel, String.valueOf(Utils.getValue(ovalue)));
                                }

                                jsonElement.getAsJsonObject().add("values", jsValues);

                                inventory.add(gson.toJson(jsonElement));
                            }

                            jsonMessaging.broadcast("event.devices.responseinventory", new ResponseZWaveDeviceArrayInventoryAdvertisement(inventory));

                        } else {
                            ZWaveDevice zdv = getZWaveDeviceByUUID(advertisement.getDeviceUUID());
                            if (zdv != null) {
                                JsonElement jsonElement = gson.toJsonTree(zdv);
                                JsonObject jsValues = new JsonObject();

                                Iterator itDigits = zdv.getValueIDs().entrySet().iterator();
                                while (itDigits.hasNext()) {

                                    Map.Entry pair = (Map.Entry) itDigits.next();

                                    String olabel = String.valueOf(pair.getKey());
                                    ValueId ovalue = (ValueId) pair.getValue();

                                    jsValues.addProperty(olabel, String.valueOf(Utils.getValue(ovalue)));
                                }

                                jsonElement.getAsJsonObject().add("values", jsValues);

                                jsonMessaging.broadcast("event.devices.responseinventory", new ResponseZWaveDeviceInventoryAdvertisement(jsonElement));
                            }
                        }

                    } else if (envelope.getReceiverInstanceId() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        log.info("Received broadcast "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.info("Received request "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            }

            // Broadcast that this service is shutdown.
            Service.serviceChecker.setAdvertisment(new ServiceAdvertisement(
                    "Devices", Service.serviceId, ServiceStatus.SHUTDOWN,
                    new ServiceCapability[]{ServiceCapability.SYSTEM}));

            // Close JSON messaging.
            jsonMessaging.close();
            messaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in ZWave Devices", t);
        }
    }

    private void setTypedValue(ValueId valueId, String value) {

        log.debug("Set type " + valueId.getType() + " to label " + Manager.get().getValueLabel(valueId));

        switch (valueId.getType()) {
            case BOOL:
                log.debug("Set value type BOOL to " + value);
                Manager.get().setValueAsBool(valueId, Boolean.valueOf(value));
                break;
            case BYTE:
                log.debug("Set value type BYTE to " + value);
                Manager.get().setValueAsByte(valueId, Short.valueOf(value));
                break;
            case DECIMAL:
                log.debug("Set value type FLOAT to " + value);
                Manager.get().setValueAsFloat(valueId, Float.valueOf(value));
                break;
            case INT:
                log.debug("Set value type INT to " + value);
                Manager.get().setValueAsInt(valueId, Integer.valueOf(value));
                break;
            case LIST:
                log.debug("Set value type LIST to " + value);
                break;
            case SCHEDULE:
                log.debug("Set value type SCHEDULE to " + value);
                break;
            case SHORT:
                log.debug("Set value type SHORT to " + value);
                Manager.get().setValueAsShort(valueId, Short.valueOf(value));
                break;
            case STRING:
                log.debug("Set value type STRING to " + value);
                Manager.get().setValueAsString(valueId, value);
                break;
            case BUTTON:
                log.debug("Set value type BUTTON to " + value);
                break;
            case RAW:
                log.debug("Set value RAW to " + value);
                break;
            default:
                break;
        }
    }

    private void setValue(String uuid, String label, String value) {
        ZWaveDevice device = getZWaveDeviceByUUID(uuid);
        HashMap<String, Object> valueIDs = device.getValueIDs();

        Iterator it = valueIDs.entrySet().iterator();

        if (!it.hasNext())
            log.info("ValueID set is empty!");

        while (it.hasNext()) {

            Map.Entry pairs = (Map.Entry) it.next();
            ValueId valueId = (ValueId) pairs.getValue();

            if (Manager.get().getValueLabel(valueId).equals(label)) {
                if (!Manager.get().isValueReadOnly(valueId)) {
                    setTypedValue(valueId, String.valueOf(value));
                } else {
                    log.info("Value \"" + label + "\" is read-only! Skip.");
                }
            }
        }
    }

    private ZWaveDevice hasInstance(String key) {
        HashMap zDv = (HashMap) zDevices.clone();

        Iterator it = zDv.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry pairs = (Map.Entry) it.next();

            if (key.equals(pairs.getKey())) {
                return (ZWaveDevice) pairs.getValue();
            }

            it.remove(); // avoids a ConcurrentModificationException
        }

        return null;
    }

    private ZWaveDevice getZWaveDeviceByUUID(String uuid) {
        for (ZWaveDevice ZWaveDevice : zDevices.values()) {
            if (ZWaveDevice.getUUID().equals(uuid))
                return ZWaveDevice;
        }
        return null;
    }

    private ZWaveDevice getZWaveDeviceByNode(short id) {
        for (ZWaveDevice ZWaveDevice : zDevices.values()) {
            if (ZWaveDevice.getNode() == id) {
                return ZWaveDevice;
            }
        }
        return null;
    }

    private ZWaveDevice addZWaveDeviceOrValue(String type, Notification notification) {

        ZWaveDevice ZWaveDevice;
        String label = Manager.get().getValueLabel(notification.getValueId());
        String state = i18n.message("not.responding");
        String productName = Manager.get().getNodeProductName(notification.getHomeId(), notification.getNodeId());
        String manufName = Manager.get().getNodeManufacturerName(notification.getHomeId(), notification.getNodeId());

        if (Manager.get().requestNodeState(homeId, notification.getNodeId()))
            state = i18n.message("listening");

        if ((ZWaveDevice = hasInstance("zwave/" + type + "/" + notification.getNodeId())) == null) {

            String uuid = UUID.randomUUID().toString();

            try {
                ZWaveDevice = new ZWaveDevice();

                ZWaveDevice.setInternalType(type);
                ZWaveDevice.setInternalName("zwave/" + type + "/" + notification.getNodeId());
                ZWaveDevice.setType(Manager.get().getNodeType(notification.getHomeId(), notification.getNodeId()));
                ZWaveDevice.setNode(notification.getNodeId());
                ZWaveDevice.setUUID(uuid);
                ZWaveDevice.setManufName(manufName);
                ZWaveDevice.setProductName(productName);
                ZWaveDevice.setStatus(state);
                ZWaveDevice.setValueID(label, notification.getValueId());

            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }

            log.info(i18n.message("zwave.say.device.to.array.0.1", type, notification.getNodeId()));
            zDevices.put("zwave/" + type + "/" + notification.getNodeId(), ZWaveDevice);
        } else {

            ZWaveDevice.setManufName(manufName);
            ZWaveDevice.setProductName(productName);
            ZWaveDevice.setStatus(state);

            log.info(i18n.message("zwave.node.0.say.value.to.device.1.2", ZWaveDevice.getNode(), label, Utils.getValue(notification.getValueId())));
            ZWaveDevice.setValueID(label, notification.getValueId());
        }

        // catch and save into database value changes after init complete
        try {
            if (initComplete)
                ZWaveDevice.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ZWaveDevice;
    }
}
