/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.iris.restful;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import ru.iris.common.Config;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

class AuthFilter implements ContainerRequestFilter
{

	// Exception thrown if user is unauthorized.
	private final static WebApplicationException unauthorized =
			new WebApplicationException(
					Response.status(Response.Status.UNAUTHORIZED)
							.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"IRIS Authorization\"")
							.entity("IRIS Authorization").build());
	private final Config config = new Config();

	@Override
	public ContainerRequest filter(ContainerRequest containerRequest)
			throws WebApplicationException
	{

		// Automatically allow certain requests.
		String method = containerRequest.getMethod();
		String path = containerRequest.getPath(true);
		if (method.equals("GET") && path.equals("application.wadl"))
		{
			return containerRequest;
		}

		// Get the authentication passed in HTTP headers parameters
		String auth = containerRequest.getHeaderValue("authorization");
		if (auth == null)
		{
			throw unauthorized;
		}

		auth = auth.replaceFirst("[Bb]asic ", "");
		String userColonPass = Base64.base64Decode(auth);

		if (!userColonPass.equals(config.getConfig().get("httpUser") + ":" + config.getConfig().get("httpPassword")))
		{
			throw unauthorized;
		}

		return containerRequest;
	}
}