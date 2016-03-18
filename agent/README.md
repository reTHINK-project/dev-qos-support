# dev-qos-broker agent
The repository for the Broker Agent to serve the best feasible TURN Server at the particular network area (by taking into account the current statistics of the network).

### Contents of this repository:
***TODOs:***
* Registering at the broker
* receive list of possible agents to start probing
* update the turn server performance list towards the broker


### Configuration

### Initialization and Operation of the QoS Agent
- ensure that you have a global installation of the gulp task-runner. If not: ***sudo npm install -g gulp***
- first execute ***npm install*** to install the dependencies
- execute ***gulp help*** to see a list of available commands

- ***bash startAgents.sh*** runs some agents on the local machine, because the interaction with the broker is still open
- ***gulp start --address [local address] --port [portnumber] --type [access|turn]*** builds, deploys and runs the qos-broker agent and runs the agent on the given address and port, the type specifies on which side the agent is running.
- ***gulp test*** executes the testcases
