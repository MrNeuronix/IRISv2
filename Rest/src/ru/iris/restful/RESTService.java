package ru.iris.restful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

        @GET
        @Path("/add/{a}/{b}")
        @Produces(MediaType.TEXT_PLAIN)
        public String addPlainText(@PathParam("a") double a, @PathParam("b") double b) {
            return (a + b) + "";
        }

        @GET
        @Path("/sub/{a}/{b}")
        @Produces(MediaType.TEXT_PLAIN)
        public String subPlainText(@PathParam("a") double a, @PathParam("b") double b) {
            return (a - b) + "";
        }

        @GET
        @Path("/add/{a}/{b}")
        @Produces(MediaType.TEXT_XML)
        public String add(@PathParam("a") double a, @PathParam("b") double b) {
            return "<?xml version=\"1.0\"?>" + "<result>" +  (a + b) + "</result>";
        }

        @GET
        @Path("/sub/{a}/{b}")
        @Produces(MediaType.TEXT_XML)
        public String sub(@PathParam("a") double a, @PathParam("b") double b) {
            return "<?xml version=\"1.0\"?>" + "<result>" +  (a - b) + "</result>";
        }
    }
