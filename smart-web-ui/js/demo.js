var htmlBase = 'drawingArea';
var numberOfElements = 0;
var exampleObj = {
    "devices":[
        {
            "name":"kettle",
            "id":"kettle",
            "location":"kitching",
            "actions":[
                "Turn On","Turn Off"
            ],
            "triggers":[
                "Switch"
            ],
            "data":{
                "temperture":70,
                "state": "ON"
            }
        },
        {
            "name":"TV",
            "id":"tv",
            "location":"living room",
            "actions":[
                "Turn On","Turn Off","Change Channel"
            ],
            "triggers":[
                "Switch",
                "EndofProgram"
            ],
            "data":{
                "temperture":70,
                "state": "ON"
            }
        },
        {
            "name":"Lights",
            "id":"lights",
            "location":"living room",
            "actions":[
                "Turn On","TurnOFF"
            ],
            "triggers":[
                "Switch",
                "EndofProgram"
            ],
            "data":{
                "temperture":70,
                "state": "ON"
            }
        },
        {
            "name":"Front Door",
            "id":"frntdoor",
            "location":"lobby",
            "actions":[
                "Turn On","TurnOFF"
            ],
            "triggers":[
                "Switch",
                "EndofProgram"
            ],
            "data":{
                "temperture":70,
                "state": "ON"
            }
        }
    ]
}

jsPlumb.ready(function() {
    var jsP = jsPlumb.getInstance();
    exampleObj.devices.forEach(function(device){
        console.log(device);
        addDevice(device);
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
                label: "my label2",
                location:0.5,
                cssClass: 'aLabel',
                events:{
                    /*click:function(labelOverlay, originalEvent) {
                        alert("click on label overlay for :" + labelOverlay.component);
                    },*/
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





    function addDevice(device){
        var id = device.id;
        if(typeof id === "undefined"){
            numberOfElements++;
            id = "decisioncontainer" + numberOfElements;
        }

        $('#'+htmlBase).append(
            '<div class="window decision node" id="' + id + '"><p style="text-align: center">'+device.name+'</p></div>'
        );

        $('#'+id).append('<p style="text-align: center">'+device.location+'</p>');
        var sourceAnchors = [
            [ 0, 1, 0, 1, "upper_dec_end endpoint" ],
            [ 0.25, 1, 0, 1,"upper_dec_end endpoint" ],
            [ 0.5, 1, 0, 1,"upper_dec_end endpoint" ],
            [ 0.75, 1, 0, 1,"upper_dec_end endpoint" ],
            [ 1, 1, 0, 1,"upper_dec_end endpoint" ]
        ];

        sourceAnchors.forEach(function(anch){
            jsP.addEndpoint(
                $('#'+id),
                {
                    isSource: true,
                    isTarget: true,
                    maxConnections: 1,
                    anchor:anch,
                    paintStyle: { fillStyle: 'red' },
                    endpoint: ["Rectangle", {width:12, height:12}]
                }
            );
        });
        jsP.draggable($('#' + id), {
            containment:"parent"
        });
        return id;
    }




});