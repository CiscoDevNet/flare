angular.module('appControllers').directive("ifttt",["$animate",
    function( $animate, session_svc ) {
        return({
            link: link,
            templateUrl:'partials/ifttt.html',
            restrict: 'E',
            replace: false,
            transclude: true,
            scope: {
                env: "=",
                mthis: "=",
                mthat: "=",
                cancel: "=",
                create: "="

            }
        });
        function link( scope, element, attributes ) {
            var htmlBase = 'drawingArea';
            var numberOfElements = 0;
            var jsP = jsPlumb.getInstance();
            var deviceList = [];
            var connectionInProgress;

            scope.$watch('mthis', function(newValue, oldValue) {
                //alert("mthis fired"+scope.mthis);
            });

            scope.$watch('cancel', function(newValue, oldValue) {
                if(scope.cancel==true){

                    jsP.detach(connectionInProgress);
                    scope.cancel = false;
                }
            });

            scope.$watch('create', function(newValue, oldValue) {
                if(scope.create==true){

                   // jsP.detach(connectionInProgress);
                    scope.create = false;
                    connectionInProgress.connection.addOverlay(
                        ["Label", {
                            label:scope.mthis,
                            location:0.1,
                            cssClass: 'aLabel',
                            events:{
                                dblclick:function(labelOverlay, originalEvent) {
                                    alert("Double on label overlay for :" + labelOverlay.component);
                                }
                            }
                        }]
                    );
                    connectionInProgress.connection.addOverlay(
                        ["Label", {
                            label:"test",
                            location:0.5,
                            cssClass: 'aLabel',
                            events:{
                                dblclick:function(labelOverlay, originalEvent) {
                                    alert("Double on label overlay for :" + labelOverlay.component);
                                }
                            }
                        }]
                    );
                    connectionInProgress.connection.addOverlay(
                        ["Label", {
                            label:scope.mthat,
                            location:0.9,
                            cssClass: 'aLabel',

                        }]
                    );

                }
            });

            scope.$watch('env', function(newValue, oldValue) {
                //alert("newData");
                jsP.empty("drawingArea");
                numberOfElements=0;
                if(newValue){
                    deviceList = [];
                    newValue.zones.forEach(function(zone){
                        //console.log(zone.things);
                        if(zone.things){
                            zone.things.forEach(function(thing) {

                                if(thing._id != "5762774b28c5cad47d5ce51d") {
                                    addDevice(thing, zone, true);

                                }else{
                                    deviceList.push({"thing":thing,"zone":zone});
                                }
                            });
                        }

                    });
                    deviceList.forEach(function(dev){
                        addDevice(dev.thing, dev.zone, false);
                    })

                }

            });


            jsP.Defaults.Connector = ["Straight"],
                jsP.Defaults.ConnectorStyle = {
                    lineWidth: 3,
                    strokeStyle: "#5b9ada"
                },


                jsP.setContainer($('#'+htmlBase));
            jsP.bind("beforeDrop", function(connection) {

                $('#this').empty()
                console.log(connection);
                var source = $.grep(deviceList, function(ele){ return ele._id == connection.sourceId; })[0];
                var target = $.grep(deviceList, function(ele){ return ele._id == connection.targetId; })[0];
                console.log("RESULT",source,target);
                scope.mthis = Object.keys(source.data.accessControl)[0];
                Object.keys(source.data.accessControl).forEach(function(val){
                    $('#this')
                        .append($("<option></option>")
                            .attr("value",val)
                            .text(val));
                });

                $('#that').empty()
                scope.mthat = target.actions[0];
                target.actions.forEach(function(val){
                    $('#that')
                        .append($("<option></option>")
                            .attr("value",val)
                            .text(val));
                });

                $("#myModal").modal();

                return true;
            });
            jsP.bind("connection", function(e) {
                var conn= Object.assign({}, e);
                connectionInProgress = e;;
            });

            function addDevice(device, zone, isVisible){
                var id = device["_id"];//.replace(/\W/g,'_');
                var cssDisplay;
                if(isVisible){
                    cssDisplay = "block";
                }else{
                    cssDisplay = "none";
                }

                $('#'+htmlBase).append(
                    '<div class="window decision node" id="' + id + '" style="display:'+cssDisplay+
                '; top:10px; left:'+(200)*numberOfElements+'px"><p style="text-align: center">'+device.name+'</p></div>'
                );

                $('#'+id).append('<p style="text-align: center">'+zone.name+'</p>');

                var numActions = 0;

                var sourceAnchors = [
                    [ 0, 0, 0, 1 ],
                    [ 0.25, 1, 0, 1 ],
                    [ 0.5, 1, 0, 1 ],
                    [ 0.75, 1, 0, 1 ],
                    [ 1, 1, 0, 1 ],
                    [ 0, 0, 0, 1 ],
                    [ 0, 0.25, 0, 1 ],
                    [ 0, 0.5, 0, 1 ],
                    [ 0, 0.75, 0, 1 ],
                    [ 1, 1, 0, 1 ],
                    [ 0, 1, 0, 1 ],
                    [ 0.25, 0, 0, 1 ],
                    [ 0.5, 0, 0, 1 ],
                    [ 0.75, 0, 0, 1 ],
                    [ 1, 0, 0, 1 ],
                    [ 1, 0, 0, 1 ],
                    [ 1, 0.25, 0, 1 ],
                    [ 1, 0.5, 0, 1 ],
                    [ 1, 0.75, 0, 1 ],
                    [ 0, 0, 0, 0 ],
                ];

               // device.actions.forEach(function(action){
                    //$('#'+id).append('<p style="text-align: center; background-color: #0b97c4; padding: 5px;">'+action+'</p>');
                    jsP.addEndpoint(
                        $('#'+id),
                        {
                            isSource: true,
                            isTarget: true,
                            maxConnections: 3,
                            anchor:sourceAnchors
                            //anchor:[ 1, (0.38)+(0.2*numActions), 0, 1, "upper_dec_end endpoint" ],
                           // paintStyle: { fillStyle: 'red' },
                            //endpoint: ["Rectangle", {width:12, height:12}]
                        }
                    );

                    numActions++;




                sourceAnchors.forEach(function(anch){

                });
                jsP.draggable($('#' + id), {
                    containment:"parent"
                });
                numberOfElements++;
                return id;
            }


        }
    }
]);