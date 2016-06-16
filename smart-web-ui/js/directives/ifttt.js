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
                create: "=",
                mparam: "="

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
                    addOverlay(connectionInProgress.connection, scope.mthis, scope.mthat, scope.mparam);
                    scope.create = false;


                }
            });

            function addOverlay(conn, mthis,mthat,maction){
                if(conn == undefined){return};
                conn.addOverlay(
                    ["Label", {
                        label:mthis,
                        location:0.1,
                        cssClass: 'aLabel',
                        events:{
                            dblclick:function(labelOverlay, originalEvent) {
                                alert("Double on label overlay for :" + labelOverlay.component);
                            }
                        }
                    }]
                );
                conn.addOverlay(
                    ["Label", {
                        label:maction,
                        location:0.5,
                        cssClass: 'aLabel',
                        events:{
                            dblclick:function(labelOverlay, originalEvent) {
                                alert("Double on label overlay for :" + labelOverlay.component);
                            }
                        }
                    }]
                );
                conn.addOverlay(
                    ["Label", {
                        label:mthat,
                        location:0.9,
                        cssClass: 'aLabel',

                    }]
                );

            }



            scope.$watch('env', function(newValue, oldValue) {
                //alert("newData");
                jsP.empty("drawingArea");
                numberOfElements=0;
                if(newValue){
                    deviceList = [];
                    var tmpSensor;
                    newValue.zones.forEach(function(zone){
                        //console.log(zone.things);
                        if(zone.things){
                            zone.things.forEach(function(thing) {

                                if(thing._id != "5762774b28c5cad47d5ce51d") {
                                    addDevice(thing, zone, true);
                                }else{
                                    if (thing!=undefined){tmpSensor = {"thing":thing,"zone":zone} ;}
                                }
                                deviceList.push(thing);
                            });
                        }

                    });
                    if(tmpSensor != undefined){addDevice(tmpSensor.thing, tmpSensor.zone, false);}


                    var conn = jsP.connect({source:"57626b6c28c5cad47d5ce51a", target:"57626c2528c5cad47d5ce51b"});
                    console.log(conn);
                    addOverlay(conn, "ON", "Make Coffee", "Ring");

                    var conn2 = jsP.connect({source:"57626b6c28c5cad47d5ce51a", target:"57626b6c28c5cad47d5ce51a"});
                    console.log(conn2);
                    addOverlay(conn2, "ON", "Alarm", "Ring");

                }

            });


                jsP.Defaults.Connector = ["StateMachine"],
                jsP.Defaults.Anchor = "TopCenter";
                jsP.Defaults.ConnectorStyle = {
                    lineWidth: 5,
                    strokeStyle: "#FFF"
                };
                jsP.Defaults.PaintStyle = { lineWidth : 5, strokeStyle : "#fff" },


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
                var cssTop;
                if(isVisible){
                    cssDisplay = "block";
                }else{
                    cssDisplay = "none";
                }

                if (numberOfElements%2 == 0){
                    cssTop = 50;
                }else{
                    cssTop = 250;
                }




                $('#'+htmlBase).append(
                    '<div class="window decision node" id="' + id + '" style="display:'+cssDisplay+
                '; top:'+cssTop+'px; left:'+(200)*numberOfElements+'px"><p style="text-align: center">'+device.name+'</p></div>'
                );

                $('#'+id).append('<p style="text-align: center">'+zone.name+'</p>');

                var numActions = 0;
                jsP.addEndpoint(
                    $('#'+id),
                    {
                        isSource: true,
                        isTarget: true,
                        maxConnections: 3,
                        //anchor:sourceAnchors
                        //anchor:[ 1, (0.38)+(0.2*numActions), 0, 1, "upper_dec_end endpoint" ],
                         paintStyle: { fillStyle: '#445' },
                         endpoint: ["Rectangle", {width:12, height:12}]
                    }
                );

               // device.actions.forEach(function(action){
                    //$('#'+id).append('<p style="text-align: center; background-color: #0b97c4; padding: 5px;">'+action+'</p>');


                    numActions++;





                jsP.draggable($('#' + id), {
                    containment:"parent"
                });
                numberOfElements++;
                return id;
            }


        }
    }
]);