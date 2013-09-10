package ru.iris.devices.zwave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwave4j.*;
import ru.iris.devices.Service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
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
    private HashMap<String, ZWaveDevice> zDevices = new HashMap<>();

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
                zdevice.setNode(rs.getInt("node"));
                zdevice.setStatus(rs.getString("status"));
                zdevice.setType(rs.getString("type"));
                zdevice.setUUID(rs.getString("uuid"));
                zdevice.setZone(rs.getInt("zone"));

                zDevices.put(zdevice.getType()+"/"+zdevice.getNode(), zdevice);
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

        final Options options = Options.create("./conf/zwave-conf", "./conf/zwave-conf", "");
        options.addOptionBool("ConsoleOutput", false);
        options.lock();

        final Manager manager = Manager.create();
        manager.addNotificationWatcher(new NotificationWatcher() {
            @Override
            public void onNotification(Notification notification) {

                String label = manager.getValueLabel(notification.getValueId());
                ZWaveDevice device;

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
                        System.out.println(String.format("Node new\n" +
                                "\tnode id: %d",
                                notification.getNodeId()
                        ));
                        break;
                    case NODE_ADDED:
                        System.out.println(String.format("Node added\n" +
                                "\tnode id: %d",
                                notification.getNodeId()
                        ));
                        break;
                    case NODE_REMOVED:
                        System.out.println(String.format("Node removed\n" +
                                "\tnode id: %d",
                                notification.getNodeId()
                        ));
                        break;
                    case ESSENTIAL_NODE_QUERIES_COMPLETE:
                        System.out.println(String.format("Node essential queries complete\n" +
                                "\tnode id: %d",
                                notification.getNodeId()
                        ));
                        break;
                    case NODE_QUERIES_COMPLETE:
                        System.out.println(String.format("Node queries complete\n" +
                                "\tnode id: %d",
                                notification.getNodeId()
                        ));
                        break;
                    case NODE_EVENT:
                        System.out.println(String.format("Node event\n" +
                                "\tnode id: %d\n" +
                                "\tevent id: %d",
                                notification.getNodeId(),
                                notification.getEvent()
                        ));
                        break;
                    case NODE_NAMING:
                        System.out.println(String.format("Node naming\n" +
                                "\tnode id: %d",
                                notification.getNodeId()
                        ));
                        break;
                    case NODE_PROTOCOL_INFO:
                        System.out.println(String.format("Node protocol info\n" +
                                "\tnode id: %d\n" +
                                "\ttype: %s",
                                notification.getNodeId(),
                                manager.getNodeType(notification.getHomeId(), notification.getNodeId())
                        ));
                        break;
                    case VALUE_ADDED:

                        switch (notification.getValueId().getCommandClassId())
                        {
                            case COMMAND_CLASS_SWITCH_MULTILEVEL:

                                if(label.equals("Level"))
                                {
                                    if((device = hasInstance("dimmer/"+notification.getNodeId())) == null)
                                    {
                                        String uuid = UUID.randomUUID().toString();

                                        try {
                                            device = new ZWaveDevice();
                                            device.setType("dimmer");
                                            device.setNode(notification.getNodeId());
                                            device.setUUID(uuid);
                                            device.setValue(label, getValue(notification.getValueId()));
                                        } catch (IOException e) {
                                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                        } catch (SQLException e) {
                                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                        }

                                        zDevices.put("dimmer/"+notification.getNodeId(), device);
                                    }
                                    else
                                    {
                                        try {
                                            device.setValue(label, getValue(notification.getValueId()));
                                        }
                                        catch (NullPointerException e)
                                        {
                                            log.info("============================\nLabel: "+label);
                                            e.printStackTrace();
                                        }

                                    }
                                }

                            case COMMAND_CLASS_SWITCH_BINARY:
                                System.out.println("Binary");
                                break;
                            default:
                                System.out.println("Unknown value");
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
                log.info(device.toString());
                device.save();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
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

        return null;
    }
    
/*
 *      Copyright (C) 2008 Harald Klein <hari@vt100.at>
*
*      This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License.
*      This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
*      of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
*
*      See the GNU General Public License for more details.
*
*      Modified by Nikolay Viguro for Java
*/

   private static final int BASIC_TYPE_CONTROLLER                        =0x01;
   private static final int BASIC_TYPE_STATIC_CONTROLLER                 =0x02;
   private static final int BASIC_TYPE_SLAVE                             =0x03;
   private static final int BASIC_TYPE_ROUTING_SLAVE                      =0x04;

   private static final int GENERIC_TYPE_GENERIC_CONTROLLER               =0x01;
   private static final int GENERIC_TYPE_STATIC_CONTROLLER                =0x02;
   private static final int GENERIC_TYPE_AV_CONTROL_POINT                 =0x03;
   private static final int GENERIC_TYPE_DISPLAY                          =0x06;
   private static final int GENERIC_TYPE_GARAGE_DOOR                      =0x07;
   private static final int GENERIC_TYPE_THERMOSTAT                       =0x08;
   private static final int GENERIC_TYPE_WINDOW_COVERING                  =0x09;
   private static final int GENERIC_TYPE_REPEATER_SLAVE                   =0x0F;
   private static final int GENERIC_TYPE_SWITCH_BINARY                    =0x10;

   private static final int GENERIC_TYPE_SWITCH_MULTILEVEL                =0x11;
   private static final int SPECIFIC_TYPE_NOT_USED                        =0x00;
   private static final int SPECIFIC_TYPE_POWER_SWITCH_MULTILEVEL         =0x01;
   private static final int SPECIFIC_TYPE_MOTOR_MULTIPOSITION             =0x03;
   private static final int SPECIFIC_TYPE_SCENE_SWITCH_MULTILEVEL         =0x04;
   private static final int SPECIFIC_TYPE_CLASS_A_MOTOR_CONTROL           =0x05;
   private static final int SPECIFIC_TYPE_CLASS_B_MOTOR_CONTROL           =0x06;
   private static final int SPECIFIC_TYPE_CLASS_C_MOTOR_CONTROL           =0x07;

   private static final int GENERIC_TYPE_SWITCH_REMOTE                    =0x12;
   private static final int GENERIC_TYPE_SWITCH_TOGGLE                    =0x13;
   private static final int GENERIC_TYPE_SENSOR_BINARY                    =0x20;
   private static final int GENERIC_TYPE_SENSOR_MULTILEVEL                =0x21;
   private static final int GENERIC_TYPE_SENSOR_ALARM                     =0xa1;
   private static final int GENERIC_TYPE_WATER_CONTROL                    =0x22;
   private static final int GENERIC_TYPE_METER_PULSE                      =0x30;
   private static final int GENERIC_TYPE_ENTRY_CONTROL                    =0x40;
   private static final int GENERIC_TYPE_SEMI_INTEROPERABLE               =0x50;
   private static final int GENERIC_TYPE_NON_INTEROPERABLE                =0xFF;

   private static final int SPECIFIC_TYPE_ADV_ZENSOR_NET_SMOKE_SENSOR     =0x0a;
   private static final int SPECIFIC_TYPE_BASIC_ROUTING_SMOKE_SENSOR      =0x06;
   private static final int SPECIFIC_TYPE_BASIC_ZENSOR_NET_SMOKE_SENSOR   =0x08;
   private static final int SPECIFIC_TYPE_ROUTING_SMOKE_SENSOR            =0x07;
   private static final int SPECIFIC_TYPE_ZENSOR_NET_SMOKE_SENSOR         =0x09;

   private static final int COMMAND_CLASS_MARK                            =0xef;
   private static final int COMMAND_CLASS_BASIC                           =0x20;
   private static final int COMMAND_CLASS_VERSION                         =0x86;
   private static final int COMMAND_CLASS_BATTERY                         =0x80	;
   private static final int COMMAND_CLASS_WAKE_UP                         =0x84	;
   private static final int COMMAND_CLASS_CONTROLLER_REPLICATION          =0x21 ;
   private static final int COMMAND_CLASS_SWITCH_MULTILEVEL               =0x26 ;
   private static final int COMMAND_CLASS_SWITCH_ALL                      =0x27	;
   private static final int COMMAND_CLASS_SENSOR_BINARY                   =0x30	;
   private static final int COMMAND_CLASS_SENSOR_MULTILEVEL               =0x31	;
   private static final int COMMAND_CLASS_SENSOR_ALARM                    =0x9c	;
   private static final int COMMAND_CLASS_ALARM                           =0x71	;
   private static final int COMMAND_CLASS_MULTI_CMD                       =0x8F	;
   private static final int COMMAND_CLASS_CLIMATE_CONTROL_SCHEDULE        =0x46	;
   private static final int COMMAND_CLASS_CLOCK                           =0x81	;
   private static final int COMMAND_CLASS_ASSOCIATION                     =0x85	;
   private static final int COMMAND_CLASS_CONFIGURATION                   =0x70	;
   private static final int COMMAND_CLASS_MANUFACTURER_SPECIFIC           =0x72	;
   private static final int COMMAND_CLASS_APPLICATION_STATUS              =0x22	;
   private static final int COMMAND_CLASS_ASSOCIATION_COMMAND_CONFIGURATION=0x9B;
   private static final int COMMAND_CLASS_AV_CONTENT_DIRECTORY_MD         =0x95	;
   private static final int COMMAND_CLASS_AV_CONTENT_SEARCH_MD            =0x97	;
   private static final int COMMAND_CLASS_AV_RENDERER_STATUS              =0x96	;
   private static final int COMMAND_CLASS_AV_TAGGING_MD                   =0x99	;
   private static final int COMMAND_CLASS_BASIC_WINDOW_COVERING           =0x50	;
   private static final int COMMAND_CLASS_CHIMNEY_FAN                     =0x2A	;
   private static final int COMMAND_CLASS_COMPOSITE                       =0x8D	;
   private static final int COMMAND_CLASS_DOOR_LOCK                       =0x62	;
   private static final int COMMAND_CLASS_ENERGY_PRODUCTION               =0x90	;
   private static final int COMMAND_CLASS_FIRMWARE_UPDATE_MD              =0x7a	;
   private static final int COMMAND_CLASS_GEOGRAPHIC_LOCATION             =0x8C	;
   private static final int COMMAND_CLASS_GROUPING_NAME                   =0x7B	;
   private static final int COMMAND_CLASS_HAIL                            =0x82	;
   private static final int COMMAND_CLASS_INDICATOR                       =0x87	;
   private static final int COMMAND_CLASS_IP_CONFIGURATION                =0x9A	;
   private static final int COMMAND_CLASS_LANGUAGE                        =0x89	;
   private static final int COMMAND_CLASS_LOCK                            =0x76	;
   private static final int COMMAND_CLASS_MANUFACTURER_PROPRIETARY        =0x91	;
   private static final int COMMAND_CLASS_METER_PULSE                     =0x35	;
   private static final int COMMAND_CLASS_METER                           =0x32	;
   private static final int COMMAND_CLASS_MTP_WINDOW_COVERING             =0x51	;
   private static final int COMMAND_CLASS_MULTI_INSTANCE_ASSOCIATION      =0x8E	;
   private static final int COMMAND_CLASS_MULTI_INSTANCE                  =0x60	;
   private static final int COMMAND_CLASS_NO_OPERATION                    =0x00	;
   private static final int COMMAND_CLASS_NODE_NAMING                     =0x77	;
   private static final int COMMAND_CLASS_NON_INTEROPERABLE               =0xf0	;
   private static final int COMMAND_CLASS_POWERLEVEL                      =0x73	;
   private static final int COMMAND_CLASS_PROPRIETARY                     =0x88	;
   private static final int COMMAND_CLASS_PROTECTION                      =0x75	;
   private static final int COMMAND_CLASS_REMOTE_ASSOCIATION_ACTIVATE     =0x7c	;
   private static final int COMMAND_CLASS_REMOTE_ASSOCIATION              =0x7d	;
   private static final int COMMAND_CLASS_SCENE_ACTIVATION                =0x2b	;
   private static final int COMMAND_CLASS_SCENE_ACTUATOR_CONF             =0x2C	;
   private static final int COMMAND_CLASS_SCENE_CONTROLLER_CONF           =0x2D	;
   private static final int COMMAND_CLASS_SCREEN_ATTRIBUTES               =0x93	;
   private static final int COMMAND_CLASS_SCREEN_MD                       =0x92	;
   private static final int COMMAND_CLASS_SECURITY                        =0x98	;
   private static final int COMMAND_CLASS_SENSOR_CONFIGURATION            =0x9E	;
   private static final int COMMAND_CLASS_SILENCE_ALARM                   =0x9d;
   private static final int COMMAND_CLASS_SIMPLE_AV_CONTROL               =0x94	;
   private static final int COMMAND_CLASS_SWITCH_BINARY                   =0x25	;
   private static final int COMMAND_CLASS_SWITCH_TOGGLE_BINARY            =0x28	;
   private static final int COMMAND_CLASS_SWITCH_TOGGLE_MULTILEVEL        =0x29	;
   private static final int COMMAND_CLASS_THERMOSTAT_FAN_MODE             =0x44	;
   private static final int COMMAND_CLASS_THERMOSTAT_FAN_STATE            =0x45	;
   private static final int COMMAND_CLASS_THERMOSTAT_HEATING              =0x38	;
   private static final int COMMAND_CLASS_THERMOSTAT_MODE                 =0x40	;
   private static final int COMMAND_CLASS_THERMOSTAT_OPERATING_STATE      =0x42	;
   private static final int COMMAND_CLASS_THERMOSTAT_SETBACK              =0x47	;
   private static final int COMMAND_CLASS_THERMOSTAT_SETPOINT             =0x43	;
   private static final int COMMAND_CLASS_TIME_PARAMETERS                 =0x8B	;
   private static final int COMMAND_CLASS_TIME                            =0x8a	;
   private static final int COMMAND_CLASS_USER_CODE                       =0x63	;
   private static final int COMMAND_CLASS_ZIP_ADV_CLIENT                  =0x34	;
   private static final int COMMAND_CLASS_ZIP_ADV_SERVER                  =0x33	;
   private static final int COMMAND_CLASS_ZIP_ADV_SERVICES                =0x2F	;
   private static final int COMMAND_CLASS_ZIP_CLIENT                      =0x2e	;
   private static final int COMMAND_CLASS_ZIP_SERVER                      =0x24	;
   private static final int COMMAND_CLASS_ZIP_SERVICES                    =0x23 ;
    
    
}

