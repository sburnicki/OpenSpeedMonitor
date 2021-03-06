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

package de.iteratec.osm.measurement.script

import de.iteratec.osm.csi.Page
import de.iteratec.osm.measurement.schedule.Job
import de.iteratec.osm.result.MeasuredEvent
import de.iteratec.osm.result.PageService
import de.iteratec.osm.util.ControllerUtils
import de.iteratec.osm.util.DataIntegrityViolationExpectionUtil
import grails.converters.JSON
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus

class ScriptController {
    PageService pageService
    ScriptService scriptService

    private String getScriptI18n() {
        return message(code: 'de.iteratec.iss.script', default: 'Skript')
    }

    void redirectIfNotFound(Script script, def id) {
        def flashMessageArgs = [getScriptI18n(), id];
        if (!script) {
            flash.message = message(code: 'default.not.found.message', args: flashMessageArgs)
            redirect(action: "list")
        }
    }

    public Map<String, Object> list() {
        List<Script> scripts
        if (params.sort == 'testedPageNames') {
            scripts = Script.list()
            scripts.sort { it.testedPageNames.join(', ') }
            if (params.order == 'desc')
                scripts.reverse(true)
        } else {
            scripts = Script.list(params)
        }
        [scripts: scripts]
    }

    def index() {
        redirect(action: 'list')
    }

    def create() {
        [script: new Script(params), pages: Page.list(), measuredEvents: MeasuredEvent.list() as JSON, archivedScripts: ""]
    }

    def save() {
        Script script = new Script(params)
        if (!script.save(flush: true)) {
            render(view: 'create', model: [script: script, pages: Page.list(), measuredEvents: MeasuredEvent.list() as JSON, archivedScripts: ""])
            return
        }
        scriptService.createNewPagesAndMeasuredEvents(new ScriptParser(pageService, script.navigationScript))
        def flashMessageArgs = [getScriptI18n(), script.label]
        flash.message = message(code: 'default.created.message', args: flashMessageArgs)
        redirect(action: "list")
    }

    def edit() {
        Script script = Script.get(params.id)
        redirectIfNotFound(script, params.id)
//		 only MeasuredEvents whose names do not contain spaces
        [script: script, pages: Page.list() as JSON, measuredEvents: MeasuredEvent.list() as JSON, archivedScripts: getListOfArchivedScripts(script)]
    }

    private getListOfArchivedScripts(Script script) {
        def archiveParams = [:]
        archiveParams.order = "desc"
        archiveParams.sort = "dateCreated"
        def returnList = []
        ArchivedScript.createCriteria().list(archiveParams) {
            eq("script", script)
            projections {
                property("id", "id")
                property("dateCreated", "dateCreated")
                property("versionDescription", "versionDescription")
            }
        }.each {
            def returnValue = [:]
            returnValue["id"] = it[0]
            returnValue["dateCreated"] = it[1]
            returnValue["versionDescription"] = it[2]
            returnList.add(returnValue)
        }
        return returnList
    }

    def update() {
        Script s = Script.get(params.id)
        def flashMessageArgs = [getScriptI18n(), s.label]
        redirectIfNotFound(s, params.id)
        ArchivedScript archivedScript = createArchiveScript(s)
        if (params.version) {
            def version = params.version.toLong()
            if (s.version > version) {
                s.errors.rejectValue("version", "default.optimistic.locking.failure",
                        [getScriptI18n()] as Object[],
                        "Another user has updated this script while you were editing")
                render(view: 'edit', model: [script: s, pages: Page.list() as JSON, measuredEvents: MeasuredEvent.list() as JSON, archivedScripts: getListOfArchivedScripts(s)])
                return
            }
        }

        s.properties = params;
        if (!s.save(flush: true)) {
            render(view: 'edit', model: [script: s, pages: Page.list() as JSON, measuredEvents: MeasuredEvent.list() as JSON, archivedScripts: getListOfArchivedScripts(s)])
            return
        }
        scriptService.createNewPagesAndMeasuredEvents(new ScriptParser(pageService, s.navigationScript))
        archivedScript.save(failOnError: true, flush: true)

        flash.message = message(code: 'default.updated.message', args: flashMessageArgs)
        redirect(action: 'edit', id: s.id)
    }

    private ArchivedScript createArchiveScript(Script s) {
        return new ArchivedScript(versionDescription: s.description,
                description: s.description,
                label: s.label,
                navigationScript: s.navigationScript,
                script: s)

    }

    def delete() {
        Script script = Script.get(params.id)
        redirectIfNotFound(script, params.id)
        def flashMessageArgs = [getScriptI18n(), script.label]

        try {
            script.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: flashMessageArgs)
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            String dependency = DataIntegrityViolationExpectionUtil.getEntityNameForForeignKeyViolation(e)
            if (dependency) {
                flashMessageArgs.add(dependency)
                flash.message = message(code: 'default.not.deleted.foreignKeyConstraint.message', args: flashMessageArgs)
            } else {
                flash.message = message(code: 'default.not.deleted.message', args: flashMessageArgs)
            }
            redirect(action: "edit", id: script.id)
        }
    }

    def parseScript(String navigationScript) {
        ScriptParser parser = new ScriptParser(pageService, navigationScript)
        Map output = [:]
        if (parser.warnings)
            output.warnings = parser.warnings.groupBy { it.lineNumber }
        else
            output.warnings = []
        if (parser.errors)
            output.errors = parser.errors.groupBy { it.lineNumber }
        else
            output.errors = []
        output.newPages = parser.newPages
        output.newMeasuredEvents = parser.newMeasuredEvents.collect { "${it.key} (${it.value})" }
        output.correctPageName = parser.correctPageName.groupBy { it.lineNumber }
        output.steps = parser.steps
        output.variables = PlaceholdersUtility.getPlaceholdersUsedInScript(navigationScript).unique()
        render output as JSON
    }

    /**
     * Collects all measured events for a script.
     *
     * @param scriptId The selected script id.
     * @return All measured events for the script id.
     */
	def getMeasuredEventsForScript(String scriptId) {
		Long id = Long.parseLong(scriptId)
		Script script = Script.get(id)
		ScriptParser parser = new ScriptParser(pageService, script.navigationScript)

		def output = parser.getAllMeasuredEvents(script.navigationScript).collect()

		render output as JSON
	}

    def getArchivedNavigationScript(long scriptId) {
        def navigationScript = ArchivedScript.findById(scriptId).navigationScript
        ControllerUtils.sendObjectAsJSON(response, [
                navigationScript: navigationScript
        ])
    }

    def getParsedScript(long scriptId, long jobId) {
        Script script = Script.get(scriptId)
        Job job = Job.get(jobId)
        String content = ""
        if (job && script) {
            content = script.getParsedNavigationScript(job)
        }
        ControllerUtils.sendResponseAsStreamWithoutModifying(response, HttpStatus.OK, content)
    }

    def loadArchivedScript(long archivedScriptId) {
        Script s = Script.get(params.id)
        def flashMessageArgs = [getScriptI18n(), s.label]
        createArchiveScript(s).save(failOnError: true, flush: true)
        ArchivedScript archivedScript = ArchivedScript.get(archivedScriptId)
        s.label = archivedScript.label
        s.description = archivedScript.description
        s.navigationScript = archivedScript.navigationScript
        s.save(failOnError: true, flush: true)
        flash.message = message(code: 'script.versionControl.load.success', args: flashMessageArgs)
        redirect(action: "edit", id: s.id)
    }

    def updateVersionDescriptionUrl(long archivedScriptId, String newVersionDescription) {
        def archivedScript = ArchivedScript.get(archivedScriptId)
        archivedScript.versionDescription = newVersionDescription
        archivedScript.save(failOnError: true, flush: true)
        ControllerUtils.sendResponseAsStreamWithoutModifying(response, HttpStatus.OK, newVersionDescription)
    }
}
