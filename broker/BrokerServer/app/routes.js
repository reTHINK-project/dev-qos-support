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
 * @brief routes for express module.
 *
 * @file
 *
 */
var path = require('path');
module.exports = function(app, auth, dao) {

    app.get('/dashboard', auth, function(req, res){
        res.sendFile('dashboard.html',{ root: path.join(__dirname, '../public') });
    });

    app.get('/apidoc', auth, function(req, res){
        res.sendFile('index.html',{ root: path.join(__dirname, '../public/apidoc/') });
    });

    /**
     * @api {post} /provRequest
     * @apiName provRequest
     * @apiGroup Broker Dashboard REST requests
     *
     * @apiParam {String} servicename name of the csp.
     * @apiParam {String} audio audio quota.
     * @apiParam {String} video video quota.
     * @apiParam {String} qdata data quota.
     */
    app.post('/provRequest', function(req, res){
        console.log(JSON.stringify(req.body));
        var sendBackInfos = dao.registerCSP(req.body,3200);
        res.redirect('/dashboard.html');
    });

    /**
     * @api {get} /getAppropriateTurn
     * @apiName getAppropriateTurn
     * @apiGroup Broker REST requests
     *
     * @apiParam {String} cspId id of the csp.
     * @apiParam {String} clientName clientName.
     *
     * @apiSuccess {Object[]} response Array with turnurl and generated clientId.
     * @apiSuccess {String} response[0] turnurl
     * @apiSuccess {String} response[1] generated clientId.
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *    ["172.16.52.90:4078","49e4f103f82f151fd5bc8272c5fb3cac"]
     */
    app.get('/getAppropriateTurn', function(req, res){
        var clientId = req.query.cspId;
        var clientName = req.query.clientName;
        dao.getAppropriateTurn(clientId, clientName)
        .then(function(reply){
            res.json(reply);
            console.log('Returned Appropriate Turn and ClientId : ' + reply);
        })
        .catch(function(error){ 
            res.sendStatus(404);
        });
    });

    /**
     * @api {get} /getCspConso
     * @apiName getCspConso
     * @apiGroup Broker Dashboard REST requests
     *
     * @apiParam {String} cspkey csp key in the database.
     *
     * @apiSuccess {Array[]} CspInfosArray Array containing all csp registered in the base
     * @apiSuccess {Object[]} CspInfosArray.0 array containing csp infos
     * @apiSuccess {String} CspInfosArray.0.audioQuota audio data quota.
     * @apiSuccess {String} CspInfosArray.0.videoQuota video data quota.
     * @apiSuccess {String} CspInfosArray.0.dataQuota datachannel quota.
     * @apiSuccess {String} CspInfosArray.0.csp csp name.
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *    [
     *      {
     *      "videoQuota":"12456231",
     *      "audioQuota":"59122313",
     *      "dataQuota":"05464314",
     *      "csp":"prov:csp1"
     *      },
     *      {
     *      "videoQuota":"8010000",
     *      "audioQuota":"0",
     *      "dataQuota":"54652231546",
     *      "csp":"prov:csp2"
     *      }
     *    ]
     */
    app.get('/getAllCspInfo', function(req, res){
        dao.getAllCspInfo()
        .then(function(result){
                res.status(200).json(result);
            },
            function(err){
                res.sendStatus(500);
            }
        );
    });


    /**
     * @api {get} /getCspConso
     * @apiName getCspConso
     * @apiGroup Broker Dashboard REST requests
     *
     * @apiParam {String} cspkey csp key in the database.
     *
     * @apiSuccess {Object[]} consumptions Object containing data consumptions for the csp
     * @apiSuccess {String} consumptions.video video data consumption
     * @apiSuccess {String} credentials.audio audio data consumption.
     * @apiSuccess {String} credentials.data datachannel consumption.
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *    {
     *      "video":"12",
     *      "audio":"59122313",
     *      "data":"0"
     *    }
     */
    app.get('/getCspConso', function(req, res){
        var cspkey = req.query.cspkey;
        dao.getCSPConso(cspkey)
        .then(function(reply){
            res.json(reply);
        });
    });

    /**
     * @api {get} /getCredentials
     * @apiName getCredentials
     * @apiGroup Broker REST requests
     *
     * @apiParam {String} clientId clientId.
     *
     * @apiSuccess {Object[]} credentials Object containing user and password for the turn
     * @apiSuccess {String} credentials.clientId user for the turn. Note that it's the same clientId as params
     * @apiSuccess {password} credentials.password password for the turn.
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *    {
     *      "clientId":"8922cee50a54f7c8c9d15d5171622358",
     *      "password":"jSXdh"
     *    }
     */
    app.get('/getCredentials', function(req, res){
        var clientId = req.query.clientId;
        dao.getCredentials(clientId)
        .then(function(credentials){
            res.json(credentials);
        });
    });

    /**
     * @api {delete} /deleteCSP
     * @apiName deleteCSP
     * @apiGroup Broker Dashboard REST requests
     *
     * @apiParam {String} cspKey csp key in the database.
     *
     * @apiSuccess {int} return 1
     */
    app.delete('/deleteCSP', function(req,res){
        var cspkey = req.query.cspkey;
        if (cspkey !== undefined){
            dao.deleteCSP(cspkey)
            .then(function(reply){
                res.sendStatus(200);
            });
        }
    });

    /**
     * @api {put} /changeQuotas
     * @apiName changeQuotas
     * @apiGroup Broker Dashboard REST requests
     *
     * @apiParam {String} cspKey csp key in the database.
     * @apiParam {String} audioQuota new audioQuota value.
     * @apiParam {String} videoQuota new videoQuota value.
     * @apiParam {String} dataQuota new dataQuota value.
     *
     * @apiSuccess {int} return 1
     */
     app.get('/changeQuotas',function(req,res){
         console.log(req.query);
         var cspkey = req.query.cspKey;
         var audioQuota = req.query.audioQuota;
         var videoQuota = req.query.videoQuota;
         var dataQuota = req.query.dataQuota;
         dao.updateCSPQuotas(cspkey,audioQuota,videoQuota,dataQuota)
         .then(function(reply){
             res.sendStatus(200);
         });
     });
};
