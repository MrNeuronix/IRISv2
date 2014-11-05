INSERT INTO iris.events (id, subject, script, is_enabled) VALUES (1, 'event.devices.noolite.setvalue', 'turnOffLater.js', 1);
INSERT INTO iris.events (id, subject, script, is_enabled) VALUES (2, 'event.devices.noolite.value.set', 'turnOffLater.js', 1);
INSERT INTO iris.events (id, subject, script, is_enabled) VALUES (5, 'event.devices.setvalue', 'dimmerChange.js', 1);
INSERT INTO iris.events (id, subject, script, is_enabled) VALUES (6, 'event.devices.zwave.value.changed', 'floodSensor.js', 1);