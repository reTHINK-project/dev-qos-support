define({ "api": [
  {
    "type": "delete",
    "url": "/deleteCSP",
    "title": "",
    "name": "deleteCSP",
    "group": "Broker_Dashboard_REST_requests",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "cspKey",
            "description": "<p>csp key in the database.</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "int",
            "optional": false,
            "field": "return",
            "description": "<p>1</p>"
          }
        ]
      }
    },
    "version": "0.0.0",
    "filename": "app/routes.js",
    "groupTitle": "Broker_Dashboard_REST_requests"
  },
  {
    "type": "get",
    "url": "/getCspConso",
    "title": "",
    "name": "getCspConso",
    "group": "Broker_Dashboard_REST_requests",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "cspkey",
            "description": "<p>csp key in the database.</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "Array[]",
            "optional": false,
            "field": "CspInfosArray",
            "description": "<p>Array containing all csp registered in the base</p>"
          },
          {
            "group": "Success 200",
            "type": "Object[]",
            "optional": false,
            "field": "CspInfosArray.0",
            "description": "<p>array containing csp infos</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "CspInfosArray.0.audioQuota",
            "description": "<p>audio data quota.</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "CspInfosArray.0.videoQuota",
            "description": "<p>video data quota.</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "CspInfosArray.0.dataQuota",
            "description": "<p>datachannel quota.</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "CspInfosArray.0.csp",
            "description": "<p>csp name.</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success-Response:",
          "content": " HTTP/1.1 200 OK\n[\n  {\n  \"videoQuota\":\"12456231\",\n  \"audioQuota\":\"59122313\",\n  \"dataQuota\":\"05464314\",\n  \"csp\":\"prov:csp1\"\n  },\n  {\n  \"videoQuota\":\"8010000\",\n  \"audioQuota\":\"0\",\n  \"dataQuota\":\"54652231546\",\n  \"csp\":\"prov:csp2\"\n  }\n]",
          "type": "json"
        }
      ]
    },
    "version": "0.0.0",
    "filename": "app/routes.js",
    "groupTitle": "Broker_Dashboard_REST_requests"
  },
  {
    "type": "get",
    "url": "/getCspConso",
    "title": "",
    "name": "getCspConso",
    "group": "Broker_Dashboard_REST_requests",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "cspkey",
            "description": "<p>csp key in the database.</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "Object[]",
            "optional": false,
            "field": "consumptions",
            "description": "<p>Object containing data consumptions for the csp</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "consumptions.video",
            "description": "<p>video data consumption</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "credentials.audio",
            "description": "<p>audio data consumption.</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "credentials.data",
            "description": "<p>datachannel consumption.</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success-Response:",
          "content": " HTTP/1.1 200 OK\n{\n  \"video\":\"12\",\n  \"audio\":\"59122313\",\n  \"data\":\"0\"\n}",
          "type": "json"
        }
      ]
    },
    "version": "0.0.0",
    "filename": "app/routes.js",
    "groupTitle": "Broker_Dashboard_REST_requests"
  },
  {
    "type": "post",
    "url": "/provRequest",
    "title": "",
    "name": "provRequest",
    "group": "Broker_Dashboard_REST_requests",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "servicename",
            "description": "<p>name of the csp.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "audio",
            "description": "<p>audio quota.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "video",
            "description": "<p>video quota.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "qdata",
            "description": "<p>data quota.</p>"
          }
        ]
      }
    },
    "version": "0.0.0",
    "filename": "app/routes.js",
    "groupTitle": "Broker_Dashboard_REST_requests"
  },
  {
    "type": "get",
    "url": "/getAppropriateTurn",
    "title": "",
    "name": "getAppropriateTurn",
    "group": "Broker_REST_requests",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "cspId",
            "description": "<p>id of the csp.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "clientName",
            "description": "<p>clientName.</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "Object[]",
            "optional": false,
            "field": "response",
            "description": "<p>Array with turnurl and generated clientId.</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "response[0]",
            "description": "<p>turnurl</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "response[1]",
            "description": "<p>generated clientId.</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success-Response:",
          "content": " HTTP/1.1 200 OK\n[\"172.16.52.90:4078\",\"49e4f103f82f151fd5bc8272c5fb3cac\"]",
          "type": "json"
        }
      ]
    },
    "version": "0.0.0",
    "filename": "app/routes.js",
    "groupTitle": "Broker_REST_requests"
  },
  {
    "type": "get",
    "url": "/getCredentials",
    "title": "",
    "name": "getCredentials",
    "group": "Broker_REST_requests",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "clientId",
            "description": "<p>clientId.</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "Object[]",
            "optional": false,
            "field": "credentials",
            "description": "<p>Object containing user and password for the turn</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "credentials.clientId",
            "description": "<p>user for the turn. Note that it's the same clientId as params</p>"
          },
          {
            "group": "Success 200",
            "type": "password",
            "optional": false,
            "field": "credentials.password",
            "description": "<p>password for the turn.</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success-Response:",
          "content": " HTTP/1.1 200 OK\n{\n  \"clientId\":\"8922cee50a54f7c8c9d15d5171622358\",\n  \"password\":\"jSXdh\"\n}",
          "type": "json"
        }
      ]
    },
    "version": "0.0.0",
    "filename": "app/routes.js",
    "groupTitle": "Broker_REST_requests"
  }
] });
