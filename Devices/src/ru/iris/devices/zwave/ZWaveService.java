package ru.iris.devices.zwave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwave4j.*;
import ru.iris.devices.Service;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class ZWaveService implements Runnable
{
    private static Logger log = LoggerFactory.getLogger (ZWaveService.class.getName ());
    private static long homeId;
    private static boolean ready = false;
    private static HashMap<String, ZWaveDevice> zDevices = new HashMap<String, ZWaveDevice>();

    public ZWaveService()
    {
        Thread t = new Thread (this);
        t.start ();
    }

    @Override
    public synchronized void run()
    {
        // Сначала вытащим уже имеющиеся устройства из БД
        ResultSet rs = Service.sql.select("SELECT * FROM DEVICES");

        try {
            while (rs.next()) {

                ZWaveDevice zdevice = new ZWaveDevice();

                zdevice.setManufName(rs.getString("manufname"));
                zdevice.setName(rs.getString("name"));
                zdevice.setNode((short) rs.getInt("node"));
                zdevice.setStatus(rs.getString("status"));
                zdevice.setInternalType(rs.getString("internaltype"));
                zdevice.setType(rs.getString("type"));
                zdevice.setUUID(rs.getString("uuid"));
                zdevice.setZone(rs.getInt("zone"));

                log.info("[zwave] Load device \""+zdevice.getInternalType()+"/"+zdevice.getNode()+"\" from database");

                zDevices.put(zdevice.getInternalType() + "/" + zdevice.getNode(), zdevice);
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

        manager.addNotificationWatcher(new NotificationWatcher() {
            @Override
            public void onNotification(Notification notification) {

                switch (notification.getType()) {
                    case DRIVER_READY:
                        System.out.println(String.format("Driver ready\n" +
                                "\thome id: %d",
                                notification.getHomeId()
                        ));
                        homeId = notification.getHomeId();
                        break;
                    case DRIVER_FAILED:
                        System.out.println("Driver failed");
                        break;
                    case DRIVER_RESET:
                        System.out.println("Driver reset");
                        break;
                    case AWAKE_NODES_QUERIED:
                        System.out.println("Awake nodes queried");
                        ready = true;
                        break;
                    case ALL_NODES_QUERIED:
                        System.out.println("All nodes queried");
                        manager.writeConfig(homeId);
                        ready = true;
                        break;
                    case ALL_NODES_QUERIED_SOME_DEAD:
                        System.out.println("All nodes queried some dead");
                        break;
                    case POLLING_ENABLED:
                        System.out.println("Polling enabled");
                        break;
                    case POLLING_DISABLED:
                        System.out.println("Polling disabled");
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
                        break;
                    case NODE_NAMING:
                        break;
                    case NODE_PROTOCOL_INFO:
                        break;
                    case VALUE_ADDED:

                        String nodeType = manager.getNodeType(notification.getHomeId(), notification.getNodeId());

                        switch (nodeType)
                        {
                            case "Portable Remote Controller":

                                addDeviceOrValue("controller", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Multilevel Power Switch":

                                addDeviceOrValue("dimmer", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Routing Alarm Sensor":

                                addDeviceOrValue("alarmsensor", notification, manager);
                                break;

                            case "Binary Power Switch":

                                addDeviceOrValue("switch", notification, manager);
                                break;

                            case "Routing Binary Sensor":

                                addDeviceOrValue("binarysensor", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Routing Multilevel Sensor":

                                addDeviceOrValue("multilevelsensor", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Simple Meter":

                                addDeviceOrValue("metersensor", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Simple Window Covering":

                                addDeviceOrValue("drapes", notification, manager);
                                break;

                            //////////////////////////////////

                            case "Setpoint Thermostat":

                                addDeviceOrValue("thermostat", notification, manager);
                                break;

                            //////////////////////////////////
                            //////////////////////////////////

                            default:
                                log.info("[zwave] Unassigned value! Node: "+notification.getNodeId()+
                                        ", Type: "+manager.getNodeType(notification.getHomeId(), notification.getNodeId())+
                                        ", Class: "+notification.getValueId().getCommandClassId()+
                                        ", Genre: "+notification.getValueId().getGenre()+
                                        ", Label: "+manager.getValueLabel(notification.getValueId())+
                                        ", Value: "+getValue(notification.getValueId())+
                                        ", Index: "+notification.getValueId().getIndex()+
                                        ", Instance: "+notification.getValueId().getInstance()
                                );


                        }

                        break;
                    case VALUE_REMOVED:

                        ZWaveDevice zrdevice = getDeviceByNode(notification.getNodeId());

                        if(zrdevice == null)
                        {
                            log.info("[zwave] Error while save value remove. Cannot find device with Node ID: "+notification.getNodeId());
                            break;
                        }

                        zrdevice.removeValue(manager.getValueLabel(notification.getValueId()));
                        log.info("[zwave] Node "+zrdevice.getNode()+": Value \""+manager.getValueLabel(notification.getValueId())+"\" removed");

                        break;
                    case VALUE_CHANGED:

                        ZWaveDevice zcdevice = getDeviceByNode(notification.getNodeId());

                        if(zcdevice == null)
                        {
                            log.info("[zwave] Error while save value change. Cannot find device with Node ID: "+notification.getNodeId());
                            break;
                        }

                        String oldValue = zcdevice.getValue(manager.getValueLabel(notification.getValueId()));
                        zcdevice.updateValue(manager.getValueLabel(notification.getValueId()),getValue(notification.getValueId()));
                        log.info("[zwave] Node "+zcdevice.getNode()+": Value for label \""+manager.getValueLabel(notification.getValueId())+"\" changed: "+oldValue+" --> "+getValue(notification.getValueId()));

                        break;
                    case VALUE_REFRESHED:
                        System.out.println(String.format("Value refreshed\n" +
                                "\tnode id: %d\n" +
                                "\tcommand class: %d\n" +
                                "\tinstance: %d\n" +
                                "\tindex: %d" +
                                "\tvalue: %s",
                                notification.getNodeId(),
                                notification.getValueId().getCommandClassId(),
                                notification.getValueId().getInstance(),
                                notification.getValueId().getIndex(),
                                getValue(notification.getValueId())
                        ));
                        break;
                    case GROUP:
                        System.out.println(String.format("Group\n" +
                                "\tnode id: %d\n" +
                                "\tgroup id: %d",
                                notification.getNodeId(),
                                notification.getGroupIdx()
                        ));
                        break;

                    case SCENE_EVENT:
                        System.out.println(String.format("Scene event\n" +
                                "\tscene id: %d",
                                notification.getSceneId()
                        ));
                        break;
                    case CREATE_BUTTON:
                        System.out.println(String.format("Button create\n" +
                                "\tbutton id: %d",
                                notification.getButtonId()
                        ));
                        break;
                    case DELETE_BUTTON:
                        System.out.println(String.format("Button delete\n" +
                                "\tbutton id: %d",
                                notification.getButtonId()
                        ));
                        break;
                    case BUTTON_ON:
                        System.out.println(String.format("Button on\n" +
                                "\tbutton id: %d",
                                notification.getButtonId()
                        ));
                        break;
                    case BUTTON_OFF:
                        System.out.println(String.format("Button off\n" +
                                "\tbutton id: %d",
                                notification.getButtonId()
                        ));
                        break;
                    case NOTIFICATION:
                        break;
                    default:
                        System.out.println(notification.getType().name());
                        break;
                }
            }
        });

        manager.addDriver(Service.config.get("zwavePort"));

        log.info("[zwave] Not Ready!");

        // Ждем окончания инициализации
        while(!ready)
        {
            try {
                Thread.sleep(1000);
                log.info("[zwave] Waiting");
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        log.info("[zwave] Initialize complete!");

        // Сохраняем текущую конфигурацию
        for (ZWaveDevice device : zDevices.values()) {
            try {
                device.save();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        Message message = null;
        MapMessage m = null;
        ExecutorService exs = Executors.newFixedThreadPool(10);

        try
        {

            while ((message = Service.messageConsumer.receive (0)) != null)
            {
                m = (MapMessage) message;

                if(m.getStringProperty("qpid.subject").contains("event.devices.setvalue"))
                {
                    String uuid = m.getStringProperty("uuid");
                    String cmd = m.getStringProperty("command");
                    short node = 0;

                    if(!cmd.equals("allon") && !cmd.equals("alloff"))
                    {
                        ZWaveDevice device = getDeviceByUUID(uuid);

                        if(device == null)
                        {
                            log.info("[zwave] Cannot find device with UUID: "+uuid);
                            continue;
                        }

                        node = device.getNode();
                    }

                    if(cmd.equals("setlevel"))
                    {
                        log.info("[zwave] Setting level "+m.getShortProperty("level")+" on UUID "+uuid + " (Node "+node+")");
                        manager.setNodeLevel(homeId, node, m.getShortProperty("level"));
                    }
                    else if (cmd.equals("enable"))
                    {
                        log.info("[zwave] Enabling UUID "+uuid + " (Node "+node+")");
                        manager.setNodeOn(homeId, node);
                    }
                    else if (cmd.equals("disable"))
                    {
                        log.info("[zwave] Disabling UUID "+uuid + " (Node "+node+")");
                        manager.setNodeOff(homeId, node);
                    }
                    else if (cmd.equals("allon"))
                    {
                        log.info("[zwave] Enabling all");
                        manager.switchAllOn(homeId);
                    }
                    else if (cmd.equals("alloff"))
                    {
                        log.info("[zwave] Disabling all");
                        manager.switchAllOff(homeId);
                    }
                    else
                    {
                        log.info("[zwave] Unknown command \""+cmd+"\"");
                    }
                }
            }

            Service.msg.close ();

        } catch (Exception e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
            log.info ("Get error! " + m);
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

    private ZWaveDevice hasInstance(String key)
    {
        HashMap zDv = (HashMap) zDevices.clone();

        Iterator it = zDv.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();

            if(key.equals(pairs.getKey()))
            {
                return (ZWaveDevice) pairs.getValue();
            }

            it.remove(); // avoids a ConcurrentModificationException
        }

        return null;
    }

    private ZWaveDevice getDeviceByUUID(String uuid)
    {
        for(ZWaveDevice device : zDevices.values())
        {
            if(device.getUUID().equals(uuid))
                return device;
        }
        return null;
    }

    private ZWaveDevice getDeviceByNode(short id)
    {
        for(ZWaveDevice device : zDevices.values())
        {
            if(device.getNode() == id)
            {
                return device;
            }
        }
        return null;
    }

    private void addDeviceOrValue(String type, Notification notification, Manager manager) {

        ZWaveDevice device = null;
        String label = manager.getValueLabel(notification.getValueId());

        try {
            device = new ZWaveDevice();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if((device = hasInstance(type+"/"+notification.getNodeId())) == null)
        {
            String uuid = UUID.randomUUID().toString();

            try {
                device = new ZWaveDevice();
                device.setManufName(manager.getNodeManufacturerName(notification.getHomeId(), notification.getNodeId()));
                device.setInternalType(type);
                device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                device.setNode(notification.getNodeId());
                device.setUUID(uuid);
                device.setValue(label, getValue(notification.getValueId()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            log.info("[zwave] Add device to array - "+type+"/"+notification.getNodeId());
            zDevices.put(type+"/"+notification.getNodeId(), device);
        }
        else
        {
            log.info("[zwave] Add value to device: "+label+" --> "+getValue(notification.getValueId()));
            device.setValue(label, getValue(notification.getValueId()));
        }

    }
}
