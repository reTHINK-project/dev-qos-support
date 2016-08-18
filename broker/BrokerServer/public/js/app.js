/**
 ******************************************************************************
 * @b Project : reThink
 *
 * @b Sub-project : QoS Broker
 *
 ******************************************************************************
 *
 *                       Copyright (C) 2016 Orange Labs
 *                       Orange Labs - PROPRIETARY
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. *
 ******************************************************************************
 *
 * @brief 
 *
 * @file
 *
 */
(function(){
    var app = angular.module('dashboard', ['cspFilters']);
    var currentFilteringUnit = "MB";

    /**
     * return the consumption percentage given the consumption and the quota
     * @param consumed {string} : consumed data
     * @param quota {string} : maximum data that can be consumed
     */
    function calculatePercentConsumed(consumed, quota){
        consumed = parseInt(consumed);
        quota = parseInt(quota);
        if (consumed === 0 | quota === 0 ){
            return 0;
        }
        return consumed/quota*100;
    }

    /**
     * get the consumption for a given csp
     * @param $http : passing the $http lib of angular
     * @param datacontainer : The data we need to edit with consumption data
     * @param cspkey : key of the csp in the provisioning database
     * @param cspIndex : index of the csp in the datacontainer
     */
    function getCSPConso($http,datacontainer,cspkey,cspIndex){
        $http.get('./getCSPConso?cspkey=' + cspkey)
        .success(function(res){
            if(res){
                datacontainer.oldcons[cspkey] = {
                    'audio' : datacontainer.provs[cspIndex].audioconsumed,
                    'video' : datacontainer.provs[cspIndex].videoconsumed,
                    'data' : datacontainer.provs[cspIndex].dataconsumed,
                };
                $.extend(datacontainer.provs[cspIndex], {
                    'audioconsumed' : parseInt(res.audio),
                    'videoconsumed' : parseInt(res.video),
                    'dataconsumed' : parseInt(res.data),
                    'audiohasrisen' : false,
                    'videohasrisen' : false,
                    'datahasrisen' : false,
                    'audiopercent' : calculatePercentConsumed(res.audio,datacontainer.provs[cspIndex].audioQuota),
                    'videopercent' : calculatePercentConsumed(res.video,datacontainer.provs[cspIndex].videoQuota),
                    'datapercent' : calculatePercentConsumed(res.data,datacontainer.provs[cspIndex].dataQuota)
                });
            }
        })
        return datacontainer;
    }

    /**
     * Reloads the consumption for any given csp (!= getCSPConso because we update the state 'hasrisen')
     * @param $http : passing the $http lib of angular
     * @param datacontainer : The data we need to edit with consumption data
     * @param cspkey : key of the csp in the provisioning database
     * @param cspIndex : index of the csp in the datacontainer
     */
    function reloadCSPConso($http,datacontainer,cspkey,cspIndex){
        $http.get('./getCSPConso?cspkey=' + cspkey).success(function(res){
            if(res){
                $.extend(datacontainer.provs[cspIndex], {
                    'audioconsumed' : parseInt(res.audio),
                    'videoconsumed' : parseInt(res.video),
                    'dataconsumed' : parseInt(res.data),
                    'audiohasrisen' : parseInt(res.audio)>datacontainer.oldcons[cspkey].audio,
                    'videohasrisen' : parseInt(res.video)>datacontainer.oldcons[cspkey].video,
                    'datahasrisen' : parseInt(res.data)>datacontainer.oldcons[cspkey].data,
                    'audiopercent' : calculatePercentConsumed(res.audio,datacontainer.provs[cspIndex].audioQuota),
                    'videopercent' : calculatePercentConsumed(res.video,datacontainer.provs[cspIndex].videoQuota),
                    'datapercent' : calculatePercentConsumed(res.data,datacontainer.provs[cspIndex].dataQuota)
                });
                console.log(datacontainer.provs[cspIndex].audiohasrisen);
                console.log(datacontainer.oldcons[cspkey].audio);
                datacontainer.oldcons[cspkey].audio = res.audio;
                datacontainer.oldcons[cspkey].video = res.video;
                datacontainer.oldcons[cspkey].data = res.data;
            }
        });
        return datacontainer;
    }

    /**
     * Creates the behavior for the table part, like requesting datas, creating a reload function
     */
    var mainController =[ '$http', '$window', function($http, $window){
        var dashboard = this;
        dashboard.provs = [];
        dashboard.order = "";
        dashboard.orderType = 1;
        dashboard.oldcons = [];
        dashboard.cspToEdit = null;
        dashboard.edit = false;
        dashboard.filteringUnit = currentFilteringUnit;
        //Getting basic csp infos (quotas, name)
        $http.get('./getAllCspInfo').success(function(data){
            dashboard.provs = data;
            for(cspIndex=0; cspIndex<data.length;cspIndex++){
                //For each csp, getting consumption data
                var cspkey = data[cspIndex].csp;
                dashboard = getCSPConso($http,dashboard,cspkey,cspIndex);
            }
        });

        //Reload function that does pretty much the same as above, but also
        //Calculates if consumption has risen or not (to display the small arrow)
        dashboard.reload = function(){
            for(cspIndex=0; cspIndex<dashboard.provs.length;cspIndex++){
                var cspkey = dashboard.provs[cspIndex].csp;
                dashboard = reloadCSPConso($http,dashboard,cspkey,cspIndex);
            }
        };

        // Table filtering function
        dashboard.filter = function(row){
            dashboard.order= dashboard.order === row? '-'+row:row;
            dashboard.orderType = dashboard.order.charAt(0);
        };

        //Deleting function
        dashboard.deleteCSP = function(cspkey){
            var canDelete = confirm("Are you sure you want to delete this csp?");
            if(canDelete){
                $http.delete('./deleteCSP', { 'params' : {'cspkey':cspkey}})
                .success(function(){
                    $window.location.reload();
                });
            }
        };

        dashboard.showEdit = function(csp){
            console.log(csp);
            dashboard.cspToEdit = csp;
            dashboard.edit = true;
        };

        dashboard.hideEdit = function(){
            dashboard.cspToEdit = null;
            dashboard.edit = false;
        };

        dashboard.submitChanges = function(cspEdited){
            console.log(cspEdited);
            $http.get('./changeQuotas',
            {'params':{'cspKey':cspEdited.csp,
            'audioQuota':cspEdited.audioQuota,
            'videoQuota':cspEdited.videoQuota,
            'dataQuota':cspEdited.dataQuota}}).success(function(){
                $window.location.reload();
            });
        };
    }];

    // Creating a custom directive that allows to use the custom element <prov-form>
    // Note that camelCase is translated into camel-case for the html.
    app.directive('provForm',function(){
        return{
            restrict:'E',
            templateUrl:'provform.html'
        };
    });

    app.directive('provTable',function(){
        return{
            restrict:'E',
            templateUrl:'provtable.html',
            controller: mainController,
            controllerAs:'csplist'
        };
    });

    app.filter('bytes', function() {
    	return function(bytes, unit, precision) {
            if (bytes===0 ) return '0';
    		if ( isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
    		if (typeof precision === 'undefined') {
                    precision = 1;
                }
    		var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
    			number = Math.floor(Math.log(bytes) / Math.log(1024));
    		return (bytes / Math.pow(1024, units.indexOf(unit))).toFixed(precision);
    	}
    });

    //Directive used to convert string to numbers in <input type="number"
    //@See : https://docs.angularjs.org/error/ngModel/numfmt
    app.directive('stringToNumber', function() {
      var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'];
      return {
        require: 'ngModel',
        link: function(scope, element, attrs, ngModel) {
          ngModel.$parsers.push(function(value) {
            return '' + (value* Math.pow(1024, units.indexOf(scope.csplist.filteringUnit)));
          });
          ngModel.$formatters.push(function(value) {
            return value/ Math.pow(1024, units.indexOf(scope.csplist.filteringUnit))
          });
        }
      };
    });
})();
