package ru.iris.devices.zwave;

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwave4j.*;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.devices.Service;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
    private static HashMap<String, ZWaveDevice> zDevices = new HashMap<String, ZWaveDevice>();
    private static final I18N i18n = new I18N();

    public ZWaveService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {
        // Сначала вытащим уже имеющиеся устройства из БД
        @NonNls ResultSet rs = Service.sql.select("SELECT * FROM DEVICES");

        try {
            while (rs.next()) {

                ZWaveDevice zDevice = new ZWaveDevice();

                zDevice.setManufName(rs.getString("manufname"));
                zDevice.setName(rs.getString("name"));
                zDevice.setNode((short) rs.getInt("node"));
                zDevice.setStatus(rs.getString("status"));
                zDevice.setInternalType(rs.getString("internaltype"));
                zDevice.setType(rs.getString("type"));
                zDevice.setUUID(rs.getString("uuid"));
                zDevice.setZone(rs.getInt("zone"));

                log.info(i18n.message("zwave.load.device.0.1.from.database", zDevice.getInternalType(), zDevice.getNode()));

                zDevices.put(zDevice.getInternalType() + "/" + zDevice.getNode(), zDevice);
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

        final Options options = Options.create(Service.config.get("openzwaveCfgPath"), "", "");
        options.addOptionBool("ConsoleOutput", Boolean.parseBoolean(Service.config.get("zwaveDebug")));
        options.lock();

        final Manager manager = Manager.create();

        final NotificationWatcher watcher = new NotificationWatcher() {

            @Override
            public void onNotification(Notification notification, Object context) {

                switch (notification.getType()) {
                    case DRIVER_READY:
                        log.info(i18n.message("zwave.driver.ready.home.id.0", notification.getHomeId()));
                        homeId = notification.getHomeId();
                        break;
                    case DRIVER_FAILED:
                        log.info(i18n.message("zwave.driver.failed"));
                        break;
                    case DRIVER_RESET:
                        log.info(i18n.message("zwave.driver.reset"));
                        break;
                    case AWAKE_NODES_QUERIED:
                        log.info(i18n.message("zwave.awake.nodes.queried"));
                        ready = true;
                        break;
                    case ALL_NODES_QUERIED:
                        log.info(i18n.message("zwave.all.nodes.queried"));
                        manager.writeConfig(homeId);
                        ready = true;
                        break;
                    case ALL_NODES_QUERIED_SOME_DEAD:
                        log.info(i18n.message("zwave.all.nodes.queried.some.dead"));
                        break;
                    case POLLING_ENABLED:
                        log.info(i18n.message("zwave.polling.enabled"));
                        break;
                    case POLLING_DISABLED:
                        log.info(i18n.message("zwave.polling.disabled"));
                        break;
                    case NODE_NEW:
                        break;
                    case NODE_ADDED:
                        break;
                    case NODE_REMOVED:
                        break;
                    case ESSENTIAL_NODE_QUERIES_COMPLETE:
                        break;
                    case NODE_QUERIES_COMPLETE:
                        break;
                    case NODE_EVENT:
                        log.info(i18n.message("zwave.update.info.for.node.0", notification.getNodeId()));
                        manager.refreshNodeInfo(homeId, notification.getNodeId());
                        break;
                    case NODE_NAMING:
                        break;
                    case NODE_PROTOCOL_INFO:
                        break;
                    case VALUE_ADDED:

                        @NonNls
                        String nodeType = manager.getNodeType(notification.getHomeId(), notification.getNodeId());

                        switch (nodeType) {
                            case "Portable Remote Controller":

                                addZWaveDeviceOrValue("controller", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Multilevel Power Switch":

                                addZWaveDeviceOrValue("dimmer", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Routing Alarm Sensor":

                                addZWaveDeviceOrValue("alarmsensor", notification, manager);
                                break;

                            case "Binary Power Switch":

                                addZWaveDeviceOrValue("switch", notification, manager);
                                break;

                            case "Routing Binary Sensor":

                                addZWaveDeviceOrValue("binarysensor", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Routing Multilevel Sensor":

                                addZWaveDeviceOrValue("multilevelsensor", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Simple Meter":

                                addZWaveDeviceOrValue("metersensor", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Simple Window Covering":

                                addZWaveDeviceOrValue("drapes", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Setpoint Thermostat":

                                addZWaveDeviceOrValue("thermostat", notification, manager);
                                break;

                            //////////////////////////////////
                            //////////////////////////////////

                            default:
                                log.info(i18n.message("zwave.unassigned.value.node.0.type.1.class.2.genre.3.label.4.value.5.index.6.instance.7",
                                        notification.getNodeId(),
                                        manager.getNodeType(notification.getHomeId(), notification.getNodeId()),
                                        notification.getValueId().getCommandClassId(),
                                        notification.getValueId().getGenre(),
                                        manager.getValueLabel(notification.getValueId()),
                                        getValue(notification.getValueId()),
                                        notification.getValueId().getIndex(),
                                        notification.getValueId().getInstance())
                                );
                        }

                        break;
                    case VALUE_REMOVED:

                        ZWaveDevice zrZWaveDevice = getZWaveDeviceByNode(notification.getNodeId());

                        if (zrZWaveDevice == null) {
                            log.info(i18n.message("zwave.error.while.save.value.remove.cannot.find.device.with.node.id.0", notification.getNodeId()));
                            break;
                        }

                        zrZWaveDevice.removeValueID(manager.getValueLabel(notification.getValueId()));
                        log.info(i18n.message("zwave.node.0.value.1.removed", zrZWaveDevice.getNode(), manager.getValueLabel(notification.getValueId())));

                        break;
                    case VALUE_CHANGED:

                        ZWaveDevice zcZWaveDevice = getZWaveDeviceByNode(notification.getNodeId());

                        if (zcZWaveDevice == null) {
                            log.info(i18n.message("zwave.error.while.save.value.change.cannot.find.device.with.node.id.0", notification.getNodeId()));
                            break;
                        }

                        // break if same value
                        if(getValue((ValueId) zcZWaveDevice.getValue(manager.getValueLabel(notification.getValueId()))) == getValue(notification.getValueId()))
                            break;

                        zcZWaveDevice.updateValueID(manager.getValueLabel(notification.getValueId()), notification.getValueId());
                        log.info(i18n.message("zwave.node.0.value.for.label.1.changed.2.3", zcZWaveDevice.getNode(), manager.getValueLabel(notification.getValueId()), getValue((ValueId) zcZWaveDevice.getValue(manager.getValueLabel(notification.getValueId()))), getValue(notification.getValueId())));

                        break;
                    case VALUE_REFRESHED:
                        log.info(i18n.message("value.refreshed.node.id.0.command.class.1.instance.2.index.3.value.4",
                                notification.getNodeId(),
                                notification.getValueId().getCommandClassId(),
                                notification.getValueId().getInstance(),
                                notification.getValueId().getIndex(),
                                getValue(notification.getValueId())));
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
                        System.out.println(notification.getType().name());
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

        for (ZWaveDevice ZWaveDevice : zDevices.values()) {
            try {
                ZWaveDevice.save();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Message message = null;
        @NonNls MapMessage m = null;

        try {
            MessageConsumer messageConsumer = new Messaging().getConsumer();

            while ((message = messageConsumer.receive(0)) != null) {
                m = (MapMessage) message;
                @NonNls ZWaveDevice ZWaveDevice = null;

                if (m.getStringProperty("qpid.subject").contains("event.devices.setvalue")) {
                    String uuid = m.getStringProperty("uuid");
                    @NonNls String cmd = m.getStringProperty("command");
                    short node = 0;

                    if (!cmd.equals("allon") && !cmd.equals("alloff")) {
                        ZWaveDevice = getZWaveDeviceByUUID(uuid);

                        if (ZWaveDevice == null) {
                            log.info(i18n.message("zwave.cannot.find.device.with.uuid.0", uuid));
                            continue;
                        }

                        node = ZWaveDevice.getNode();
                    }

                    if (cmd.equals("setlevel")) {
                        log.info(i18n.message("zwave.setting.level.0.on.uuid.1.node.2", m.getIntProperty("level"), uuid, node));
                        setValue(uuid, m.getIntProperty("level"));
                    } else if (cmd.equals("enable")) {
                        log.info(i18n.message("zwave.enabling.uuid.0.node.1", uuid, node));
                        setValue(uuid, 255);
                    } else if (cmd.equals("disable")) {
                        log.info(i18n.message("zwave.disabling.uuid.0.node.1", uuid, node));
                        setValue(uuid, 0);
                    } else if (cmd.equals("allon")) {
                        log.info(i18n.message("zwave.enabling.all"));
                        manager.switchAllOn(homeId);
                    } else if (cmd.equals("alloff")) {
                        log.info(i18n.message("zwave.disabling.all"));
                        manager.switchAllOff(homeId);
                    } else if (cmd.equals("updateinfo")) {
                        log.info(i18n.message("zwave.update.info.for.node.0", node));
                        manager.refreshNodeInfo(homeId, node);
                    } else if (cmd.equals("inventory")) {
                    } else {
                        log.info(i18n.message("zwave.unknown.command.0", cmd));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.info(i18n.message("zwave.get.error.0", m));
        }

    }

    private static Object getValue(ValueId valueId) {
        switch (valueId.getType()) {
            case BOOL:
                AtomicReference<Boolean> b = new AtomicReference<>();
                Manager.get().getValueAsBool(valueId, b);
                return b.get();
            case BYTE:
                AtomicReference<Short> bb = new AtomicReference<>();
                Manager.get().getValueAsByte(valueId, bb);
                return bb.get();
            case DECIMAL:
                AtomicReference<Float> f = new AtomicReference<>();
                Manager.get().getValueAsFloat(valueId, f);
                return f.get();
            case INT:
                AtomicReference<Integer> i = new AtomicReference<>();
                Manager.get().getValueAsInt(valueId, i);
                return i.get();
            case LIST:
                return null;
            case SCHEDULE:
                return null;
            case SHORT:
                AtomicReference<Short> s = new AtomicReference<>();
                Manager.get().getValueAsShort(valueId, s);
                return s.get();
            case STRING:
                AtomicReference<String> ss = new AtomicReference<>();
                Manager.get().getValueAsString(valueId, ss);
                return ss.get();
            case BUTTON:
                return null;
            case RAW:
                AtomicReference<short[]> sss = new AtomicReference<>();
                Manager.get().getValueAsRaw(valueId, sss);
                return sss.get();
            default:
                return null;
        }
    }

    private void setValue(String uuid, int value)
    {
        ZWaveDevice device = getZWaveDeviceByUUID(uuid);

        HashMap <String, Object> valueIDs = device.getValueIDs();

        Iterator it = valueIDs.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry pairs = (Map.Entry) it.next();
            ValueId valueId = (ValueId) pairs.getValue();

            if(valueId.getCommandClassId() == 0x25 || valueId.getCommandClassId() == 0x26 || valueId.getCommandClassId() == 0x27)
            {
                Manager.get().setValueAsShort(valueId, (short) value);
            }

            it.remove(); // avoids a ConcurrentModificationException
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

    private void addZWaveDeviceOrValue(@NonNls String type, Notification notification, Manager manager) {

        ZWaveDevice ZWaveDevice;
        String label = manager.getValueLabel(notification.getValueId());

        if ((ZWaveDevice = hasInstance(type + "/" + notification.getNodeId())) == null) {
            String uuid = UUID.randomUUID().toString();
            String state = i18n.message("not.responding");

            if (manager.requestNodeState(homeId, notification.getNodeId()))
                state = i18n.message("listening");

            try {
                ZWaveDevice = new ZWaveDevice();
                ZWaveDevice.setManufName(manager.getNodeManufacturerName(notification.getHomeId(), notification.getNodeId()));
                ZWaveDevice.setInternalType(type);
                ZWaveDevice.setStatus(state);
                ZWaveDevice.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                ZWaveDevice.setNode(notification.getNodeId());
                ZWaveDevice.setUUID(uuid);
                ZWaveDevice.setValueID(label, notification.getValueId());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            log.info(i18n.message("zwave.add.device.to.array.0.1", type, notification.getNodeId()));
            zDevices.put(type + "/" + notification.getNodeId(), ZWaveDevice);
        } else {
            log.info(i18n.message("zwave.add.value.to.device.0.1", label, getValue(notification.getValueId())));
            ZWaveDevice.setValueID(label, notification.getValueId());
        }

    }
}
