package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;

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
 * Date: 12.11.13
 * Time: 10:17
 * License: GPL v3
 */

@Path("/status")
public class StatusREST {

    private static Logger log = LoggerFactory.getLogger(StatusREST.class.getName());
    private static I18N i18n = new I18N();
    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    @GET
    @Path("/all")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String status() throws IOException, SQLException {

        log.info(i18n.message("rest.get.status.module.all"));

         ResultSet rs = Service.sql.select("SELECT * FROM MODULESTATUS");

         HashMap<String, Object> obj = new HashMap<>();
        ArrayList result = new ArrayList();

        try {
            while (rs.next()) {
                obj.put("name", rs.getString("name"));
                obj.put("state", rs.getString("state"));
                obj.put("lastseen", rs.getString("lastseen"));

                result.add(obj.clone());
                obj.clear();
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gson.toJson(result);
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String status(@PathParam("name") String name) throws IOException, SQLException {

        log.info(i18n.message("rest.get.status.module.0", name));

         ResultSet rs = Service.sql.select("SELECT * FROM MODULESTATUS WHERE name='" + name + "'");

         HashMap<String, Object> result = new HashMap<>();

        try {
            while (rs.next()) {
                result.put("id", rs.getInt("id"));
                result.put("name", rs.getString("name"));
                result.put("state", rs.getString("state"));
                result.put("lastseen", rs.getString("lastseen"));
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gson.toJson(result);
    }
}
