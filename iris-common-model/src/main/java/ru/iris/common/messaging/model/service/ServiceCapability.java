/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.iris.common.messaging.model.service;

import com.google.gson.annotations.Expose;

/**
 * Enumeration describing service capabilities
 *
 * @author Tommi S.E. Laukkanen
 */

public enum ServiceCapability
{
	/**
	 * Service can speak to user.
	 */
	@Expose
	SPEAK,
	/**
	 * Service can listen to user.
	 */
	@Expose
	LISTEN,
	/**
	 * Service can see environment.
	 */
	@Expose
	SEE,
	/**
	 * Service can sense environment variables like temperature, humidity, etc...
	 */
	@Expose
	SENSE,
	/**
	 * Service can control environment variables like light level, temperature, etc...
	 */
	@Expose
	CONTROL,
	/**
	 * System service
	 */
	@Expose
	SYSTEM
}
