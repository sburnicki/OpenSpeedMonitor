//= require pageComparison.js
//= require_self

"use strict";

var OpenSpeedMonitor = OpenSpeedMonitor || {};
OpenSpeedMonitor.ChartModules = OpenSpeedMonitor.ChartModules || {};
OpenSpeedMonitor.ChartModules.GuiHandling = OpenSpeedMonitor.ChartModules.GuiHandling || {};

OpenSpeedMonitor.ChartModules.GuiHandling.pageComparison = (function () {

    var pageComparisonChart = OpenSpeedMonitor.ChartModules.PageComparisonChart("#page-comparison-svg");
    var spinner = OpenSpeedMonitor.Spinner("#chart-container");
    var drawGraphButton = $("#graphButtonHtmlId");

    var init = function () {
        $(window).on('resize', function () {
            renderChart({}, false);
        });
        $(window).on('historyStateLoaded', function () {
            loadData(false);
        });
        $("input[name='aggregationValue']").on("change", function () {
            renderChart({aggregationValue: getAggregationValue()}, true);
        });
        drawGraphButton.click(function () {
            loadData(true);
        });

        OpenSpeedMonitor.ChartModules.GuiHandling.PageComparison.Comparisons.setShowButtonDisabledCallback(setShowButtonDisabled);
        setShowButtonDisabled(true);
    };

    var renderChart = function (data, isStateChange) {
        if (data) {
            pageComparisonChart.setData(data);
            if (isStateChange) {
                $(window).trigger("historyStateChanged");
            }
        }
        if (!data.series)  pageComparisonChart.render();
        if (data.series && getAggregationValue() === data.series[0].data[0].aggregationValue) {
            pageComparisonChart.render();
        }
    };

    var getAggregationValue = function () {
        return $('input[name=aggregationValue]:checked').val()
    };

    var handleNewData = function (data, isStateChange) {
        $("#chart-card").removeClass("hidden");
        data.aggregationValue = getAggregationValue();
        renderChart(data, isStateChange)
    };

    var setShowButtonDisabled = function (state) {
        if(!state) {
            $('#graphButtonHtmlId').removeAttr('disabled', 'disabled');
            $('#warning-no-page').hide();
        } else {
            $('#graphButtonHtmlId').attr('disabled', 'disabled');
            $('#warning-no-page').show();
        }
    };

    var loadData = function (isStateChange) {
        pageComparisonChart.resetData();
        var selectedTimeFrame = OpenSpeedMonitor.selectIntervalTimeframeCard.getTimeFrame();
        var queryData = {
            from: selectedTimeFrame[0].toISOString(),
            to: selectedTimeFrame[1].toISOString(),
            measurand: JSON.stringify(OpenSpeedMonitor.BarchartMeasurings.getValues()),
            selectedPageComparisons: JSON.stringify(OpenSpeedMonitor.ChartModules.GuiHandling.PageComparison.Comparisons.getComparisons())
        };

        spinner.start();
        getDataForAggregationValue("median", queryData, isStateChange);
        getDataForAggregationValue("avg", queryData, isStateChange);
    };

    function getDataForAggregationValue(aggregationValue, queryData, isStateChange) {
        queryData.selectedAggregationValue = aggregationValue;
        $.ajax({
            type: 'POST',
            data: queryData,
            url: OpenSpeedMonitor.urls.pageComparisonGetData,
            dataType: "json",
            success: function (data) {
                spinner.stop();
                if (!$("#error-div").hasClass("hidden"))
                    $("#error-div").addClass("hidden");

                if (!$.isEmptyObject(data)) {
                    $('#warning-no-data').hide();
                    handleNewData(data, isStateChange);
                    $("#dia-save-chart-as-png").removeClass("disabled");
                } else {
                    $('#warning-no-data').show();
                }
            },
            error: function (e) {
                spinner.stop();
                $("#error-div").removeClass("hidden");
                $("#chart-card").removeClass("hidden");
                $("#error-message").html(e.responseText);
            }
        });
    };

    init();
    return {}
})();