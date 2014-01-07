/**
 * @author Nikolay A. Viguro
 * Date: 27.11.13
 * Time: 12:19
 * This is test script for event engine of IRISv2
 */

// importing all classes in package (like import ru.iris.common.* in java)
importPackage(Packages.ru.iris.common);

    var label = advertisement.getLabel();
    var value = advertisement.getValue();
    var device = advertisement.getDevice();

    if (label == "Level") {
        // lets speak!
        out.println("Уровень яркости на устройстве " + device.getNode() + " выставлен на " + value + " процентов");
        new Speak().say("Уровень яркости на устройстве " + device.getNode() + " выставлен на " + value + " процентов");
    }
