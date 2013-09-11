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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

        final Options options = Options.create("./conf/zwave-conf", "", "");
        options.addOptionBool("ConsoleOutput", false);
        options.lock();

        final Manager manager = Manager.create();
        manager.addNotificationWatcher(new NotificationWatcher() {
            @Override
            public void onNotification(Notification notification) {

                String label = manager.getValueLabel(notification.getValueId());
                ZWaveDevice device = null;

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

                                //////////////////////////////////

                                if((device = hasInstance("controller/"+notification.getNodeId())) == null)
                                {
                                    String uuid = UUID.randomUUID().toString();

                                    try {
                                        device = new ZWaveDevice();
                                        device.setInternalType("controller");
                                        device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                        device.setNode(notification.getNodeId());
                                        device.setUUID(uuid);
                                        device.setValue(label, getValue(notification.getValueId()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }

                                    log.info("[zwave] Add device to array - "+"controller/"+notification.getNodeId());
                                    zDevices.put("controller/"+notification.getNodeId(), device);
                                }
                                else
                                {
                                    device.setValue(label, getValue(notification.getValueId()));
                                }
                                break;

                            //////////////////////////////////


                            case "Multilevel Power Switch":

                                //////////////////////////////////

                                    if((device = hasInstance("dimmer/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("dimmer");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"dimmer/"+notification.getNodeId());
                                        zDevices.put("dimmer/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                            device.setValue(label, getValue(notification.getValueId()));
                                    }
                                break;

                                //////////////////////////////////

                            case "Routing Alarm Sensor":

                                if((device = hasInstance("alarmsensor/"+notification.getNodeId())) == null)
                                {
                                    String uuid = UUID.randomUUID().toString();

                                    try {
                                        device = new ZWaveDevice();
                                        device.setInternalType("alarmsensor");
                                        device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                        device.setNode(notification.getNodeId());
                                        device.setUUID(uuid);
                                        device.setValue(label, getValue(notification.getValueId()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }

                                    log.info("[zwave] Add device to array - "+"alarmsensor/"+notification.getNodeId());
                                    zDevices.put("alarmsensor/"+notification.getNodeId(), device);
                                }
                                else
                                {
                                    device.setValue(label, getValue(notification.getValueId()));
                                }
                                break;

                            case "Binary Power Switch":
                                //////////////////////////////////
                                    if((device = hasInstance("switch/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("switch");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"switch/"+notification.getNodeId());
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }
                                break;

                            case "Routing Binary Sensor":

                                //////////////////////////////////

                                    if((device = hasInstance("binarysensor/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("binarysensor");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"binarysensor/"+notification.getNodeId());
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }
                                break;

                            //////////////////////////////////

                            case "Routing Multilevel Sensor":

                                //////////////////////////////////

                                if(label.equals("Luminance"))
                                {
                                    if((device = hasInstance("brightnesssensor/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("brightnesssensor");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"brightnesssensor/"+notification.getNodeId());
                                        zDevices.put("brightnesssensor/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }
                                } else if (label.equals("Temperature"))
                                {
                                    if((device = hasInstance("temperaturesensor/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("temperaturesensor");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"temperaturesensor/"+notification.getNodeId());
                                        zDevices.put("temperaturesensor/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }

                                } else {

                                    log.info("[zwave] Unhandled label \""+label+"\" detected. Adding generic multilevel sensor");

                                    if((device = hasInstance("multilevelsensor/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("multilevelsensor)");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"multilevelsensor/"+notification.getNodeId());
                                        zDevices.put("multilevelsensor/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }

                                }
                                break;

                            //////////////////////////////////

                            case "Simple Meter":

                                //////////////////////////////////

                                    if((device = hasInstance("metersensor/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("metersensor");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"metersensor/"+notification.getNodeId());
                                        zDevices.put("metersensor/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }

                                break;

                            //////////////////////////////////

                            case "Simple Window Covering":

                                //////////////////////////////////

                                    if((device = hasInstance("drapes/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("drapes");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));;
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"drapes/"+notification.getNodeId());
                                        zDevices.put("drapes/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }

                                break;

                            //////////////////////////////////

                            case "Setpoint Thermostat":

                                //////////////////////////////////

                                    if((device = hasInstance("thermostat/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setInternalType("thermostat");
                                            device.setType(manager.getNodeType(notification.getHomeId(), notification.getNodeId()));
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        log.info("[zwave] Add device to array - "+"thermostat/"+notification.getNodeId());
                                        zDevices.put("thermostat/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        device.setValue(label, getValue(notification.getValueId()));
                                    }
                                break;

                            //////////////////////////////////

                            //////////////////////////////////

                            default:
                                log.info("[zwave] Unassigned value! Node: "+notification.getNodeId()+
                                        ", Type: "+manager.getNodeType(notification.getHomeId(), notification.getNodeId())+
                                        ", Class: "+notification.getValueId().getCommandClassId()+
                                        ", Genre: "+notification.getValueId().getGenre()+
                                        ", Label: "+label+
                                        ", Value: "+getValue(notification.getValueId())+
                                        ", Index: "+notification.getValueId().getIndex()+
                                        ", Instance: "+notification.getValueId().getInstance()
                                );


                        }

                        break;
                    case VALUE_REMOVED:
                        System.out.println(String.format("Value removed\n" +
                                "\tnode id: %d\n" +
                                "\tcommand class: %d\n" +
                                "\tinstance: %d\n" +
                                "\tindex: %d",
                                notification.getNodeId(),
                                notification.getValueId().getCommandClassId(),
                                notification.getValueId().getInstance(),
                                notification.getValueId().getIndex()
                        ));
                        break;
                    case VALUE_CHANGED:
                        System.out.println(String.format("Value changed\n" +
                                "\tnode id: %d\n" +
                                "\tcommand class: %d\n" +
                                "\tinstance: %d\n" +
                                "\tindex: %d\n" +
                                "\tvalue: %s",
                                notification.getNodeId(),
                                notification.getValueId().getCommandClassId(),
                                notification.getValueId().getInstance(),
                                notification.getValueId().getIndex(),
                                getValue(notification.getValueId())
                        ));
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
                        System.out.println("Notification");
                        break;
                    default:
                        System.out.println(notification.getType().name());
                        break;
                }
            }
        });

        String controllerPort = "/dev/ttyUSB0";

        manager.addDriver(controllerPort);

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

        log.info ("[speak] Service started (TTS: Google)");

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

                    ZWaveDevice device = getDeviceByUUID(uuid);

                    short node = device.getNode();

                    if(cmd.equals("setlevel"))
                    {
                        log.info("[zwave] Setting level "+m.getShortProperty("level")+" on UUID "+uuid);
                        manager.setNodeLevel(homeId, node, m.getShortProperty("level"));
                    } else if (cmd.equals("enable"))
                    {
                        log.info("[zwave] Enabling UUID "+uuid);
                        manager.setNodeOn(homeId, node);
                    } else if (cmd.equals("disable"))
                    {
                        log.info("[zwave] Disabling UUID "+uuid);
                        manager.setNodeOff(homeId, node);
                    } else
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
        HashMap zDv = (HashMap) zDevices.clone();

        Iterator it = zDv.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();

            if(uuid.equals(pairs.getKey()))
            {
                return (ZWaveDevice) pairs.getValue();
            }

            it.remove(); // avoids a ConcurrentModificationException
        }

        return null;
    }
}

