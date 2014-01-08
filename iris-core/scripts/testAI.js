/**
 * @author Nikolay A. Viguro
 * Date: 27.11.13
 * Time: 12:19
 * This is test script for event engine of IRISv2
 */

// importing all classes in package (like import ru.iris.common.* in java)
importPackage(Packages.ru.iris.common);

var action = advertisement.getResponse().getOutcome().getEntities().get("action").getValue();

if (action == "on") {
        new Speak().say("Включаю свет!");
}
else if (action == "off") {
    new Speak().say("Выключаю свет!");
}
else if (action == "dim") {
    new Speak().say("Приглушаю свет!");
}
else if (action == "bright") {
    new Speak().say("Делаю ярче свет!");
}
else
{
    new Speak().say("Неизвестная команда!");
}
