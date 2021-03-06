/*
* OpenSpeedMonitor (OSM)
* Copyright 2014 iteratec GmbH
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* 	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package de.iteratec.osm.api

/**
 * Used to secure api functions. Contains one boolean for each api function or group of functions that has to be secured.
 * These booleans should only be used directly
 * @author nkuhn
 * @see ApiSecurityService
 */
class ApiKey {

	String secretKey
    String description
	Boolean valid = true
	Boolean allowedForJobActivation = false
	Boolean allowedForJobDeactivation = false
    Boolean allowedForJobSetExecutionSchedule = false
    Boolean allowedForCreateEvent = false
    Boolean allowedForMeasurementActivation = false
	Boolean allowedForNightlyDatabaseCleanupActivation = false

    static mapping = {
		valid(defaultValue: true)
		allowedForJobActivation(defaultValue: false)
		allowedForJobDeactivation(defaultValue: false)
		allowedForJobSetExecutionSchedule(defaultValue: false)
		allowedForCreateEvent(defaultValue: false)
        allowedForMeasurementActivation(defaultValue: false)
		allowedForNightlyDatabaseCleanupActivation(defaultValue: false)
    }

	static constraints = {
		secretKey(nullable: false, blank: false)
        description(nullable: true)
		valid(nullable: false)
		allowedForJobActivation(nullable: false)
		allowedForJobDeactivation(nullable: false)
        allowedForJobSetExecutionSchedule(nullable: false)
        allowedForCreateEvent(nullable: false)
        allowedForMeasurementActivation(nullable: false)
		allowedForNightlyDatabaseCleanupActivation(nullable: false)
    }

}
