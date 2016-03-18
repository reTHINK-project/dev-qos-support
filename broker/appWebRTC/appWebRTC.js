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
 * @brief Entry point of the WebRTC example app
 *
 * @file
 *
 */

 var fs = require('fs'),
    express = require('express'),
    app = express(),
    server = require('https').createServer({
        key: fs.readFileSync('sslkeys/server-key.pem'),
        cert: fs.readFileSync('sslkeys/server-crt.pem'),
        ca: fs.readFileSync('sslkeys/ca-crt.pem'),
        },
        app),
    io = require('socket.io')(server);

server.listen(8080);
app.use(express.static(__dirname + '/public'));
app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  next();
});
app.get('/', function(req, res){
    res.sendFile(__dirname + '/public/index.html');
});

io.sockets.on('connection', function (socket) {
    console.log("connection");
    socket.on('message', function (message) {
        console.log(message);
        socket.broadcast.to(message['room']).emit('message', message['msg']);
    });

    socket.on('create or join', function (room){
        console.log(socket.id + " Trying to join the room : " + room);
        var socketroom = io.sockets.adapter.rooms[room];
    // ('Room ' + room + ' has ' + numClients + ' client(s)');
    // log('Request to create or join room', room);

        if (socketroom === undefined){
            socket.join(room);
            socket.emit('created', room);
        } else if (socketroom.length < 2) {
            io.sockets.in(room).emit('join', room);
            socket.join(room);
            socket.emit('joined', room);
        } else { // max two clients
            socket.emit('full', room);
        }
        // socket.broadcast.emit('join', 'broadcast(): client ' + socket.id + '
        // joined room ' + room);

    });

    socket.on('bye', function (room){
        console.log(socket.id + " Left the room : " + room);
        socket.leave(room);
    });
});

server.listen(8080);
console.log('Server running and listening to the port 8080');
