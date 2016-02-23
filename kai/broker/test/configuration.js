/**
 * The configuration we need to configure to have the tests running.
 * After creating the Configuration Object. All data is available by
 * accessing the fields.
 * @author: Kay Haensge, Steffen Druesedow
 */
export default class Configuration {
  constructor() {

    /**
     *  The Matrix Homeserver of the given domain
     */
    this.homeserver = "localdocker";

    /**
     * The Domain for an external communication
     */
    this.externalruntime = "external.runtime";

    /**
     * The messaging node we connect to.
     * Pattern: <protocoll>://<hostname>:<port>/url/to/connect
     */
    this.messagingnode = "ws://localhost:8001/stub/connect";

    /**
     * The Name of the runtime Bus
     */
    this.busName = "Steffen";

    /**
     * The List of accounts for testing.
     */
    this.accounts = new Array();

    // A user account we use for testing
    this.accounts.push({
      username: "@test1:" + this.homeserver,
      password: "test1test1",
      token: "QHN0ZWZmZW46bWF0cml4LmRvY2tlcg...fVQroZzieCAGpKXzmt"
    });

    // A user account we use for testing
    this.accounts.push({
      username: "@test2:" + this.homeserver,
      password: "test2test2"
    });

  }
}
