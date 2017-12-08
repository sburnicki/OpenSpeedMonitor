<script type="text/x-template" id="threshold-measured-event-vue">
<div>
    <div v-if="Object.keys(measuredEventItem.measuredEvent).length">
        <label class="measuredEventLabel">{{ measuredEventItem.measuredEvent.name }}</label>
        <button type="button"
                class="margins btn btn-default"
                :disabled="availableMeasurands.length === 0"
                @click="addMetric()">
            <i class="fa text-success fa-lg fa-plus"></i>
        </button>
    </div>
    <div v-else>
        <select class="form-control thresholdSelects" v-model="selectedMeasuredEvent">
            <option v-for="measuredEvent in availableMeasuredEvents" :value="measuredEvent">{{measuredEvent.name}}</option>
        </select>
    </div>


    <div v-for="thresholdList in measuredEventItem.thresholdList">
        <div>
            <threshold :threshold-item="thresholdList"
                           :available-measurands="availableMeasurands"
                           v-on:delete-threshold="deleteThreshold"
                           v-on:update-threshold="updateThreshold"
                           v-on:create-threshold="createThreshold"
                           v-on:remove-new-threshold="removeNewThreshold">
            </threshold>
        </div>
    </div>
</div>
</script>

<g:render template="/job/threshold/thresholdVue"/>

<asset:javascript src="/job/threshold/measuredEventVue.js"/>