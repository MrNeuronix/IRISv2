/**
 * @author Nikolay A. Viguro
 * Date: 26.11.13
 * Time: 16:39
 * This is test script for event engine of IRISv2
 */

out.println("TEST OK!");

obj = { run: function () {
    out.println("hi");
} }
obj.run();

defineClass("ru.iris.common.script.SpeakJS");
new SpeakJS().say("Test java script engine successfully");

