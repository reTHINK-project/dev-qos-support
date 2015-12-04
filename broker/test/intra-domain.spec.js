import Bus from './bus.js';
import expect from 'expect.js';
import ProtoStubMatrix from '../src/stub/ProtoStubMatrix';
import Config from './configuration.js';

let config = new Config();

describe('Matrix-Stub address allocation and domain internal messaging. Matrix Homeserver: ' + config.homeserver, function() {

  this.timeout(3000);

  let address1 = null;
  let address2 = null;
  let stub1 = null;
  let stub2 = null;
  let seq1 = 0;
  let seq2 = 0;

  let connectStub = (callback, stubId, stubConfig) => {

    let bus = new Bus(callback, false);
    let stub = new ProtoStubMatrix('hyperty-runtime://' + config.homeserver + '/protostub/' + stubId, bus, stubConfig);

    stub.connect(stubConfig.identity).then((validatedToken) => {

    }, (err) => {
      expect.fail();
    });
    return stub;
  };

  let cleanup = () => {
    stub1.disconnect();
    stub2.disconnect();
  }

  /**
   * Tests the connection of a stub internally in a Matrix Domain.
   * This test uses an idToken to authenticate against the Matrix Domain.
   */
  it('allocate hyperty addresses via 2 stubs and send PING/PONG between them', function(done) {

    // prepare and connect stub1 with an identity token
    let config1 = {
      identity: {
        // token: "QHN0ZWZmZW46bWF0cml4LmRvY2tlcg...fVQroZzieCAGpKXzmt"
        user : config.accounts[0].username,
        pwd : config.accounts[0].password
      },
      messagingnode: config.messagingnode
    }

    let callback1 = (m) => {
      seq1++;
      // console.log("stub 1 got message no " + seq1 + " : " + JSON.stringify(m));
      if ( seq1 === 1 ) {
        expect(m).to.eql("SYNC COMPLETE");
        let allocateMsg = {
          "id": "1",
          "type": "CREATE",
          "from": "hyperty-runtime://" + config.homeserver +  "/runsteffen/registry/allocation",
          "to": "domain://msg-node." + config.homeserver +  "/hyperty-address-allocation",
          "body": {
            "number": 1
          }
        };
        stub1.postMessage(allocateMsg);
      }
      else if ( seq1 === 2 ) {
          // this message is expected to be the allocation response
          expect(m.id).to.eql("1");
          expect(m.type).to.eql("RESPONSE");
          expect(m.from).to.eql("domain://msg-node." + config.homeserver +  "/hyperty-address-allocation");
          expect(m.to).to.eql("hyperty-runtime://" + config.homeserver + "/runsteffen/registry/allocation");
          expect(m.body.message).not.to.be.null;
          expect(m.body.allocated.length).to.be(1);
          // store address1
          address1 = m.body.allocated[0];
          // console.log("allocated address for hyperty 1: " + address1);
      }
      else if ( seq1 === 3 ){

          // this msg is expected to be the the text sent from address1 via stub2 to address1 via stub1
          expect(m.id).to.eql("2");
          expect(m.type).to.eql("PING");
          expect(m.from).to.eql(address2);
          expect(m.to).to.eql(address1);
          expect(m.body.message).to.be.eql("Hello from 2 to 1");

          let message = {
            "id": "3",
            "type": "PONG",
            "from": address1,
            "to": address2,
            "body": {
              "message": "Thanks and hello back from 1 to 2"
            }
          };
          // console.log("posting message via stub2: " + JSON.stringify(message));
          stub1.postMessage(message);
      }
      else
          console.log("stub1: received unexpected msg" + msg);

    }
    stub1 = connectStub(callback1, 1, config1);



    let config2 = {
      identity: {
        // token: "QGhvcnN0Om1hdHJpeC5kb2NrZXI..ROuHTAmfmcHigPvkJK"
        user : config.accounts[1].username,
        pwd : config.accounts[1].password
      },
      messagingnode: config.messagingnode
    }

    let callback2 = (m) => {
      seq2++;
      // console.log("stub 2 got message no " + seq2 + " : " + JSON.stringify(m));

      if ( seq2 === 1 ) {
        expect(m).to.eql("SYNC COMPLETE");
        let allocateMsg = {
          "id": "1",
          "type": "CREATE",
          "from": "hyperty-runtime://" + config.homeserver + "/runhorst/registry/allocation",
          "to": "domain://msg-node." + config.homeserver + "/hyperty-address-allocation",
          "body": {
            "number": 1
          }
        };
        stub2.postMessage(allocateMsg);
      }
      else
      if (seq2 === 2) {
        expect(m.id).to.eql("1");
        expect(m.type).to.eql("RESPONSE");
        expect(m.from).to.eql("domain://msg-node." + config.homeserver + "/hyperty-address-allocation");
        expect(m.to).to.eql("hyperty-runtime://" + config.homeserver + "/runhorst/registry/allocation");
        expect(m.body.allocated).not.to.be.null
        expect(m.body.allocated.length).to.be(1);
        address2 = m.body.allocated[0];
        // console.log("allocated address for hyperty 2: " + address2);

        // send msg from address2 via stub2 to address 1
        let message = {
          "id": "2",
          "type": "PING",
          "from": address2,
          "to": address1,
          "body": {
            "message": "Hello from 2 to 1"
          }
        };
        // console.log("posting message via stub2: " + JSON.stringify(message));
        stub2.postMessage(message);
      }
      else
      if (seq2 == 3) {
        // this msg is expected to be the the text sent from address1 via stub2 to address1 via stub1
        expect(m.id).to.eql("3");
        expect(m.type).to.eql("PONG");
        expect(m.from).to.eql(address1);
        expect(m.to).to.eql(address2);
        expect(m.body.message).to.be.eql("Thanks and hello back from 1 to 2");
        // We are done --> cleaning up
        cleanup();
        done();

      }
      else
          console.log("stub2 received unexpected msg " + JSON.stringify(m));

    }
    stub2 = connectStub(callback2, 2, config2);

  });

});
