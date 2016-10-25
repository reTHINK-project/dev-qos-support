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
 * @brief Data Access Object draw a database abstract layer.
 *
 * @file
 *
 */
var REALM = "orange.com";
var TTLUSER = 604800; // == 1 week
var redis = require('redis');
var Promise = require('bluebird');
Promise.promisifyAll(redis.RedisClient.prototype);
var MD5 = require('crypto-js/md5');

function randomString(length) {
    return Math.round((Math.pow(36, length + 1) - Math.random() * Math.pow(36, length))).toString(36).slice(1);
}

/**
 * Log function for better clarity in node logs
 * @param {string} message - log message to display
 */
function daoLog(message){
    console.log('#### DAO Log : ' + message + ' #####');
}

/**
 * Redis DAO contructor
 @constructor
 @param {string} port - Port of the redis DB
 @param {string} host - Ip adress/host of the redis DB
 */
function redisDAO(port, host){
    // Redis Client are locked to subscribtion mode when activated, so we need two clients
    var dao = this;
    this.subscribers = {};
    this.subscribeClient = redis.createClient(port,host);
    this.subscribeClient.on('connect', function() {
        daoLog('DAO subscribeClient connected to redis');
    });

    this.subscribeClient.on('error',function(error){
        daoLog(error);
    });

    this.client = redis.createClient(port,host);
    this.client.on('connect', function() {
        daoLog('DAO connected to redis');
    });

    this.client.on('error',function(error){
        daoLog(error);
    });

    this.client.on('reconnecting', function(reconnect) {
        daoLog('DAO trying to reconnect to redis : attempt #' + reconnect.attempt + " delay : " + reconnect.delay );
    });

    /**
     * Event handler called on pattern publish
     * @See psubscribe
     */
    this.subscribeClient.on('psubscribe', function (channel, count) {
        console.log('Subscribed to ' + channel + ' (' + count + ')');
    });

    /**
     * Event handler called on subscribe
     * @See psubscribe
     */
    this.subscribeClient.on('subscribe', function (channel, count) {
        console.log('Subscribed to ' + channel + ' (' + count + ')');
    });

    /**
     * Event handler called on pattern publish
     * @See psubscribe
     */
    this.subscribeClient.on('pmessage', function(channel, message) {
        console.log(message);
        // message describes the matched subscription
        // Return immediately if the subscription has already been set
        if( this.subscribers[message] != undefined )
            return ;
        console.log(message);
        // Subscribe to the exact channel (no pattern) to retrieve the traffic informations
        this.subscribeClient.subscribe(message);
        // Save this state to avoid several same subscription
        // '1' is arbitrary
        this.subscribers[message] = 1 ;
    }.bind(this));

    /**
     * Event handler called on publish (no pattern)
     * @See subscribe
     */
    this.subscribeClient.on('message', function(channel, message) {
        // console.log(channel + ": " + message);
        var trafficParsed = dao.parseTurnTraffic(channel, message) ;
        dao.addCSPConso(trafficParsed);
    });


    /**
     * Parse the Turn traffic published
     * @param channel : The channel onto Turn published the traffic
     * @param message : The data published
     * @See subscribe
     */
    this.parseTurnTraffic = function(channel, message) {
        // Define the regex pattern to retrieve the user name and the session id
        var pattern = /turn\/.*?\/.*?\/realm\/.*?\/user\/(.*?)\/allocation\/([0-9]*)\//;

        // Apply the pattern and let's see the result
        var match = channel.match(pattern);
        if( !match ) {
            console.log('No match found into published traffic channel');
            return;
        }
        var user = match[1], session_id = match[2] ;

        // Now retrieve the traffic data
        pattern = /rcvp=[0-9]*, rcvb=([0-9]*), sentp=[0-9]*, sentb=([0-9]*)/;

        // Apply the pattern and let's see the result
        var match = message.match(pattern);
        if( !match ) {
            console.log('No match found into published traffic message... Abnormal');
            return;
        }

        var received_bytes = match[1], sent_bytes = match[2] ;
        console.log('user=' + user + ', session_id=' + session_id + ', received_bytes=' + received_bytes + ', sent_bytes=' +  sent_bytes) ;
        return {
            'user' : user,
            'sent_bytes':sent_bytes
        };
    };
}

/**
 * Returns the approriate turn server for the csp provided
 * @callback callback - Callback function to deal with the result (queries are async)
 * @param {string} csp - ClientId of the csp
 */
redisDAO.prototype.getAppropriateTurn = function(cspName, clientname){
    console.info("#### getAppropriateTurn, cspName=" + cspName + " client name="+clientname);

    var dao = this;

    var clientId = MD5(cspName+':'+clientname + new Date().toISOString()).toString();
    console.log("clientId generated : " + clientId);
    dao.registerUser(cspName,clientId);

    return new Promise(function(resolve, reject){

        dao.getCspInfoAndConso(cspName).then(function(result) {
            console.log("CSP=" + result.cspName + " dataQuota="+result.dataQuota + " dataConso="+result.dataConso);

            if (Number(result.dataConso) > Number(result.dataQuota)) {
                console.info("CSP:"+cspName+" data consumed exceeds provisioned quota");
                throw("data consumption exceeded");
            } else {
                // data consumption doesn't exceed provisioned quota
                return;
            }
        }).then(function() {

	    dao.getTurnServers().then(function(res,err) {
                if(err){
                    console.log("Unable to find a Turn server : " + err);
                    reject(err);
                } else {
                    var availableTurnServers = res;
                    //Filtering the key "turnservers:"
                    for (var turnServersIndex =0; turnServersIndex <availableTurnServers.length; turnServersIndex++){
                        availableTurnServers[turnServersIndex] = availableTurnServers[turnServersIndex].turnUrl.replace("turnservers:","");
                    }
                    var randomIndex = Math.floor((Math.random() * availableTurnServers.length));
                    resolve([availableTurnServers[randomIndex],clientId]);
                }
            })
            .catch(function(err){
                console.log("Error when retrieving turnservers, error : " + err);
                reject(err);
            });

        }).catch(function(err){
            console.log("Unable to get a turn server : " + err);
            reject("Unable to get a turn server : " + err);
        });
    });
};

/**
 * Returns credentials for a certain Turn server and an appID
 *
 * @param {string} clientId - clientId given to the client in the previous requests :/
 */
redisDAO.prototype.getCredentials = function(clientId){
    var dao = this;
    return new Promise(function(resolve, reject){
        dao.createTurnCredentials(clientId)
        .then(function(password){
            resolve({'clientId':clientId,'password':password});
        });
    });
};

/**
 * Adds a CSP to the redis database using datas from the provisiong page
 * @param {string} formDatas - Datas from the provisioning page (servicename,audio,video,lowspeeddata,highspeeddata)
 * @param {int} [TTL] - Time To Live of the database entry
 *
 * @return {object{}} {"clientId":clientId,"TTL":TTL}
 */
redisDAO.prototype.registerCSP = function(formDatas, TTL){
    console.info("#### registerCSP, csp=" + formDatas.servicename);

    var clientIdHash = formDatas.servicename;
    var redisKey = 'prov:'+clientIdHash;
    var dao = this;
    var asGB = 1024*1024*1024;

    var audioQuota = 0;
    var videoQuota = 0;

    if ((formDatas.audio !== undefined) && (formDatas.audio !== null)) {
        audioQuota = parseInt(formDatas.audio);
        daoLog("#### audio quota is not null : " + audioQuota);
    }

    if ((formDatas.video !== undefined) && (formDatas.video !== null)) {
        videoQuota = parseInt(formDatas.video);
        daoLog("#### video quota is not null : " + videoQuota);
    }

    //Switching to DB 2
    daoLog("Calling HMSET with key : " + redisKey);
    dao.client.selectAsync(2)
    .then(function(){
        dao.client.HSETNX(redisKey,'audioQuota',audioQuota*asGB,function(){});
        dao.client.HSETNX(redisKey,'videoQuota',videoQuota*asGB,function(){});
        dao.client.HSETNX(redisKey,'dataQuota',parseInt(formDatas.qdata)*asGB,function(){});
        dao.client.HSETNX(redisKey, 'conso', redisKey+":conso",function(){});
        dao.client.HSETNX(redisKey+":conso", 'audio', 0,function(){});
        dao.client.HSETNX(redisKey+":conso", 'video', 0,function(){});
        dao.client.HSETNX(redisKey+":conso", 'data', 0,function(){});
    });
    return {
        "clientId":clientIdHash,
        "TTL":TTL
    };
};

/**
 * Creates a table that links a csp to a user
 *
 * @param {string} csp csp name in the database (without prov:)
 * @param {string} user user to match with the csp
 */
redisDAO.prototype.registerUser = function(csp, user){
    console.info("#### registerUser, csp name=" + csp + " user="+user);

    this.client.select(2, function(){});
    var redisKey = 'users:'+ user;
    this.client.set(redisKey,csp,function(){});
    this.client.expire(redisKey,TTLUSER);
};


/**
 * Shortcut for redis psubscribe
 *
 * @param {string} pattern pattern for the subscribtion
 */
redisDAO.prototype.psubscribe = function(pattern){
    this.subscribeClient.psubscribe(pattern);
};

/**
 * get all csp(s) basic infos (quotas)
 *
 * @resolve {array} infos array containing an associative array with all values for each csp
 * @reject {string} err error during one of the requests
 */
redisDAO.prototype.getAllCspInfo = function(){
    //console.info("#### getAllCspInfo");

    var infos = [];
    var provKeys;
    var consoKey;
    var dataConso;
    var dao = this.client;
    return new Promise(function(resolve, reject){
        dao.select(2, function(){});
        dao.keys('prov:*',function(err,reply){
            // reply has the following format :
            // prov:csp1,prov:csp1:conso,prov:csp2:conso,prov:csp2

            provKeys = [];
            for( var replyindex = 0; replyindex < reply.length; replyindex ++){
                if(!reply[replyindex].match('.*:conso')){
                    //console.log("CSP found : " + reply[replyindex]);
                    provKeys.push(reply[replyindex]);
                }
            }
            for (var i=0; i< provKeys.length; i++){
                var provKey = provKeys[i];
                if(provKey.match('.*\:conso$') === null){
                    //DISCOLURE TO BE ABLE TO USE PROVKEYS INSIDE THE callback
                    (function(provKeys,client){
                            client.hgetall(provKey, function(err,reply){
                                if(err){
                                    reject(err);
                                }
                                var info = reply;
                                info.csp = this.args[0];
                                //console.log("CSP="+info.csp+ " dataQuota="+info.dataQuota+ " conso=" + info.conso);

                                infos.push(info);

                                if(Object.keys(infos).length === provKeys.length){
                                    resolve(infos);
                                }
                        });
                    })(provKeys,dao);
                }
            }
        });
    });
};


/**
 * get basic infos (quotas) and  consumption value for a given cspkey
 *
 * @param {string} cspkey key of the csp in the database
 *
 * @return {object} reply associative array containing audio, video and data quotas and audio, video and data consumptions
 * @reject {string} err error during one of the requests
 */
redisDAO.prototype.getCspInfoAndConso = function(cspName) {
    //console.info("#### getCspInfoAndConso, cspName="+cspName);

    var dao = this;
    var cspInfos;

    return new Promise(function(resolve, reject){

        var cspKey = "prov:"+cspName;
        console.log("cspKey="+cspKey);

        dao.getCSPInfos(cspKey).then(function(reply) {
            cspInfos = reply;
            cspInfos.cspName = cspName;

            dao.getCSPConso(cspKey).then(function(consoResult) {
                cspInfos.dataConso = consoResult.data;
                resolve(cspInfos);

            }).catch(function(err) {
                console.log("consumption not found for CSP ["+cspName+"]");
                reject("CSP consumption unknown");
            });

        }).catch(function(err) {
            console.log("CSP ["+cspName+"] not found");
            reject("CSP not found");
        });
    });
};



/**
 * Creates the credentials for turn authentification (clientId:passwordgenerated)
 * @param {string} clientId clientId that will be used as login for the turn
 *
 * @return {string} password CLEAR generated password
 */
redisDAO.prototype.createTurnCredentials = function(clientId){
    var dao = this;
    var password = randomString(6);
    return new Promise(function(resolve, reject){
        dao.client.selectAsync(0)
        .then(function(){
            dao.client.set('turn/realm/'+REALM+'/user/'+clientId+'/key',MD5(clientId+':'+REALM+':'+password),function(err,reply){
                if(err){
                    reject(err);
                }else{
                    resolve(password);
                }
            });
        });
    });
};

/**
 * Gets the informations associated to a given cspkey
 * @param {string} cspkey key of the csp in the database
 *
 * @return {object} reply associative array containing audio, video and data quotas
 */
redisDAO.prototype.getCSPInfos = function(cspkey){
    console.info("#### getCSPInfos, cspkey="+cspkey);

    // Requests to prov:csp
    var dao = this.client;
    return new Promise(function(resolve, reject){
        dao.selectAsync(2)
        .then(function(){
            dao.hgetall(cspkey,function(err,reply){
                if (err){
                    reject(err);
                } else {
                    resolve(reply);
                }
            });
        });
    });
};


/**
 * Gets the consumption value for a given cspkey
 * @param {string} cspkey key of the csp in the database
 *
 * @return {object} reply associative array containing audio, video and data consumptions
 */
redisDAO.prototype.getCSPConso = function(cspConsoKey){
    //console.info("#### getCSPConso, cspConsoKey="+cspConsoKey);

    // Requests to prov:csp:conso
    var dao = this.client;
    return new Promise(function(resolve, reject){
        dao.selectAsync(2)
        .then(function(){
            dao.hgetall(cspConsoKey+":conso",function(err,reply){
                if (err){
                    reject(err);
                } else {
                    resolve(reply);
                }
            });
        });
    });
};


/**
 * Gets the list of all turn servers online
 *
 * @return {array} reply list of all the turnservers api (on future impl, may add additional values like charge)
 */
redisDAO.prototype.getTurnServers = function(){
    //console.info("#### getTurnServers");

    var dao = this.client;
    return new Promise(function(resolve,reject){
        dao.selectAsync(2)
        .then(function(){
            dao.keys("turnservers:*",function(err,reply){
                if(err){
                    reject(err);
                }
                else{
                    var turnList = [];
                    if (reply.length === 0) {
                        console.log('No turn found');
                        reject('No turn found');
                    }
                    for (var turnIndex = 0; turnIndex<reply.length; turnIndex++){
                        (function(turnList,turnListSize,turnName){
                            dao.get(turnName,function(err,reply){

                                if(err){
                                    reject(err);
                                }
                                else {
                                    turnList.push({'turnUrl':turnName,'turnStatus':reply});
                                    if (turnList.length === turnListSize) {
                                        resolve(turnList);
                                    }
                                }
                            });
                        })(turnList,reply.length,reply[turnIndex]);
                    }
                }
            });
        });
    });
};

/**
 * Deletes a csp from the provisioning database (actually, just renamed so we can keep track of old consumptions)
 * @param {string} cspkey - csp key in the database
 *
 * @return {int} 1 - ...
 */
redisDAO.prototype.deleteCSP = function(cspkey){
    var dao = this.client;
    return new Promise(function(resolve, reject){
        dao.select(2,function(){});
        dao.rename(cspkey+":conso",'histo'+cspkey+":conso");
        dao.rename(cspkey,'histo'+cspkey, function(err,reply){
            if(err){
                reject(err);
            }
            else {
                resolve(1);
            }
        });
    });
};

/**
 * Updates a given CSP Quotas
 * @param {string} cspkey - csp key in the database
 * @param {string} audioQuota - new audioQuota
 * @param {string} videoQuota - new videoQuota
 * @param {string} dataQuota - new dataQuota
 *
 * @return {int} 1 - ...
 */
redisDAO.prototype.updateCSPQuotas = function(cspkey,audioQuota,videoQuota,dataQuota){
    var dao = this.client;

    return new Promise(function(resolve, reject){
        dao.selectAsync(2)
        .then(function(){
            dao.hmset(cspkey,{
                'audioQuota':audioQuota,
                'videoQuota':videoQuota,
                'dataQuota':dataQuota
            },function(err,reply){
                if (err){
                    reject(err);
                } else {
                    resolve(reply);
                }
            });
        });
    });
};


/**
 * Increments the data consumption for a given CSP, no distinction yet
 * @param {string} parsedTraffic - traffic from the function parsedTraffic();
 *
 * @return {string} newConsummedValue - value of the csp consumption after increment
 */
redisDAO.prototype.addCSPConso = function(parsedTraffic){
    var dao = this.client;
    return new Promise(function(resolve, reject){
        dao.selectAsync(2)
        .then(function(){
            dao.getAsync('users:'+parsedTraffic.user)
            .then(function(res){
                dao.hincrby('prov:'+res+':conso', 'data', parsedTraffic.sent_bytes, function(err,reply){
                    if (err){
                        reject(err);
                    } else {
                        resolve(reply);
                    }
                });
            });
        });
    });
};
/**
 * Creates a DAO with whatever db technology you want. This object is what is exported
 * @constructor
 * @param {string} port - Port of the DB
 * @param {string} host - Ip adress/host of the DB
 */
function DAO(dbType,port,host){
    switch (dbType){
        case 'redis' :
            return new redisDAO(port,host);
    }
}

module.exports = DAO;
