var Telnet = require('telnet-client');
var fs = require('fs');
var configurationFile = './config/turnserver-connector.json';

export default class TURNConnector {

	/**
	 * The constructor
	 */
	constructor() {

		// Read configuration and store the JSON
		this.configuration = JSON.parse(
			fs.readFileSync(configurationFile)
		);

		// Initialize the Telnet connection
		this.connection = new Telnet();

		// Start Connecting
		this._connect();

	}

	/**
	 * Connect to the TURN Service via Telnet
	 */
	_connect() {
		try {
			this.connection.connect(this.configuration)
				.then(
					(res) => {
						console.log("[TURN] Connection to TURNservice (" + this.configuration.host + ":" + this.configuration.port + ") established.");
					},
					(err) => {
						console.log("[TURN] Connection to TURNservice COULD NOT BE established");
					}
				);
		} catch (error) {
			console.log("[TURN] Somthing failed while connecting to the given TURNservice " + this.configuration.host + ":" + this.configuration.port + ". " + e);
		}
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
				console.log('[TURN] promises reject:', error);
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
		var matches = result.match(/Total sessions: (\d+)/);
		var total_sessions = matches[1];
		return total_sessions;
	}

}
