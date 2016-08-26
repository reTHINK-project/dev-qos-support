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
 * @brief WebRTC example app
 *
 * @file
 *
 */

var BROKERURL = "https://10.193.5.91:8181";
//var BROKERURL = "https://localhost:8181";

// RTCSERVICEURL : IP Address (port) of the server that will host the webrtc app
var RTCSERVICEURL = "https://10.193.5.91:8080";
//var RTCSERVICEURL = "https://localhost:8080";

// CSPNAME : the one defined by admin in the dashboard
var CSPNAME = "reThinkTestCSP";
//var CSPNAME = "test2";
var CLIENTNAME = "RealTimeVideoCall";

var localStream = null;
var peerConn = null;
// var sourcevid = document.getElementById('local');
// var remotevid = document.getElementById('remote');
var isChannelReady;
var isInitiator = false;
var isStarted = false;
var localStream;
var pc;
var remoteStream;
var turnReady;
var peerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection ||
                       window.webkitRTCPeerConnection || window.msRTCPeerConnection;
var sessionDescription = window.RTCSessionDescription || window.mozRTCSessionDescription ||
                       window.webkitRTCSessionDescription || window.msRTCSessionDescription;
var RTCIceCandidate = window.RTCIceCandidate || window.mozRTCIceCandidate ||
                                              window.webkitRTCIceCandidate || window.msRTCIceCandidate;
navigator.getUserMedia = navigator.getUserMedia || navigator.mozGetUserMedia ||
                     navigator.webkitGetUserMedia || navigator.msGetUserMedia;
var pc_constraints = {
    'optional': [{
        'DtlsSrtpKeyAgreement': true
    }]
};

// Set up audio and video regardless of what devices are present.
var sdpConstraints = {
    'mandatory': {
        'OfferToReceiveAudio': true,
        'OfferToReceiveVideo': true
    }
};

// ///////////////////////////////////////////

var room = '';
if (room === '') {
    room = prompt('Enter room name:');
} else {
    //
}

// ROUTINES #############################################################

/**
 * Sends a message to the signaling server via socket.
 * @param {object} message - Message to transmit, could be whatever you want (int, object, string...)
 */
function sendMessage(message) {
    console.info('Client sending message: ', message);
    socket.emit('message', {
        'msg': message,
        'room': room
    });
}

/**
 * Success callback function for getUserMedia(), attaches the stream to the localVideo
 * @param {LocalMediaStream} stream - Local media stream object (video, audio...)
 */
function handleUserMedia(stream) {
    console.info('Adding local stream.');
    localVideo.src = window.URL.createObjectURL(stream);
    localStream = stream;
    sendMessage('got user media');
    if (isInitiator) {
        maybeStart();
    }
}

/**
 * Error callback function for getUserMedia()
 * @param {Error} error - Error from the getUserMedia function
 */
function handleUserMediaError(error) {
    console.info('getUserMedia error: ', error);
}

/**
 * Main logic function, decides whether to start or not
 * @param {object} RTCPeerConfiguration - Configuration object (see WebRTC doc) for the PeerConnection
 */
function maybeStart(RTCPeerConfiguration) {
    console.debug('isStarted : ' + isStarted);
    console.debug('localStream : ' + localStream);
    console.debug('isChannelReady : ' + isChannelReady);
    if (!isStarted && typeof localStream != 'undefined' && isChannelReady) {
        console.debug('Starting!');
        createPeerConnection(RTCPeerConfiguration);
        pc.addStream(localStream);
        isStarted = true;
        console.info('isInitiator', isInitiator);
        if (isInitiator) {
            doCall();
        }
    }
}

/**
 * Creates a new PeerConnection
 * @param {object} RTCPeerConfiguration - Configuration object (see WebRTC doc) for the PeerConnection
 */
function createPeerConnection(RTCPeerConfiguration) {
    try {
        pc = new peerConnection(RTCPeerConfiguration);
        pc.onicecandidate = handleIceCandidate;
        pc.onaddstream = handleRemoteStreamAdded;
        pc.onremovestream = handleRemoteStreamRemoved;
        console.info('Created RTCPeerConnnection');
    } catch (e) {
        console.info('Failed to create PeerConnection, exception: ' + e.message);
        alert('Cannot create RTCPeerConnection object.');
        return;
    }
}

/**
 * IceCandidate Handler (webRTC inner working). When a IceCandidate is "found", sends it using sendMessage
 * @param {event} event - Event of finding a candidate
 * @see sendMessage
 */
function handleIceCandidate(event) {
    console.info('handleIceCandidate event: ', event);
    if (event.candidate) {
        sendMessage({
            type: 'candidate',
            label: event.candidate.sdpMLineIndex,
            id: event.candidate.sdpMid,
            candidate: event.candidate.candidate
        });
    } else {
        console.info('End of candidates.');
    }
}

/**
 * Start the webRTC call. Consist of calling createOffer
 * @see webRTC do for RTCPeerConnection.createOffer()
 */
function doCall() {
    console.info('Sending offer to peer');
    pc.createOffer(setLocalAndSendMessage, handleCreateOfferError);
}

/**
 * Error handler when creating an offer
 * @param {event} event - Error event
 * @see doCall
 */
function handleCreateOfferError(event) {
    console.info('createOffer() error: ', e);
}

/**
 * Answering a WebRTC Offer. Consist of calling createAnswer
 * @see webRTC do for RTCPeerConnection.createAnswer()
 */
function doAnswer() {
    console.info('Sending answer to peer.');
    pc.createAnswer(setLocalAndSendMessage, createAnswerFailed, sdpConstraints);
}

/**
 * Error handler when creating an answer
 * @param {event} event - Error event
 * @see doAnswer
 */
function createAnswerFailed() {
    console.info("Create Answer failed");
}

/**
 * Sets local description (see webRTC doc.) and sends SessionDescription
 * @param {RTCSessionDescription} sessionDescription - RTCSessionDescription
 * @see WebRTC documentation
 */
function setLocalAndSendMessage(sessionDescription) {
    // Set Opus as the preferred codec in SDP if Opus is
    // present.
    pc.setLocalDescription(sessionDescription);
    console.info('setLocalAndSendMessage sending message', sessionDescription);
    sendMessage(sessionDescription);
}

/**
 * Handler triggered when we receive a remote video stream. Adds the stream to the remote video
 * @param {event} event - Event of receiving a stream
 */
function handleRemoteStreamAdded(event) {
    console.info('Remote stream added.');
    remoteVideo.src = window.URL.createObjectURL(event.stream);
    remoteStream = event.stream;
}

/**
 * Handler triggered when a remote video stream is removed. Removes the stream from the remote video
 * @param {event} event - Event of a removed stream
 */
function handleRemoteStreamRemoved(event) {
    console.info('Remote stream removed. Event: ', event);
}

/**
 * Hanging up the call. Sending a message bye to the server to notify other users
 * @see stop
 */
function hangup() {
    console.info('Hanging up.');
    stop();
    sendMessage('bye');
}

/**
 * Handling remote hangup, removing the remote video. Setting iniator to true because you're the one left on the room
 * @see stop
 */
function handleRemoteHangup() {
    console.info('Session terminated.');
    remoteVideo.src = null;
    stop();
    isInitiator = true;
}

/**
 * When connection is stopped, resetting some variables, closing PeerConnection
 */
function stop() {
    isStarted = false;
    pc.close();
    pc = null;
}

// END - ROUTINES #############################################################

var socket,localVideo,remoteVideo;

/**
 * Main behavior function, responsible of starting everything the page needs to return
 * @param {RTCConfiguration} RTCPeerConfiguration - Configuration object with IceServers, TransportPolicy
 */
function startupBehavior(RTCPeerConfiguration){
    socket = io.connect(RTCSERVICEURL);
    localVideo = document.querySelector('#local');
    remoteVideo = document.querySelector('#remote');

    var constraints = {
        video: 'true',
        audio: 'true'
    };
    console.info('Getting user media with constraints', constraints);
    navigator.mediaDevices.getUserMedia(constraints)
        .then(handleUserMedia)
        .catch(handleUserMediaError);
    // navigator.getUserMedia(constraints,handleUserMedia,handleUserMediaError);
    if (room !== '') {
        console.info('Create or join room', room);
        socket.emit('create or join', room);
    }

    socket.on('created', function(room) {
        console.info('Server : Created room ' + room);
        isInitiator = true;
    });

    socket.on('full', function(room) {
        console.info('Room ' + room + ' is full');
    });

    socket.on('join', function(room) {
        console.info('Another peer made a request to join room ' + room);
        console.info('You are the initiator of room ' + room + '!');
        isChannelReady = true;
    });

    socket.on('joined', function(room) {
        console.info('This peer has joined room ' + room);
        isChannelReady = true;
    });

    socket.on('log', function(array) {
        console.info.apply(console, array);
    });

    socket.on('message', function(message) {
        console.info('Client received message:', message);
        if (message === 'got user media') {
            maybeStart(RTCPeerConfiguration);
        } else if (message.type === 'offer') {
            if (!isInitiator && !isStarted) {
                maybeStart();
            }
            pc.setRemoteDescription(new sessionDescription(message));
            doAnswer();
        } else if (message.type === 'answer' && isStarted) {
            pc.setRemoteDescription(new sessionDescription(message));
        } else if (message.type === 'candidate' && isStarted) {
            var candidate = new RTCIceCandidate({
                sdpMLineIndex: message.label,
                candidate: message.candidate
            });
            pc.addIceCandidate(candidate);
        } else if (message === 'bye' && isStarted) {
            handleRemoteHangup();
        }
    });

    window.onbeforeunload = function(e){
        socket.emit('bye', room);
        sendMessage('bye');
    };
}

$(document).ready(
//    console.log("Request for a QoS Turn");
    $.get(BROKERURL+"/getAppropriateTurn",
        {
            cspId:CSPNAME,
            clientName:CLIENTNAME
        })
    .done(function(data, status){
        console.log(data);
        var turnServer = {
            urls: 'turn:'+data[0],
        };
        $.get(BROKERURL+"/getCredentials",{
            clientId : data[1]
        })
        .done(function(data,status){
            turnServer.username = data.clientId;
            turnServer.credential = data.password;
            console.log(turnServer);
            var RTCPeerConfiguration = {
                iceServers: [
                    turnServer
                ],
                iceTransportPolicy: "relay"
            };
            startupBehavior(RTCPeerConfiguration);
        });
    })
    .fail(function(err){
        console.error("Error during request for a QoS Turn : " + err.status + " " + err.responseText);

        //Only 404 supported now
        startupBehavior();
    })
);
