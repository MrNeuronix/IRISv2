package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.scheduler.Task;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 12.11.13
 * Time: 10:20
 * License: GPL v3
 */

@Path("/scheduler")
public class SchedulerREST {

    private static Logger log = LoggerFactory.getLogger(StatusREST.class.getName());
    private static I18N i18n = new I18N();
    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    @GET
    @Path("/get/all")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String schedulerGetAll() throws IOException, SQLException {

        ArrayList<Task> allTasks = new ArrayList<>();

        try {
            ResultSet rs = Service.sql.select("SELECT id FROM scheduler WHERE enabled='1' AND language='"+Service.config.get("language")+"' ORDER BY ID ASC");

            while (rs.next()) {
                allTasks.add(new Task(rs.getInt("id")));
            }

            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return gson.toJson(allTasks);
    }

    @GET
    @Path("/get/{id}")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String schedulerGet(@PathParam("id") int id) throws IOException {

        Task task;

        try {
            task = new Task(id);
        }
        catch (SQLException e)
        {
            return "{ \"error\": \"task "+id+" not found\" }";
        }

        return gson.toJson(task);
    }

}
