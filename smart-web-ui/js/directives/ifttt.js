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
            }
        });
        function link( scope, element, attributes ) {
            var htmlBase = 'drawingArea';
            var numberOfElements = 0;
            var jsP = jsPlumb.getInstance();
            scope.$watch('env', function(newValue, oldValue) {
                //alert("newData");
                jsP.empty("drawingArea");
                numberOfElements=0;
                if(newValue){
                    newValue.zones.forEach(function(zone){
                        //console.log(zone.things);
                        if(zone.things){
                            zone.things.forEach(function(thing) {
                                addDevice(thing, zone);
                                console.log(thing);
                            });
                        }

                    });
                }

            });

            jsP.Defaults.Endpoint =  ["Dot", {
                radius: 4
            }];
            jsP.Defaults.EndpointStyle = {
                fillStyle: "#5b9ada"
            };

            jsP.Defaults.PaintStyle =  {
                strokeStyle: "#5b9ada",
                lineWidth: 3
            };
            jsP.Defaults.Connector = ["Straight"],
                jsP.Defaults.ConnectorStyle = {
                    lineWidth: 3,
                    strokeStyle: "#5b9ada"
                },


                jsP.setContainer($('#'+htmlBase));
            jsP.bind("connection", function(e) {
                var conn=e.connection;
                conn.endpoint =  ["Dot", {
                    radius: 4
                }];




                conn.addOverlay(
                    ["Label", {
                        label: prompt("What is the link", "Harry Potter"),
                        location:0.5,
                        cssClass: 'aLabel',
                        events:{

                            dblclick:function(labelOverlay, originalEvent) {
                                alert("Double on label overlay for :" + labelOverlay.component);
                            }
                        }

                    }]);
                conn.addOverlay(
                    ["Arrow", {
                        width: 10,
                        length: 10,
                        foldback: 1,
                        location: 1,
                        id: "arrow"
                    }]
                );

                conn.addOverlay([ "Dot", {
                    radius: 15,
                    paintStyle:{ strokeStyle:"blue", lineWidth:10, fillStyle:"red", },
                    events:{
                        dblclick:function(diamondOverlay, originalEvent) {
                            console.log("double click on diamond overlay for : " + diamondOverlay.component);
                        }
                    }
                }]);
            });





            function addDevice(device, zone){
                var id = device.id;
                if(typeof id === "undefined"){
                    numberOfElements++;
                    id = "decisioncontainer" + numberOfElements;
                }

                $('#'+htmlBase).append(
                    '<div class="window decision node" id="' + id + '" style="top:10px; left:'+(300)*numberOfElements+'px"><p style="text-align: center">'+device.name+'</p></div>'
                );

                $('#'+id).append('<p style="text-align: center">'+zone.name+'</p>');

                var numActions = 0;
                device.actions.forEach(function(action){
                    $('#'+id).append('<p style="text-align: center; background-color: #0b97c4; padding: 5px;">'+action+'</p>');
                    jsP.addEndpoint(
                        $('#'+id),
                        {
                            isSource: true,
                            isTarget: false,
                            maxConnections: 1,
                            anchor:[ 1, (0.38)+(0.2*numActions), 0, 1, "upper_dec_end endpoint" ],
                            paintStyle: { fillStyle: 'red' },
                            endpoint: ["Rectangle", {width:12, height:12}]
                        }
                    );
                    jsP.addEndpoint(
                        $('#'+id),
                        {
                            isSource: false,
                            isTarget: true,
                            maxConnections: 1,
                            anchor:[ 0, (0.38)+(0.2*numActions), 0, 1, "upper_dec_end endpoint" ],
                            paintStyle: { fillStyle: 'Blue' },
                            endpoint: ["Dot", {width:12, height:12}]
                        }
                    );
                    numActions++;
                });

                var sourceAnchors = [
                    ,
                    [ 0.25, 1, 0, 1,"upper_dec_end endpoint" ],
                    [ 0.5, 1, 0, 1,"upper_dec_end endpoint" ],
                    [ 0.75, 1, 0, 1,"upper_dec_end endpoint" ],
                    [ 1, 1, 0, 1,"upper_dec_end endpoint" ]
                ];

                sourceAnchors.forEach(function(anch){

                });
                jsP.draggable($('#' + id), {
                    containment:"parent"
                });
                return id;
            }


        }
    }
]);