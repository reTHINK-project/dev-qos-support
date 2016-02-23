import Bus from './bus.js';
import expect from 'expect.js';
import ProtoStubMatrix from '../src/stub/ProtoStubMatrix';
import Config from './configuration.js';

let config = new Config();

describe('Matrix-Stub connect to ' + config.homeserver + ' with messagingnode ' + config.messagingnode, function() {


  /**
   * Tests the connection of a stub internally in a Matrix Domain.
   * This test uses an idToken to authenticate against the Matrix Domain.
   */
  it('stub connected to internal domain with idToken', function(done) {

    let bus = new Bus("steffen", true);

    let configuration = {
      identity : {
        token : config.accounts[0].token
      },
      messagingnode : config.messagingnode
    }
    let stub = new ProtoStubMatrix('hyperty-runtime://' + config.homeserver + '/protostub/1', bus, configuration);

    stub.connect( configuration.identity ).then( (validatedToken) => {

      expect( configuration.identity.token ).to.eql( validatedToken );
      stub.disconnect();
      done();
    },
    (err) => {
      expect.fail();
    });
  });

  /**
  * Tests the connection of a stub internally in a Matrix Domain.
  * This test uses username/password to authenticate against the Matrix Domain.
   */
  it('stub connected to internal domain with username(' + config.accounts[0].username + ') / password (' + config.accounts[0].password + ')', function(done) {

    let bus = new Bus("steffen", true);

    let configuration = {
      identity : {
        user : config.accounts[0].username,
        pwd : config.accounts[0].password
      },
      messagingnode : config.messagingnode
    }
    let stub = new ProtoStubMatrix('hyperty-runtime://' + config.homeserver + '/protostub/1', bus, configuration);

    stub.connect( configuration.identity ).then( (validatedToken) => {

      expect(validatedToken).not.to.be.null;
      stub.disconnect();
      done();
    },
    (err) => {
      expect.fail();
    });
  });

  /**
  * Tests the connection of a stub internally in a Matrix Domain.
  * This test uses username/password to authenticate against the Matrix Domain.
   */
  it('stub connected to internal domain with a second username(' + config.accounts[1].username + ') / password (' + config.accounts[1].password + ')', function(done) {

    let bus = new Bus(config.busName, true);

    let configuration = {
      identity : {
        user : config.accounts[0].username,
        pwd : config.accounts[0].password
      },
      messagingnode : config.messagingnode
    }
    let stub = new ProtoStubMatrix('hyperty-runtime://' + config.homeserver + '/protostub/1', bus, configuration);

    stub.connect( configuration.identity ).then( (validatedToken) => {

      expect(validatedToken).not.to.be.null;
      stub.disconnect();
      done();
    },
    (err) => {
      expect.fail();
    });
  });



  /*
   * The connection of a stub without credentials must be treated as extra domain connect.
   */
  it('stub without credentials is treated as external', function(done) {

    let bus = new Bus("external-hyperty", true);

    let configuration = {
      messagingnode : config.messagingnode
    }
    let stub = new ProtoStubMatrix('hyperty-runtime://' + config.externalruntime + '/protostub/1', bus, configuration);

    stub.connect().then(
      (validatedToken) => {
        expect(validatedToken).to.be.undefined;
        done();
    },
    (err) => {
      expect.fail();
    });
  });

});
