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
import com.avaje.ebean.Expr;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zwave4j.*;
import ru.iris.common.Config;
import ru.iris.common.Utils;
import ru.iris.common.database.model.Log;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.SetDeviceLevelAdvertisement;
import ru.iris.common.messaging.model.devices.zwave.*;

import java.util.List;
import java.util.UUID;

public class ZWaveService implements Runnable
{

	private final Logger LOGGER = LogManager.getLogger(ZWaveService.class.getName());
	private final Gson gson = new GsonBuilder().create();
	// Adverstiments
	private final ZWaveDriverReady zWaveDriverReady = new ZWaveDriverReady();
	private final ZWaveDriverFailed zWaveDriverFailed = new ZWaveDriverFailed();
	private final ZWaveDriverReset zWaveDriverReset = new ZWaveDriverReset();
	private final ZWaveNodeNaming zWaveNodeNaming = new ZWaveNodeNaming();
	private final ZWaveNodeAdded zWaveNodeAdded = new ZWaveNodeAdded();
	private final ZWaveNodeEvent zWaveNodeEvent = new ZWaveNodeEvent();
	private final ZWaveNodeNew zWaveNodeNew = new ZWaveNodeNew();
	private final ZWaveNodeProtocolInfo zWaveNodeProtocolInfo = new ZWaveNodeProtocolInfo();
	private final ZWaveNodeQueriesComplete zWaveNodeQueriesComplete = new ZWaveNodeQueriesComplete();
	private final ZWaveNodeRemoved zWaveNodeRemoved = new ZWaveNodeRemoved();
	private final ZWaveAwakeNodesQueried zWaveAwakeNodesQueried = new ZWaveAwakeNodesQueried();
	private final ZWaveAllNodesQueried zWaveAllNodesQueried = new ZWaveAllNodesQueried();
	private final ZWaveAllNodesQueriedSomeDead zWaveAllNodesQueriedSomeDead = new ZWaveAllNodesQueriedSomeDead();
	private final ZWaveDeviceValueAdded zWaveDeviceValueAdded = new ZWaveDeviceValueAdded();
	private final ZWaveDeviceValueChanged zWaveDeviceValueChanged = new ZWaveDeviceValueChanged();
	private final ZWaveDeviceValueRemoved zWaveDeviceValueRemoved = new ZWaveDeviceValueRemoved();
	private final ZWaveEssentialNodeQueriesComplete zWaveEssentialNodeQueriesComplete = new ZWaveEssentialNodeQueriesComplete();
	private final ZWavePolling zWavePolling = new ZWavePolling();
	private long homeId;
	private boolean ready = false;
	private List<Device> devices;
	private boolean initComplete = false;
	private boolean shutdown = false;
	private JsonMessaging messaging;

	public ZWaveService()
	{
		Thread t = new Thread(this);
		t.setName("ZWave Service");
		t.start();
	}

	@Override
	public synchronized void run()
	{
		Config config = Config.getInstance();

		messaging = new JsonMessaging(UUID.randomUUID(), "devices-zwave");

		devices = Ebean.find(Device.class)
				.where().eq("source", "zwave").findList();

		NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

		final Options options = Options.create(config.get("openzwaveCfgPath"), "", "");
		options.addOptionBool("ConsoleOutput", Boolean.parseBoolean(config.get("zwaveDebug")));
		options.addOptionString("UserPath", "conf/", true);
		options.lock();

		final Manager manager = Manager.create();

		final NotificationWatcher watcher = new NotificationWatcher()
		{

			@Override
			public void onNotification(Notification notification, Object context)
			{

				short node = notification.getNodeId();

				switch (notification.getType())
				{
					case DRIVER_READY:
						homeId = notification.getHomeId();
						LOGGER.info("Driver ready. Home ID: " + homeId);
						messaging.broadcast("event.devices.zwave.driver.ready", zWaveDriverReady.set(homeId));
						break;
					case DRIVER_FAILED:
						LOGGER.info("Driver failed");
						messaging.broadcast("event.devices.zwave.driver.failed", zWaveDriverFailed);
						break;
					case DRIVER_RESET:
						LOGGER.info("Driver reset");
						messaging.broadcast("event.devices.zwave.driver.reset", zWaveDriverReset);
						break;
					case AWAKE_NODES_QUERIED:
						LOGGER.info("Awake nodes queried");
						ready = true;
						messaging.broadcast("event.devices.zwave.awakenodesqueried", zWaveAwakeNodesQueried);
						break;
					case ALL_NODES_QUERIED:
						LOGGER.info("All node queried");
						manager.writeConfig(homeId);
						ready = true;
						messaging.broadcast("event.devices.zwave.allnodesqueried", zWaveAllNodesQueried);
						break;
					case ALL_NODES_QUERIED_SOME_DEAD:
						LOGGER.info("All node queried, some dead");
						messaging.broadcast("event.devices.zwave.allnodesqueriedsomedead", zWaveAllNodesQueriedSomeDead);
						break;
					case POLLING_ENABLED:
						LOGGER.info("Polling enabled");
						messaging.broadcast("event.devices.zwave.polling.disabled", zWavePolling.set(getZWaveDeviceByNode(notification.getNodeId()), true));
						break;
					case POLLING_DISABLED:
						LOGGER.info("Polling disabled");
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
						LOGGER.info("Update info for node " + node);
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

						// check empty label
						if (Manager.get().getValueLabel(notification.getValueId()).isEmpty())
							break;

						String nodeType = manager.getNodeType(homeId, node);
						Device zw = null;

						switch (nodeType)
						{
							case "Portable Remote Controller":

								zw = addZWaveDeviceOrValue("controller", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							//////////////////////////////////

							case "Multilevel Power Switch":

								zw = addZWaveDeviceOrValue("dimmer", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							//////////////////////////////////

							case "Routing Alarm Sensor":

								zw = addZWaveDeviceOrValue("alarmsensor", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							case "Binary Power Switch":

								zw = addZWaveDeviceOrValue("switch", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							case "Routing Binary Sensor":

								zw = addZWaveDeviceOrValue("binarysensor", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							//////////////////////////////////

							case "Routing Multilevel Sensor":

								zw = addZWaveDeviceOrValue("multilevelsensor", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							//////////////////////////////////

							case "Simple Meter":

								zw = addZWaveDeviceOrValue("metersensor", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							//////////////////////////////////

							case "Simple Window Covering":

								zw = addZWaveDeviceOrValue("drapes", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
								break;

							//////////////////////////////////

							case "Setpoint Thermostat":

								zw = addZWaveDeviceOrValue("thermostat", notification);
								messaging.broadcast("event.devices.zwave.value.added",
										zWaveDeviceValueAdded.set(
												zw,
												Manager.get().getValueLabel(notification.getValueId()),
												String.valueOf(Utils.getValue(notification.getValueId()))));
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
						}

						boolean isNewDeviceValue = false;
						DeviceValue udv = Ebean.find(DeviceValue.class).where().and(Expr.eq("device_id", zw.getId()), Expr.eq("label", manager.getValueLabel(notification.getValueId()))).findUnique();

						// new device
						if (udv == null)
						{
							udv = new DeviceValue();
							isNewDeviceValue = true;
						}

						udv.setLabel(manager.getValueLabel(notification.getValueId()));
						udv.setValueType(Utils.getValueType(notification.getValueId()));
						udv.setValueId(notification.getValueId());
						udv.setValueUnits(Manager.get().getValueUnits(notification.getValueId()));
						udv.setValue(String.valueOf(Utils.getValue(notification.getValueId())));
						udv.setReadonly(Manager.get().isValueReadOnly(notification.getValueId()));
						udv.setUuid(zw.getUuid());

						zw.updateValue(udv);

						// Update or save device value
						if (!isNewDeviceValue)
							Ebean.update(udv);
						else
							Ebean.save(udv);

						break;
					case VALUE_REMOVED:

						Device zrZWaveDevice = getZWaveDeviceByNode(node);

						if (zrZWaveDevice == null)
						{
							LOGGER.info("While save remove value, node " + node + " not found");
							break;
						}

						DeviceValue dv = Ebean.find(DeviceValue.class).where().and(Expr.eq("device_id", zrZWaveDevice.getId()), Expr.eq("label", manager.getValueLabel(notification.getValueId()))).findUnique();

						if (dv != null)
						{
							zrZWaveDevice.removeValue(dv);
						}

						if (initComplete)
						{
							Ebean.update(zrZWaveDevice);
						}

						messaging.broadcast("event.devices.zwave.value.removed",
								zWaveDeviceValueRemoved.set(
										zrZWaveDevice,
										Manager.get().getValueLabel(notification.getValueId()),
										String.valueOf(Utils.getValue(notification.getValueId()))));

						if (!manager.getValueLabel(notification.getValueId()).isEmpty())
						{
							LOGGER.info("Node " + zrZWaveDevice.getNode() + ": Value " + manager.getValueLabel(notification.getValueId()) + " removed");
						}

						break;
					case VALUE_CHANGED:

						Device zWaveDevice = getZWaveDeviceByNode(node);

						if (zWaveDevice == null)
						{
							break;
						}

						// Check for awaked after sleeping nodes
						if (manager.isNodeAwake(homeId, zWaveDevice.getNode()) && zWaveDevice.getStatus().equals("Sleeping"))
						{
							LOGGER.info("Setting node " + zWaveDevice.getNode() + " to LISTEN state");
							zWaveDevice.setStatus("Listening");
						}

						ValueId valueId = gson.fromJson(
								zWaveDevice.getValue(
										manager.getValueLabel(notification.getValueId())
								).getValueId(),
								ValueId.class);

						// break if same value
						try
						{
							if (Utils.getValue(valueId) == Utils.getValue(notification.getValueId()))
							{
								LOGGER.debug("Same value. Breaking");
								break;
							}
						}
						catch (NullPointerException e)
						{
							LOGGER.error("Error while change value: " + e.toString());
							e.printStackTrace();
							//break;
						}

						LOGGER.info("Node " +
								zWaveDevice.getNode() + ": " +
								"Value for label \"" + manager.getValueLabel(notification.getValueId()) + "\" changed  --> " +
								"\"" + Utils.getValue(notification.getValueId()) + "\"");

						boolean isNewDeviceValueChg = false;
						DeviceValue udvChg = Ebean.find(DeviceValue.class).where().and(Expr.eq("device_id", zWaveDevice.getId()), Expr.eq("label", manager.getValueLabel(notification.getValueId()))).findUnique();

						// new device
						if (udvChg == null)
						{
							udvChg = new DeviceValue();
							isNewDeviceValueChg = true;
						}

						udvChg.setLabel(manager.getValueLabel(notification.getValueId()));
						udvChg.setValueType(Utils.getValueType(notification.getValueId()));
						udvChg.setValueId(notification.getValueId());
						udvChg.setValueUnits(Manager.get().getValueUnits(notification.getValueId()));
						udvChg.setValue(String.valueOf(Utils.getValue(notification.getValueId())));
						udvChg.setReadonly(Manager.get().isValueReadOnly(notification.getValueId()));
						udvChg.setUuid(zWaveDevice.getUuid());

						zWaveDevice.updateValue(udvChg);

						// Update or save device value
						if (!isNewDeviceValueChg)
							Ebean.update(udvChg);
						else
							Ebean.save(udvChg);

						// log change
						/////////////////////////////////
						Log logChange = new Log("INFO", "Value " + manager.getValueLabel(notification.getValueId()) + " changed: " + Utils.getValue(notification.getValueId()), zWaveDevice.getUuid());
						Ebean.save(logChange);
						/////////////////////////////////

						if (initComplete)
						{
							Ebean.update(udvChg);
						}

						messaging.broadcast("event.devices.zwave.value.changed",
								zWaveDeviceValueChanged.set(
										zWaveDevice,
										Manager.get().getValueLabel(notification.getValueId()),
										String.valueOf(Utils.getValue(notification.getValueId()))));

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
		while (!ready)
		{
			try
			{
				Thread.sleep(1000);
				LOGGER.info("Still waiting");
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		LOGGER.info("Initialization complete. Found " + devices.size() + " devices");

		for (Device ZWaveDevice : devices)
		{
			// Check for dead nodes
			if (Manager.get().isNodeFailed(homeId, ZWaveDevice.getNode()))
			{
				LOGGER.info("Setting node " + ZWaveDevice.getNode() + " to DEAD state");
				ZWaveDevice.setStatus("dead");
			}

			// Check for sleeping nodes
			if (!Manager.get().isNodeAwake(homeId, ZWaveDevice.getNode()))
			{
				LOGGER.info("Setting node " + ZWaveDevice.getNode() + " to SLEEP state");
				ZWaveDevice.setStatus("sleeping");
			}

			Ebean.update(ZWaveDevice);
		}

		// reload from database for avoid Ebean.update() key duplicate error
		devices = Ebean.find(Device.class)
				.where().eq("source", "zwave").findList();

		initComplete = true;

		try
		{
			// Make sure we exit the wait loop if we receive shutdown signal.
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					shutdown = true;
				}
			}));

			messaging.subscribe("event.devices.zwave.setvalue");
			messaging.subscribe("event.devices.zwave.node.add");
			messaging.subscribe("event.devices.zwave.node.remove");
			messaging.subscribe("event.devices.zwave.cancel");
			messaging.start();

			while (!shutdown)
			{

				// Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
				final JsonEnvelope envelope = messaging.receive(100);
				if (envelope != null)
				{
					if (envelope.getObject() instanceof ZWaveSetDeviceLevelAdvertisement)
					{
						// We know of service advertisement
						final SetDeviceLevelAdvertisement advertisement = envelope.getObject();

						String uuid = advertisement.getDeviceUUID();
						String label = advertisement.getLabel();
						String level = advertisement.getValue();

						Device ZWaveDevice = getZWaveDeviceByUUID(uuid);

						if (ZWaveDevice == null)
						{
							LOGGER.info("Cant find device with UUID " + uuid);
							continue;
						}

						int node = ZWaveDevice.getNode();

						if (!label.isEmpty() && !level.isEmpty() && !ZWaveDevice.getStatus().equals("Dead"))
						{
							LOGGER.info("Setting value: " + level + " to label \"" + label + "\" on node " + node + " (UUID: " + uuid + ")");
							setValue(uuid, label, level);
						}
						else
						{
							LOGGER.info("Node: " + node + " Cant set empty value or node dead");
						}

					}
					else if (envelope.getObject() instanceof ZWaveAddNodeRequest)
					{
						LOGGER.info("Set controller into AddDevice mode");
						Manager.get().beginControllerCommand(homeId, ControllerCommand.ADD_DEVICE, new ControllerCallback()
						{
							@Override
							public void onCallback(ControllerState state,
									ControllerError err, Object context)
							{
								LOGGER.info("ZWave Command Add : {} , {}", state, err);
							}
						}, null, true);
					}
					else if (envelope.getObject() instanceof ZWaveRemoveNodeRequest)
					{
						LOGGER.info("Set controller into RemoveDevice mode");

						final ZWaveRemoveNodeRequest advertisement = envelope.getObject();

						Manager.get().beginControllerCommand(homeId, ControllerCommand.REMOVE_DEVICE, new ControllerCallback()
						{
							@Override
							public void onCallback(ControllerState state,
									ControllerError err, Object context)
							{
								LOGGER.info("ZWave Command Remove: {} , {}", state, err);
							}
						}, null, true, advertisement.getNode());

					}
					else if (envelope.getObject() instanceof ZWaveCancelCommand)
					{
						LOGGER.info("Canceling controller command");
						Manager.get().cancelControllerCommand(homeId);
					}
					else
					{
						// We received unknown request message. Lets make generic log entry.
						LOGGER.info("Received request "
								+ " from " + envelope.getSenderInstance()
								+ " to " + envelope.getReceiverInstance()
								+ " at '" + envelope.getSubject()
								+ ": " + envelope.getObject());
					}
				}
			}

			// Close JSON messaging.
			messaging.close();

		}
		catch (final Throwable t)
		{
			t.printStackTrace();
			LOGGER.error("Unexpected exception in ZWave Devices", t);
		}
	}

	private void setTypedValue(ValueId valueId, String value)
	{

		LOGGER.debug("Set type " + valueId.getType() + " to label " + Manager.get().getValueLabel(valueId));

		switch (valueId.getType())
		{
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

	private void setValue(String uuid, String label, String value)
	{
		Device device = getZWaveDeviceByUUID(uuid);

		for (DeviceValue zv : device.getValueIDs())
		{
			if (zv.getLabel().equals(label))
			{
				if (!Manager.get().isValueReadOnly(gson.fromJson(zv.getValueId(), ValueId.class)))
				{
					setTypedValue(gson.fromJson(zv.getValueId(), ValueId.class), value);
				}
				else
				{
					LOGGER.info("Value \"" + label + "\" is read-only! Skip.");
				}
			}
		}
	}

	private Device hasInstance(String key)
	{

		for (Device device : devices)
		{

			if (key.equals(device.getInternalName()))
			{
				return device;
			}
		}

		return null;
	}

	private Device getZWaveDeviceByUUID(String uuid)
	{

		for (Device device : devices)
		{

			if (uuid.equals(device.getUuid()))
			{
				return device;
			}
		}

		return null;
	}

	private Device getZWaveDeviceByNode(short id)
	{

		for (Device device : devices)
		{

			if (device.getNode() == id)
			{
				return device;
			}
		}

		return null;
	}

	private Device addZWaveDeviceOrValue(String type, Notification notification)
	{

		Device ZWaveDevice;
		String label = Manager.get().getValueLabel(notification.getValueId());
		String state = "not responding";
		String productName = Manager.get().getNodeProductName(notification.getHomeId(), notification.getNodeId());
		String manufName = Manager.get().getNodeManufacturerName(notification.getHomeId(), notification.getNodeId());

		if (Manager.get().requestNodeState(homeId, notification.getNodeId()))
		{
			state = "listening";
		}

		if ((ZWaveDevice = hasInstance("zwave/" + type + "/" + notification.getNodeId())) == null)
		{

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

			ZWaveDevice.updateValue(new DeviceValue(
					label,
					uuid,
					String.valueOf(Utils.getValue(notification.getValueId())),
					Utils.getValueType(notification.getValueId()),
					Manager.get().getValueUnits(notification.getValueId()),
					notification.getValueId(),
					Manager.get().isValueReadOnly(notification.getValueId())));

			Ebean.save(ZWaveDevice);
			ZWaveDevice = Ebean.find(Device.class).where().eq("id", ZWaveDevice.getId()).findUnique();

			LOGGER.info("Adding device " + type + " (node: " + notification.getNodeId() + ") to array");
			devices.add(ZWaveDevice);
		}
		else
		{
			ZWaveDevice.setManufName(manufName);
			ZWaveDevice.setProductName(productName);
			ZWaveDevice.setStatus(state);

			// check empty label
			if (label.isEmpty())
				return ZWaveDevice;

			LOGGER.info("Node " + ZWaveDevice.getNode() + ": Add \"" + label + "\" value \"" + Utils.getValue(notification.getValueId()) + "\"");

			DeviceValue udv = Ebean.find(DeviceValue.class).where().and(Expr.eq("device_id", ZWaveDevice.getId()), Expr.eq("label", Manager.get().getValueLabel(notification.getValueId()))).findUnique();

			if (udv != null)
			{
				udv.setLabel(label);
				udv.setUuid(ZWaveDevice.getUuid());
				udv.setValueType(Utils.getValueType(notification.getValueId()));
				udv.setValueId(notification.getValueId());
				udv.setValueUnits(Manager.get().getValueUnits(notification.getValueId()));
				udv.setValue(String.valueOf(Utils.getValue(notification.getValueId())));
				udv.setReadonly(Manager.get().isValueReadOnly(notification.getValueId()));

				ZWaveDevice.updateValue(udv);
			}
			else
			{
				ZWaveDevice.updateValue(new DeviceValue(
						label,
						ZWaveDevice.getUuid(),
						String.valueOf(Utils.getValue(notification.getValueId())),
						Utils.getValueType(notification.getValueId()),
						Manager.get().getValueUnits(notification.getValueId()),
						notification.getValueId(),
						Manager.get().isValueReadOnly(notification.getValueId())));
			}

			Ebean.update(ZWaveDevice);
		}

		return ZWaveDevice;
	}
}
