/**
 * @author Nikolay A. Viguro
 * Date: 27.11.13
 * Time: 12:19
 * This is test script for event engine of IRISv2
 */

// importing all classes in package (like import ru.iris.common.* in java)
importPackage(Packages.ru.iris.common);

// get simple class name
// advertisment - is java object, passed to script
var clazz = advertisement.getClass().getSimpleName();

if (clazz == "ZWaveDeviceValueChanged") {
    var label = advertisement.getLabel();
    var value = advertisement.getValue();

    if (label == "Level") {
        // lets speak!
        new Speak().say("Lighting level change to " + value);
    }
}
