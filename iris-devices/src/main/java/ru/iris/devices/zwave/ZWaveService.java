package ru.iris.devices.zwave;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwave4j.*;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.SQL;
import ru.iris.common.Utils;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.common.devices.ZWaveDeviceValue;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.*;
import ru.iris.common.messaging.model.zwave.*;
import ru.iris.devices.Service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class ZWaveService implements Runnable {

    private Logger log = LoggerFactory.getLogger(ZWaveService.class.getName());
    private long homeId;
    private boolean ready = false;
    private static final Map<String, ZWaveDevice> zDevices = new HashMap<>();
    private final I18N i18n = new I18N();
    private boolean initComplete = false;
    private boolean shutdown = false;
    private JsonMessaging messaging;
    private Map<String, String> config;
    private SQL sql = new SQL();
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    // Adverstiments
    private ZWaveDriverReady zWaveDriverReady = new ZWaveDriverReady();
    private ZWaveDriverFailed zWaveDriverFailed = new ZWaveDriverFailed();
    private ZWaveDriverReset zWaveDriverReset = new ZWaveDriverReset();
    private ZWaveNodeNaming zWaveNodeNaming = new ZWaveNodeNaming();
    private ZWaveNodeAdded zWaveNodeAdded = new ZWaveNodeAdded();
    private ZWaveNodeEvent zWaveNodeEvent = new ZWaveNodeEvent();
    private ZWaveNodeNew zWaveNodeNew = new ZWaveNodeNew();
    private ZWaveNodeProtocolInfo zWaveNodeProtocolInfo = new ZWaveNodeProtocolInfo();
    private ZWaveNodeQueriesComplete zWaveNodeQueriesComplete = new ZWaveNodeQueriesComplete();
    private ZWaveNodeRemoved zWaveNodeRemoved = new ZWaveNodeRemoved();
    private ZWaveAwakeNodesQueried zWaveAwakeNodesQueried = new ZWaveAwakeNodesQueried();
    private ZWaveAllNodesQueried zWaveAllNodesQueried = new ZWaveAllNodesQueried();
    private ZWaveAllNodesQueriedSomeDead zWaveAllNodesQueriedSomeDead = new ZWaveAllNodesQueriedSomeDead();
    private ZWaveDeviceValueAdded zWaveDeviceValueAdded = new ZWaveDeviceValueAdded();
    private ZWaveDeviceValueChanged zWaveDeviceValueChanged = new ZWaveDeviceValueChanged();
    private ZWaveDeviceValueRemoved zWaveDeviceValueRemoved = new ZWaveDeviceValueRemoved();
    private ZWaveEssentialNodeQueriesComplete zWaveEssentialNodeQueriesComplete = new ZWaveEssentialNodeQueriesComplete();
    private ZWavePolling zWavePolling = new ZWavePolling();
    private ResponseZWaveDeviceArrayInventoryAdvertisement responseZWaveDeviceArrayInventoryAdvertisement = new ResponseZWaveDeviceArrayInventoryAdvertisement();
    private ResponseZWaveDeviceInventoryAdvertisement responseZWaveDeviceInventoryAdvertisement = new ResponseZWaveDeviceInventoryAdvertisement();


    public ZWaveService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        messaging = new JsonMessaging(UUID.randomUUID());
        config = new Config().getConfig();

        ResultSet rs = sql.select("SELECT * FROM devices");

        try {
            while (rs.next()) {

                ZWaveDevice zDevice = new ZWaveDevice();

                zDevice.setManufName(rs.getString("manufname"));
                zDevice.setName(rs.getString("name"));
                zDevice.setNode(rs.getShort("node"));
                zDevice.setStatus(rs.getString("status"));
                zDevice.setInternalType(rs.getString("internaltype"));
                zDevice.setType(rs.getString("type"));
                zDevice.setUUID(rs.getString("uuid"));
                zDevice.setZone(rs.getInt("zone"));
                zDevice.setProductName(rs.getString("productname"));
                zDevice.setInternalName(rs.getString("internalname"));

                log.info(i18n.message("zwave.load.device.0.1.from.database", zDevice.getInternalType(), zDevice.getNode()));

                zDevices.put("zwave/" + zDevice.getInternalType() + "/" + zDevice.getNode(), zDevice);
            }

            rs.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

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

                switch (notification.getType()) {
                    case DRIVER_READY:
                        homeId = notification.getHomeId();
                        log.info(i18n.message("zwave.driver.ready.home.id.0", homeId));
                        messaging.broadcast("event.devices.zwave.driver.ready", zWaveDriverReady.set(homeId));
                        break;
                    case DRIVER_FAILED:
                        log.info(i18n.message("zwave.driver.failed"));
                        messaging.broadcast("event.devices.zwave.driver.failed", zWaveDriverFailed);
                        break;
                    case DRIVER_RESET:
                        log.info(i18n.message("zwave.driver.reset"));
                        messaging.broadcast("event.devices.zwave.driver.reset", zWaveDriverReset);
                        break;
                    case AWAKE_NODES_QUERIED:
                        log.info(i18n.message("zwave.awake.nodes.queried"));
                        ready = true;
                        messaging.broadcast("event.devices.zwave.awakenodesqueried", zWaveAwakeNodesQueried);
                        break;
                    case ALL_NODES_QUERIED:
                        log.info(i18n.message("zwave.all.nodes.queried"));
                        manager.writeConfig(homeId);
                        ready = true;
                        messaging.broadcast("event.devices.zwave.allnodesqueried", zWaveAllNodesQueried);
                        break;
                    case ALL_NODES_QUERIED_SOME_DEAD:
                        log.info(i18n.message("zwave.all.nodes.queried.some.dead"));
                        messaging.broadcast("event.devices.zwave.allnodesqueriedsomedead", zWaveAllNodesQueriedSomeDead);
                        break;
                    case POLLING_ENABLED:
                        log.info(i18n.message("zwave.polling.enabled"));
                        messaging.broadcast("event.devices.zwave.polling.disabled", zWavePolling.set(getZWaveDeviceByNode(notification.getNodeId()), true));
                        break;
                    case POLLING_DISABLED:
                        log.info(i18n.message("zwave.polling.disabled"));
                        messaging.broadcast("event.devices.zwave.polling.enabled", zWavePolling.set(getZWaveDeviceByNode(notification.getNodeId()), false));
                        break;
                    case NODE_NEW:
                        messaging.broadcast("event.devices.zwave.node.new", zWaveNodeNew.set(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_ADDED:
                        messaging.broadcast("event.devices.zwave.node.added", zWaveNodeAdded.set(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_REMOVED:
                        messaging.broadcast("event.devices.zwave.node.removed", zWaveNodeRemoved.set(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case ESSENTIAL_NODE_QUERIES_COMPLETE:
                        messaging.broadcast("event.devices.zwave.essentialnodequeriscomplete", zWaveEssentialNodeQueriesComplete);
                        break;
                    case NODE_QUERIES_COMPLETE:
                        messaging.broadcast("event.devices.zwave.node.queriescomplete", zWaveNodeQueriesComplete);
                        break;
                    case NODE_EVENT:
                        log.info(i18n.message("zwave.update.info.for.node.0", node));
                        manager.refreshNodeInfo(homeId, node);
                        messaging.broadcast("event.devices.zwave.node.event", zWaveNodeEvent.set(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_NAMING:
                        messaging.broadcast("event.devices.zwave.node.naming", zWaveNodeNaming.set(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case NODE_PROTOCOL_INFO:
                        messaging.broadcast("event.devices.zwave.node.protocolinfo", zWaveNodeProtocolInfo.set(getZWaveDeviceByNode(notification.getNodeId())));
                        break;
                    case VALUE_ADDED:

                        String nodeType = manager.getNodeType(homeId, node);
                        ZWaveDevice zw;

                        ZWaveDevice zcZWaveDevice = getZWaveDeviceByNode(node);

                        if (zcZWaveDevice != null) {
                            // Check for awaked after sleeping nodes
                            if (manager.isNodeAwake(homeId, zcZWaveDevice.getNode()) && zcZWaveDevice.getStatus().equals("Sleeping")) {
                                log.info("Setting node " + zcZWaveDevice.getNode() + " to LISTEN state");
                                zcZWaveDevice.setStatus("Listening");
                            }
                        }

                        switch (nodeType) {
                            case "Portable Remote Controller":

                                zw = addZWaveDeviceOrValue("controller", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Multilevel Power Switch":

                                zw = addZWaveDeviceOrValue("dimmer", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Routing Alarm Sensor":

                                zw = addZWaveDeviceOrValue("alarmsensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            case "Binary Power Switch":

                                zw = addZWaveDeviceOrValue("switch", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            case "Routing Binary Sensor":

                                zw = addZWaveDeviceOrValue("binarysensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Routing Multilevel Sensor":

                                zw = addZWaveDeviceOrValue("multilevelsensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Simple Meter":

                                zw = addZWaveDeviceOrValue("metersensor", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Simple Window Covering":

                                zw = addZWaveDeviceOrValue("drapes", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
                                                zw,
                                                Manager.get().getValueLabel(notification.getValueId()),
                                                String.valueOf(Utils.getValue(notification.getValueId()))));
                                break;

                            //////////////////////////////////

                            case "Setpoint Thermostat":

                                zw = addZWaveDeviceOrValue("thermostat", notification);
                                messaging.broadcast("event.devices.value.added",
                                        zWaveDeviceValueAdded.set(
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

                        zrZWaveDevice.removeValue(new ZWaveDeviceValue(
                                manager.getValueLabel(notification.getValueId()),
                                String.valueOf(Utils.getValue(notification.getValueId())),
                                Utils.getValueType(notification.getValueId()),
                                Manager.get().getValueUnits(notification.getValueId()),
                                notification.getValueId()));

                        messaging.broadcast("event.devices.value.removed",
                                zWaveDeviceValueRemoved.set(
                                        zrZWaveDevice,
                                        Manager.get().getValueLabel(notification.getValueId()),
                                        String.valueOf(Utils.getValue(notification.getValueId()))));

                        if (!manager.getValueLabel(notification.getValueId()).isEmpty())
                            log.info(i18n.message("zwave.node.0.value.1.removed", zrZWaveDevice.getNode(), manager.getValueLabel(notification.getValueId())));

                        break;
                    case VALUE_CHANGED:

                        ZWaveDevice zWaveDevice = getZWaveDeviceByNode(node);

                        // Check for awaked after sleeping nodes
                        if (manager.isNodeAwake(homeId, zWaveDevice.getNode()) && zWaveDevice.getStatus().equals("Sleeping")) {
                            log.info("Setting node " + zWaveDevice.getNode() + " to LISTEN state");
                            zWaveDevice.setStatus("Listening");
                            try {
                                zWaveDevice.save();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }

                        if (zWaveDevice == null) {
                            log.info(i18n.message("zwave.error.while.save.value.change.cannot.find.device.with.node.id.0", node));
                            break;
                        }

                        // break if same value
                        try {
                            if (Utils.getValue(zWaveDevice.getValue(manager.getValueLabel(notification.getValueId())).getValueId()) == Utils.getValue(notification.getValueId()))
                                break;
                        } catch (NullPointerException e) {
                            log.error("Error while change value: " + e.toString());
                            break;
                        }

                        log.info(i18n.message("zwave.node.0.value.for.label.1.changed.2.3",
                                zWaveDevice.getNode(),
                                manager.getValueLabel(notification.getValueId()),
                                Utils.getValue(zWaveDevice.getValue(manager.getValueLabel(notification.getValueId())).getValueId()),
                                Utils.getValue(notification.getValueId())));

                        zWaveDevice.updateValue(new ZWaveDeviceValue(
                                manager.getValueLabel(notification.getValueId()),
                                String.valueOf(Utils.getValue(notification.getValueId())),
                                Utils.getValueType(notification.getValueId()),
                                Manager.get().getValueUnits(notification.getValueId()),
                                notification.getValueId()));

                        messaging.broadcast("event.devices.value.changed",
                                zWaveDeviceValueChanged.set(
                                        zWaveDevice,
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
        manager.addDriver(config.get("zwavePort"));

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


        Service.serviceChecker.setAdvertisment(Service.advertisement.set("Devices", Service.serviceId, ServiceStatus.AVAILABLE));


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
            jsonMessaging.subscribe("event.devices.setname");
            jsonMessaging.subscribe("event.devices.setzone");

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

                        if (advertisement.getDeviceUUID().equals("all")) {

                            jsonMessaging.broadcast("event.devices.responseinventory", responseZWaveDeviceArrayInventoryAdvertisement.set(zDevices));

                        } else {

                            ZWaveDevice zdv = getZWaveDeviceByUUID(advertisement.getDeviceUUID());

                            if (zdv != null) {
                                jsonMessaging.broadcast("event.devices.responseinventory", responseZWaveDeviceInventoryAdvertisement.set(zdv));
                            }
                        }

                    } else if (envelope.getObject() instanceof SetDeviceNameAdvertisement) {

                        SetDeviceNameAdvertisement advertisement = envelope.getObject();

                        ZWaveDevice zdv = getZWaveDeviceByUUID(advertisement.getDeviceUUID());
                        if (zdv != null) {
                            zdv.setName(advertisement.getName());
                            zdv.save();
                        }

                    } else if (envelope.getObject() instanceof SetDeviceZoneAdvertisement) {

                        SetDeviceZoneAdvertisement advertisement = envelope.getObject();

                        ZWaveDevice zdv = getZWaveDeviceByUUID(advertisement.getDeviceUUID());
                        if (zdv != null) {
                            zdv.setZone(advertisement.getZone());
                            zdv.save();
                        }

                    } else if (envelope.getReceiverInstance() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        log.info("Received broadcast "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.info("Received request "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            }

            // Broadcast that this service is shutdown.
            Service.serviceChecker.setAdvertisment(Service.advertisement.set(
                    "Devices", Service.serviceId, ServiceStatus.SHUTDOWN));

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

        for (ZWaveDeviceValue zv : device.getValueIDs()) {
            if (zv.getLabel().equals(label)) {
                if (!Manager.get().isValueReadOnly(zv.getValueId())) {
                    setTypedValue(zv.getValueId(), value);
                } else {
                    log.info("Value \"" + label + "\" is read-only! Skip.");
                }
            }
        }
    }

    private ZWaveDevice hasInstance(String key) {

        Iterator it = zDevices.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry pairs = (Map.Entry) it.next();

            if (key.equals(pairs.getKey())) {
                return (ZWaveDevice) pairs.getValue();
            }
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
                ZWaveDevice.addValue(
                        new ZWaveDeviceValue(
                                label,
                                String.valueOf(Utils.getValue(notification.getValueId())),
                                Utils.getValueType(notification.getValueId()),
                                Manager.get().getValueUnits(notification.getValueId()),
                                notification.getValueId()));

            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }

            log.info(i18n.message("zwave.add.device.to.array.0.1", type, notification.getNodeId()));
            zDevices.put("zwave/" + type + "/" + notification.getNodeId(), ZWaveDevice);
        } else {

            ZWaveDevice.setManufName(manufName);
            ZWaveDevice.setProductName(productName);
            ZWaveDevice.setStatus(state);

            log.info(i18n.message("zwave.node.0.add.value.to.device.1.2", ZWaveDevice.getNode(), label, Utils.getValue(notification.getValueId())));
            ZWaveDevice.addValue(new ZWaveDeviceValue(
                    label,
                    String.valueOf(Utils.getValue(notification.getValueId())),
                    Utils.getValueType(notification.getValueId()),
                    Manager.get().getValueUnits(notification.getValueId()),
                    notification.getValueId()));
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
