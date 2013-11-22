package ru.iris.restful;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import ru.iris.common.Config;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 11.11.13
 * Time: 15:48
 * License: GPL v3
 */

public class AuthFilter implements ContainerRequestFilter {

    private Config config = new Config();

    // Exception thrown if user is unauthorized.
    private final static WebApplicationException unauthorized =
            new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"IRIS Authorization\"")
                            .entity("IRIS Authorization").build());

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest)
            throws WebApplicationException {

        // Automatically allow certain requests.
        String method = containerRequest.getMethod();
        String path = containerRequest.getPath(true);
        if (method.equals("GET") && path.equals("application.wadl"))
            return containerRequest;

        // Get the authentication passed in HTTP headers parameters
        String auth = containerRequest.getHeaderValue("authorization");
        if (auth == null)
            throw unauthorized;

        auth = auth.replaceFirst("[Bb]asic ", "");
        String userColonPass = Base64.base64Decode(auth);

        if (!userColonPass.equals(config.getConfig().get("httpUser") + ":" + config.getConfig().get("httpPassword")))
            throw unauthorized;

        return containerRequest;
    }
}