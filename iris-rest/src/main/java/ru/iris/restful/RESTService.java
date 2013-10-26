package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Module;
import ru.iris.common.devices.ZWaveDevice;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 06.09.12
 * Time: 17:24
 * License: GPL v3
 */



@Path("/")
public class RESTService
{
    private static Logger log = LoggerFactory.getLogger(RESTService.class.getName());
    private static I18N i18n = new I18N();

        @GET
        @Path("/device/get/{uuid}")
        @Produces(MediaType.TEXT_PLAIN)
        public String device(@PathParam("uuid") String uuid) throws IOException, SQLException {

            log.info(i18n.message("rest.get.device.get.0", uuid));

            @NonNls ResultSet rs = Service.sql.select("SELECT * FROM DEVICES");
            ArrayList<ZWaveDevice> zDevices = new ArrayList<ZWaveDevice>();
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

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

                    @NonNls ResultSet rsv = Service.sql.select("SELECT * FROM devicelabels WHERE UUID='"+rs.getString("uuid")+"'");
                        while (rsv.next()) {
                            zdevice.setValue(rsv.getString("label"), rsv.getString("value"));
                        }

                    if(rs.getString("uuid").equals(uuid))
                        return gson.toJson(zdevice);

                    zDevices.add(zdevice);
                }

                rs.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return gson.toJson(zDevices);
        }

    /////////////////////////////////////
    // Команда
    /////////////////////////////////////

    @GET
    @Path("/cmd/{text}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String cmd(@PathParam("text") String text) throws JMSException, SQLException {

        log.info(i18n.message("rest.get.cmd.0", text));

        Service.msg.simpleSendMessage("event.command", "cmd", text);

        Message mess;
        @NonNls MapMessage m = null;

        while ((mess = Service.messageConsumer.receive (0)) != null)
        {
            m = (MapMessage) mess;

            if(m.getStringProperty("qpid.subject").equals ("event.command"))
            {
                log.info (i18n.message("rest.got.0.command", m.getStringProperty("cmd")));

                @NonNls ResultSet rs = Service.sql.select("SELECT name, command, param FROM modules");

                while (rs.next())
                {
                    String name = rs.getString("name");
                    String comm = rs.getString("command");
                    String param = rs.getString("param");

                    if (m.getStringProperty("cmd").contains(comm)) {
                        try {

                            Class cl = Class.forName("modules." + name);
                            Module execute = (Module) cl.newInstance();
                            execute.run(param);

                        } catch (Exception e) {
                            log.info(i18n.message("module.error.at.loading.module.0.with.params.1", name, param));
                            e.printStackTrace ();
                        }
                    }
                }

                rs.close();
            }
        }

        return "{ status: "+i18n.message("done")+" }";
    }

    /////////////////////////////////////
    // Cинтез речи
    /////////////////////////////////////

    @GET
    @Path("/speak/{text}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String speak(@PathParam("text") String text) throws JMSException {

        log.info(i18n.message("rest.get.speak.0", text));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("text", text);
        message.setDoubleProperty("confidence", 100);
        message.setStringProperty ("qpid.subject", "event.speak");

        Service.messageProducer.send (message);

        return "{ status: "+i18n.message("done")+" }";
    }

    /////////////////////////////////////
    // Управление значениями устройств
    /////////////////////////////////////

    @GET
    @Path("/device/{uuid}/enable")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devEnable(@PathParam("uuid") String uuid) throws JMSException {

        log.info(i18n.message("rest.enable.0.device", uuid));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "enable");
        message.setStringProperty("uuid", uuid);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "{ status: "+i18n.message("done")+" }";
    }

    @GET
    @Path("/device/{uuid}/disable")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devDisable(@PathParam("uuid") String uuid) throws JMSException {

        log.info(i18n.message("rest.disable.0.device", uuid));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "disable");
        message.setStringProperty("uuid", uuid);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "{ status: "+i18n.message("done")+" }";
    }

    @GET
    @Path("/device/{uuid}/level/{level}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devSetLevel(@PathParam("uuid") String uuid, @PathParam("level") short level) throws JMSException {

        log.info(i18n.message("rest.set.level.0.on.1.device", level, uuid));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "setlevel");
        message.setShortProperty("level", level);
        message.setStringProperty("uuid", uuid);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "{ status: "+i18n.message("done")+" }";
    }

    @GET
    @Path("/device/all/{state}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devAllState(@PathParam("state") String state) throws JMSException
    {
        log.info(i18n.message("rest.switch.all.devices.to.0.state", state));
        Service.msg.simpleSendMessage("event.devices.setvalue", "command", "all"+state);

        return "{ status: "+i18n.message("done")+" }";
    }

    @GET
    @Path("/status/module/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String status(@PathParam("name") String name) throws IOException, SQLException {

        log.info(i18n.message("rest.get.status.module.0", name));

        @NonNls ResultSet rs = Service.sql.select("SELECT * FROM MODULESTATUS WHERE name='"+name+"'");
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

        @NonNls HashMap<String,Object> result = new HashMap<>();

        try {
            while (rs.next()) {
                result.put("id",rs.getInt("id"));
                result.put("name",rs.getString("name"));
                result.put("lastseen",rs.getString("lastseen"));
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gson.toJson(result);
    }

    @GET
    @Path("/status/module/all")
    @Produces(MediaType.TEXT_PLAIN)
    public String status() throws IOException, SQLException {

        log.info(i18n.message("rest.get.status.module.all"));

        @NonNls ResultSet rs = Service.sql.select("SELECT * FROM MODULESTATUS");
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

        @NonNls HashMap<String,Object> obj = new HashMap<>();
        ArrayList result = new ArrayList();

        try {
            while (rs.next()) {
                obj.put("id", rs.getInt("id"));
                obj.put("name",rs.getString("name"));
                obj.put("lastseen",rs.getString("lastseen"));

                result.add(obj.clone());
                obj.clear();
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gson.toJson(result);
    }
}
