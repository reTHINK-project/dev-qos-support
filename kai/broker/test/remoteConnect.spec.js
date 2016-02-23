import Bus from './bus.js';
import expect from 'expect.js';
import ProtoStubMatrix from '../src/stub/ProtoStubMatrix';
import Config from './configuration.js';

let config = new Config();

describe('Matrix-Stub address allocation and domain external messaging', function() {

  this.timeout(2000);

  let address1 = null;
  let addressExternal = "hyperty://vertx.pt/a7c5e056-a2c0-4504-9808-62a1425411ee";
  let stub1 = null;
  let stub2 = null;
  let seq1 = 0;
  let seq2 = 0;

  let connectStub = (callback, stubId, stubConfig) => {

    return new Promise( (resolve, reject) => {
      let bus = new Bus(callback, false);
      let stub = new ProtoStubMatrix('hyperty-runtime://' + config.homeserver +  '/protostub/' + stubId, bus, stubConfig);

      stub.connect(stubConfig.identity ? stubConfig.identity : null).then((validatedToken) => {
        resolve(stub);

      }, (err) => {
        expect.fail();
        reject();
      });
    })
  };

  let cleanup = () => {
    stub1.disconnect();
    stub2.disconnect();
  }


  /**
   * Tests the connection of a stub internally in a Matrix Domain.
   * This test uses an idToken to authenticate against the Matrix Domain.
   */
  it('connect internal and external stub, send external PING and expect PONG back', function(done) {

    // prepare and connect stub1 with an identity token
    let config1 = {
      identity: {
        user : config.accounts[0].username,
        pwd : config.accounts[0].password
      },
      messagingnode: config.messagingnode
    }

    let callback1 = (m) => {
      seq1++;
      // console.log("stub 1 (internal) got message no " + seq1 + " : " + JSON.stringify(m));
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
          expect(m.to).to.eql("hyperty-runtime://" + config.homeserver +  "/runsteffen/registry/allocation");
          expect(m.body.message).not.to.be.null;
          expect(m.body.allocated.length).to.be(1);
          // store address1
          address1 = m.body.allocated[0];
          // console.log("allocated address for domain internal hyperty: " + address1);

          // run external stub, after hyperty allocation is done
          runExternalStub(done);
      }
      else if ( seq1 === 3 ){
          // this msg is expected to be the the text sent from address1 via stub2 to address1 via stub1
          expect(m.id).to.eql("2");
          expect(m.type).to.eql("PING");
          expect(m.from).to.eql(addressExternal);
          expect(m.to).to.eql(address1);
          expect(m.body.message).to.be.eql("Hello from external Domain");

          let message = {
            "id": "3",
            "type": "PONG",
            "from": address1,
            "to": addressExternal,
            "body": {
              "message": "Thanks and hello back from 1 to external Domain"
            }
          };
          stub1.postMessage(message);

      }
      // else
          // console.log("received unexpected msg" + msg);

    }
    connectStub(callback1, 1, config1).then((stub) => {
      stub1 = stub;
    });

  });


  let runExternalStub = (done) => {
    // don't provide any credentials --> stub must be treated as External (from another domain)
    let config2 = {
      messagingnode: config.messagingnode
    }

    let callback2 = (m) => {
      seq2++;
      // console.log("external stub got message no " + seq2 + " : " + JSON.stringify(m));
      if ( seq2 == 1 ) {
        // this msg is expected to be the the text sent from address1 via stub2 to address1 via stub1
        expect(m.id).to.eql("3");
        expect(m.type).to.eql("PONG");
        expect(m.from).to.eql(address1);
        expect(m.to).to.eql(addressExternal);
        expect(m.body.message).to.be.eql("Thanks and hello back from 1 to external Domain");
        // We are done --> cleaning up
        cleanup();

        done();
      }
    }

    connectStub(callback2, 2, config2).then( (stub) => {
      stub2 = stub;
      // send msg from addressExternal via external stub2 to address1
      let message = {
        "id": "2",
        "type": "PING",
        "from": addressExternal,
        "to": address1,
        "body": {
          "message": "Hello from external Domain"
        }
      };
      // console.log("posting message via external stub: " + JSON.stringify(message));
      stub2.postMessage(message);
    })
  }

});
