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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException>
{
	/**
	 * Map an exception to a {@link javax.ws.rs.core.Response}.
	 *
	 * @param exception the exception to map to a response.
	 * @return a response mapped from the supplied exception.
	 */
	@Override
	public Response toResponse(final NotFoundException exception)
	{
		String info = "{ error: \"The requested resource hasn't been found\" }";

		return Response
				.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity(info)
				.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
