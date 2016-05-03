var Telnet = require('telnet-client');
//var telnetConfig = require('../config/turnserver-connector.json');
// //
var params = {
	"host": "127.0.0.1",
	"port": 5766,
	"shellPrompt": "> ",
	"timeout": 1500
}

export default class TURNConnector {

	/**
	 * The constructor
	 */
	constructor() {

		// The Telnet connection
		this.connection = new Telnet();

		this._connect();

	}

	/**
	 * Connect to the TURN Service via Telnet
	 */
	_connect() {
		this.connection.connect(params)
			.then(
				(res) => {
					console.log("Connection to turnservice established");
				},
				(err) => {
					console.log("Connection to turnservice COULD NOT BE established");
				}
			);
	}

	/**
	 * Run the extraction of the established sessions
	 * at the TURN service
	 * @return {int} The number of established TURN sessions
	 */
	runProbe() {
		return new Promise((resolve, reject) => {
					this.connection.exec("ps")
						.then((res) => {
							var total_sessions = this._parseResults(res);
              resolve(total_sessions);
						})
				},
				(error) => {
					console.log('promises reject:', error);
					reject(error);
				}
			);
	}

	/**
	 * Parse the console output and return the number of established
	 * sessions.
	 * @param {String} The content of the turn service output
	 * @return {int} the number of established sessions
	 */
	_parseResults(result) {
		var matches = res.match(/Total sessions: (\d+)/);
		var total_sessions = matches[1];
		return total_sessions;
	}

}
