(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
// shim for using process in browser

var process = module.exports = {};
var queue = [];
var draining = false;
var currentQueue;
var queueIndex = -1;

function cleanUpNextTick() {
    draining = false;
    if (currentQueue.length) {
        queue = currentQueue.concat(queue);
    } else {
        queueIndex = -1;
    }
    if (queue.length) {
        drainQueue();
    }
}

function drainQueue() {
    if (draining) {
        return;
    }
    var timeout = setTimeout(cleanUpNextTick);
    draining = true;

    var len = queue.length;
    while(len) {
        currentQueue = queue;
        queue = [];
        while (++queueIndex < len) {
            if (currentQueue) {
                currentQueue[queueIndex].run();
            }
        }
        queueIndex = -1;
        len = queue.length;
    }
    currentQueue = null;
    draining = false;
    clearTimeout(timeout);
}

process.nextTick = function (fun) {
    var args = new Array(arguments.length - 1);
    if (arguments.length > 1) {
        for (var i = 1; i < arguments.length; i++) {
            args[i - 1] = arguments[i];
        }
    }
    queue.push(new Item(fun, args));
    if (queue.length === 1 && !draining) {
        setTimeout(drainQueue, 0);
    }
};

// v8 likes predictible objects
function Item(fun, array) {
    this.fun = fun;
    this.array = array;
}
Item.prototype.run = function () {
    this.fun.apply(null, this.array);
};
process.title = 'browser';
process.browser = true;
process.env = {};
process.argv = [];
process.version = ''; // empty string to avoid regexp issues
process.versions = {};

function noop() {}

process.on = noop;
process.addListener = noop;
process.once = noop;
process.off = noop;
process.removeListener = noop;
process.removeAllListeners = noop;
process.emit = noop;

process.binding = function (name) {
    throw new Error('process.binding is not supported');
};

process.cwd = function () { return '/' };
process.chdir = function (dir) {
    throw new Error('process.chdir is not supported');
};
process.umask = function() { return 0; };

},{}],2:[function(require,module,exports){
/*

The MIT License (MIT)

Original Library 
  - Copyright (c) Marak Squires

Additional functionality
 - Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/

var colors = {};
module['exports'] = colors;

colors.themes = {};

var ansiStyles = colors.styles = require('./styles');
var defineProps = Object.defineProperties;

colors.supportsColor = require('./system/supports-colors');

if (typeof colors.enabled === "undefined") {
  colors.enabled = colors.supportsColor;
}

colors.stripColors = colors.strip = function(str){
  return ("" + str).replace(/\x1B\[\d+m/g, '');
};


var stylize = colors.stylize = function stylize (str, style) {
  if (!colors.enabled) {
    return str+'';
  }

  return ansiStyles[style].open + str + ansiStyles[style].close;
}

var matchOperatorsRe = /[|\\{}()[\]^$+*?.]/g;
var escapeStringRegexp = function (str) {
  if (typeof str !== 'string') {
    throw new TypeError('Expected a string');
  }
  return str.replace(matchOperatorsRe,  '\\$&');
}

function build(_styles) {
  var builder = function builder() {
    return applyStyle.apply(builder, arguments);
  };
  builder._styles = _styles;
  // __proto__ is used because we must return a function, but there is
  // no way to create a function with a different prototype.
  builder.__proto__ = proto;
  return builder;
}

var styles = (function () {
  var ret = {};
  ansiStyles.grey = ansiStyles.gray;
  Object.keys(ansiStyles).forEach(function (key) {
    ansiStyles[key].closeRe = new RegExp(escapeStringRegexp(ansiStyles[key].close), 'g');
    ret[key] = {
      get: function () {
        return build(this._styles.concat(key));
      }
    };
  });
  return ret;
})();

var proto = defineProps(function colors() {}, styles);

function applyStyle() {
  var args = arguments;
  var argsLen = args.length;
  var str = argsLen !== 0 && String(arguments[0]);
  if (argsLen > 1) {
    for (var a = 1; a < argsLen; a++) {
      str += ' ' + args[a];
    }
  }

  if (!colors.enabled || !str) {
    return str;
  }

  var nestedStyles = this._styles;

  var i = nestedStyles.length;
  while (i--) {
    var code = ansiStyles[nestedStyles[i]];
    str = code.open + str.replace(code.closeRe, code.open) + code.close;
  }

  return str;
}

function applyTheme (theme) {
  for (var style in theme) {
    (function(style){
      colors[style] = function(str){
        if (typeof theme[style] === 'object'){
          var out = str;
          for (var i in theme[style]){
            out = colors[theme[style][i]](out);
          }
          return out;
        }
        return colors[theme[style]](str);
      };
    })(style)
  }
}

colors.setTheme = function (theme) {
  if (typeof theme === 'string') {
    try {
      colors.themes[theme] = require(theme);
      applyTheme(colors.themes[theme]);
      return colors.themes[theme];
    } catch (err) {
      console.log(err);
      return err;
    }
  } else {
    applyTheme(theme);
  }
};

function init() {
  var ret = {};
  Object.keys(styles).forEach(function (name) {
    ret[name] = {
      get: function () {
        return build([name]);
      }
    };
  });
  return ret;
}

var sequencer = function sequencer (map, str) {
  var exploded = str.split(""), i = 0;
  exploded = exploded.map(map);
  return exploded.join("");
};

// custom formatter methods
colors.trap = require('./custom/trap');
colors.zalgo = require('./custom/zalgo');

// maps
colors.maps = {};
colors.maps.america = require('./maps/america');
colors.maps.zebra = require('./maps/zebra');
colors.maps.rainbow = require('./maps/rainbow');
colors.maps.random = require('./maps/random')

for (var map in colors.maps) {
  (function(map){
    colors[map] = function (str) {
      return sequencer(colors.maps[map], str);
    }
  })(map)
}

defineProps(colors, init());
},{"./custom/trap":3,"./custom/zalgo":4,"./maps/america":5,"./maps/rainbow":6,"./maps/random":7,"./maps/zebra":8,"./styles":9,"./system/supports-colors":10}],3:[function(require,module,exports){
module['exports'] = function runTheTrap (text, options) {
  var result = "";
  text = text || "Run the trap, drop the bass";
  text = text.split('');
  var trap = {
    a: ["\u0040", "\u0104", "\u023a", "\u0245", "\u0394", "\u039b", "\u0414"],
    b: ["\u00df", "\u0181", "\u0243", "\u026e", "\u03b2", "\u0e3f"],
    c: ["\u00a9", "\u023b", "\u03fe"],
    d: ["\u00d0", "\u018a", "\u0500" , "\u0501" ,"\u0502", "\u0503"],
    e: ["\u00cb", "\u0115", "\u018e", "\u0258", "\u03a3", "\u03be", "\u04bc", "\u0a6c"],
    f: ["\u04fa"],
    g: ["\u0262"],
    h: ["\u0126", "\u0195", "\u04a2", "\u04ba", "\u04c7", "\u050a"],
    i: ["\u0f0f"],
    j: ["\u0134"],
    k: ["\u0138", "\u04a0", "\u04c3", "\u051e"],
    l: ["\u0139"],
    m: ["\u028d", "\u04cd", "\u04ce", "\u0520", "\u0521", "\u0d69"],
    n: ["\u00d1", "\u014b", "\u019d", "\u0376", "\u03a0", "\u048a"],
    o: ["\u00d8", "\u00f5", "\u00f8", "\u01fe", "\u0298", "\u047a", "\u05dd", "\u06dd", "\u0e4f"],
    p: ["\u01f7", "\u048e"],
    q: ["\u09cd"],
    r: ["\u00ae", "\u01a6", "\u0210", "\u024c", "\u0280", "\u042f"],
    s: ["\u00a7", "\u03de", "\u03df", "\u03e8"],
    t: ["\u0141", "\u0166", "\u0373"],
    u: ["\u01b1", "\u054d"],
    v: ["\u05d8"],
    w: ["\u0428", "\u0460", "\u047c", "\u0d70"],
    x: ["\u04b2", "\u04fe", "\u04fc", "\u04fd"],
    y: ["\u00a5", "\u04b0", "\u04cb"],
    z: ["\u01b5", "\u0240"]
  }
  text.forEach(function(c){
    c = c.toLowerCase();
    var chars = trap[c] || [" "];
    var rand = Math.floor(Math.random() * chars.length);
    if (typeof trap[c] !== "undefined") {
      result += trap[c][rand];
    } else {
      result += c;
    }
  });
  return result;

}

},{}],4:[function(require,module,exports){
// please no
module['exports'] = function zalgo(text, options) {
  text = text || "   he is here   ";
  var soul = {
    "up" : [
      '̍', '̎', '̄', '̅',
      '̿', '̑', '̆', '̐',
      '͒', '͗', '͑', '̇',
      '̈', '̊', '͂', '̓',
      '̈', '͊', '͋', '͌',
      '̃', '̂', '̌', '͐',
      '̀', '́', '̋', '̏',
      '̒', '̓', '̔', '̽',
      '̉', 'ͣ', 'ͤ', 'ͥ',
      'ͦ', 'ͧ', 'ͨ', 'ͩ',
      'ͪ', 'ͫ', 'ͬ', 'ͭ',
      'ͮ', 'ͯ', '̾', '͛',
      '͆', '̚'
    ],
    "down" : [
      '̖', '̗', '̘', '̙',
      '̜', '̝', '̞', '̟',
      '̠', '̤', '̥', '̦',
      '̩', '̪', '̫', '̬',
      '̭', '̮', '̯', '̰',
      '̱', '̲', '̳', '̹',
      '̺', '̻', '̼', 'ͅ',
      '͇', '͈', '͉', '͍',
      '͎', '͓', '͔', '͕',
      '͖', '͙', '͚', '̣'
    ],
    "mid" : [
      '̕', '̛', '̀', '́',
      '͘', '̡', '̢', '̧',
      '̨', '̴', '̵', '̶',
      '͜', '͝', '͞',
      '͟', '͠', '͢', '̸',
      '̷', '͡', ' ҉'
    ]
  },
  all = [].concat(soul.up, soul.down, soul.mid),
  zalgo = {};

  function randomNumber(range) {
    var r = Math.floor(Math.random() * range);
    return r;
  }

  function is_char(character) {
    var bool = false;
    all.filter(function (i) {
      bool = (i === character);
    });
    return bool;
  }
  

  function heComes(text, options) {
    var result = '', counts, l;
    options = options || {};
    options["up"] =   typeof options["up"]   !== 'undefined' ? options["up"]   : true;
    options["mid"] =  typeof options["mid"]  !== 'undefined' ? options["mid"]  : true;
    options["down"] = typeof options["down"] !== 'undefined' ? options["down"] : true;
    options["size"] = typeof options["size"] !== 'undefined' ? options["size"] : "maxi";
    text = text.split('');
    for (l in text) {
      if (is_char(l)) {
        continue;
      }
      result = result + text[l];
      counts = {"up" : 0, "down" : 0, "mid" : 0};
      switch (options.size) {
      case 'mini':
        counts.up = randomNumber(8);
        counts.mid = randomNumber(2);
        counts.down = randomNumber(8);
        break;
      case 'maxi':
        counts.up = randomNumber(16) + 3;
        counts.mid = randomNumber(4) + 1;
        counts.down = randomNumber(64) + 3;
        break;
      default:
        counts.up = randomNumber(8) + 1;
        counts.mid = randomNumber(6) / 2;
        counts.down = randomNumber(8) + 1;
        break;
      }

      var arr = ["up", "mid", "down"];
      for (var d in arr) {
        var index = arr[d];
        for (var i = 0 ; i <= counts[index]; i++) {
          if (options[index]) {
            result = result + soul[index][randomNumber(soul[index].length)];
          }
        }
      }
    }
    return result;
  }
  // don't summon him
  return heComes(text, options);
}

},{}],5:[function(require,module,exports){
var colors = require('../colors');

module['exports'] = (function() {
  return function (letter, i, exploded) {
    if(letter === " ") return letter;
    switch(i%3) {
      case 0: return colors.red(letter);
      case 1: return colors.white(letter)
      case 2: return colors.blue(letter)
    }
  }
})();
},{"../colors":2}],6:[function(require,module,exports){
var colors = require('../colors');

module['exports'] = (function () {
  var rainbowColors = ['red', 'yellow', 'green', 'blue', 'magenta']; //RoY G BiV
  return function (letter, i, exploded) {
    if (letter === " ") {
      return letter;
    } else {
      return colors[rainbowColors[i++ % rainbowColors.length]](letter);
    }
  };
})();


},{"../colors":2}],7:[function(require,module,exports){
var colors = require('../colors');

module['exports'] = (function () {
  var available = ['underline', 'inverse', 'grey', 'yellow', 'red', 'green', 'blue', 'white', 'cyan', 'magenta'];
  return function(letter, i, exploded) {
    return letter === " " ? letter : colors[available[Math.round(Math.random() * (available.length - 1))]](letter);
  };
})();
},{"../colors":2}],8:[function(require,module,exports){
var colors = require('../colors');

module['exports'] = function (letter, i, exploded) {
  return i % 2 === 0 ? letter : colors.inverse(letter);
};
},{"../colors":2}],9:[function(require,module,exports){
/*
The MIT License (MIT)

Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/

var styles = {};
module['exports'] = styles;

var codes = {
  reset: [0, 0],

  bold: [1, 22],
  dim: [2, 22],
  italic: [3, 23],
  underline: [4, 24],
  inverse: [7, 27],
  hidden: [8, 28],
  strikethrough: [9, 29],

  black: [30, 39],
  red: [31, 39],
  green: [32, 39],
  yellow: [33, 39],
  blue: [34, 39],
  magenta: [35, 39],
  cyan: [36, 39],
  white: [37, 39],
  gray: [90, 39],
  grey: [90, 39],

  bgBlack: [40, 49],
  bgRed: [41, 49],
  bgGreen: [42, 49],
  bgYellow: [43, 49],
  bgBlue: [44, 49],
  bgMagenta: [45, 49],
  bgCyan: [46, 49],
  bgWhite: [47, 49],

  // legacy styles for colors pre v1.0.0
  blackBG: [40, 49],
  redBG: [41, 49],
  greenBG: [42, 49],
  yellowBG: [43, 49],
  blueBG: [44, 49],
  magentaBG: [45, 49],
  cyanBG: [46, 49],
  whiteBG: [47, 49]

};

Object.keys(codes).forEach(function (key) {
  var val = codes[key];
  var style = styles[key] = [];
  style.open = '\u001b[' + val[0] + 'm';
  style.close = '\u001b[' + val[1] + 'm';
});
},{}],10:[function(require,module,exports){
(function (process){
/*
The MIT License (MIT)

Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/

var argv = process.argv;

module.exports = (function () {
  if (argv.indexOf('--no-color') !== -1 ||
    argv.indexOf('--color=false') !== -1) {
    return false;
  }

  if (argv.indexOf('--color') !== -1 ||
    argv.indexOf('--color=true') !== -1 ||
    argv.indexOf('--color=always') !== -1) {
    return true;
  }

  if (process.stdout && !process.stdout.isTTY) {
    return false;
  }

  if (process.platform === 'win32') {
    return true;
  }

  if ('COLORTERM' in process.env) {
    return true;
  }

  if (process.env.TERM === 'dumb') {
    return false;
  }

  if (/^screen|^xterm|^vt100|color|ansi|cygwin|linux/i.test(process.env.TERM)) {
    return true;
  }

  return false;
})();
}).call(this,require('_process'))
},{"_process":1}],11:[function(require,module,exports){
//
// Remark: Requiring this file will use the "safe" colors API which will not touch String.prototype
//
//   var colors = require('colors/safe);
//   colors.red("foo")
//
//
var colors = require('./lib/colors');
module['exports'] = colors;
},{"./lib/colors":2}],12:[function(require,module,exports){
/**
 * Main file for the QoS Agent
 *
 */

// Imports
'use strict';

Object.defineProperty(exports, '__esModule', {
  value: true
});

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

var requestify = require('requestify');
var express = require('express');
var microtime = require('microtime');
var http = require("http");
var myProcess = require('process');
//var colors = require('colors');
var level = require('../log/level.json');
var colors = require('colors/safe');

var Agent = (function () {

  /**
   * The constructor
   */

  function Agent() {
    _classCallCheck(this, Agent);

    this.applicationLogLevel = level.Information;

    this.turnUser = this._generateUUID();
    this.turnPass = this._generateUUID();
    this._ownId = this._generateUUID();

    this._log(level.Information, "Running Agent: " + this._ownId);

    // Global vars
    this.servingAreaName = "XY";
    this.isAccessAgent = true;
    // TODO: Extract such information from a config file
    this.brokerUrl = "http://localhost:8667";
    this.restApp = express();
    this.restPort = 10000;
    this.timers = [];
    this.ownIp = ""; //To be edited and based on the TURN config
    this.TURNPort = 3478; //To be edited and based on the TURN config
    this.servingAreaServers = {};
    this.nodeTimeout = 100; //seconds
    this.isRegisteredAtBroker = true; // for development, we set this value to true
    this.registerTimeout = 1000 * 5000; // When do ne need to update the registration for this agent at the broker
    this.registerAttempts = 0;
    this.unsuccessfulRegisterTimeout = 5 * 1000; // the time, when the registering is being tried again
    this.probingTimeout = 5 * 1000;
    this.maxProbes = 10;
    this.showStateTimeout = 10 * 1000;

    this.turnAgentList = ["127.0.0.1:20000", "127.0.0.1:20001", "127.0.0.1:20002", "127.0.0.1:20003", "127.0.0.1:20004", "127.0.0.1:20005", "127.0.0.1:20006", "127.0.0.1:20007", "127.0.0.1:20008"];

    /**
     * the overall object which stores that lastest performance values
     * of the probed Turn Agents
     *
     * The structure is the following
     * "agentid": {
     *    agentAddress = "xx.xx.xx.xx:ppppp",
     *   lastProbes = [<integer oldest>, ..., <integer newest>],
     *   meanProbes = <integer>
     *   lastSeen = <UnixTimestamp>
     * }
     */
    this.turnProbes = {};
  }

  // end constructor

  /**
   * Starting the stuff
   */

  _createClass(Agent, [{
    key: 'start',
    value: function start() {
      this._runWatchDogs();
    }

    /**
     * run the watchdogs and timers
     */
  }, {
    key: '_runWatchDogs',
    value: function _runWatchDogs() {
      var _this = this;

      this._log(level.Debug, "_runWatchDogs called");

      // Check if the registeing is set to true
      if (this.isRegisteredAtBroker) {

        this._log(level.Debug, "Agent is registered at broker, setting timeout for re-registration");

        // Re-Register at Broker with the given timer
        setTimeout(function () {
          _this.isRegisteredAtBroker = false;
          _this._registerAtBroker();
        }, this.registerTimeout);

        // if we are an access agent, we start the probing
        if (this.isAccessAgent) {
          setInterval(function () {
            _this._runProbing();
          }, this.probingTimeout);

          // Status Report to console
          setInterval(function () {
            _this._statusReport();
          }, this.showStateTimeout);
        }
      } else {
        // Try to register at Broker
        this._registerAtBroker();
        // Try again if the registering was not successful
        setTimeout(function () {
          _this._runWatchDogs();
        }, this.unsuccessfulRegisterTimeout);
      }
    }
    // end runWatchDogs

    /**
     * Show an Status Report the current Status of all reached TURN Agents
     */
  }, {
    key: '_statusReport',
    value: function _statusReport() {
      this._log(level.Information, "Current stored TURN Agents");
      var agentAddress = undefined;
      var meanProbes = undefined;
      var lastSeen = undefined;
      for (var entry in this.turnProbes) {
        agentAddress = this.turnProbes[entry].agentAddress;
        meanProbes = this.turnProbes[entry].meanProbes;
        lastSeen = parseInt((new Date().getTime() - this.turnProbes[entry].lastSeen) / 1000);
        this._log(level.Information, entry + "\t" + agentAddress + "\t" + "~" + meanProbes + "ms\t" + "last seen before " + lastSeen + "s");
      }
      this._log(level.Information, "--------------------------");
    }

    /**
     * Register at the broker,
     * this is a TODO!!!!
     */
  }, {
    key: '_registerAtBroker',
    value: function _registerAtBroker() {
      this._log(level.Debug, "Registering at Broker " + this.brokerUrl + "/turn-servers-management/register/");
      requestify.post(this.brokerUrl + "/turn-servers-management/register/", {
        agentAddress: this.ownIp + ":" + this.restPort,
        servingArea: this.servingArea,
        action: "register",
        isAccessAgent: this.isAccessAgent
      }).then(function (response) {
        console.log("The level debug: " + level.Debug);
        this._log(level.Debug, "Incoming response from broker");
        this.isRegisteredAtBroker = this._validate("registerResponse", response);
      }, function (err) {
        this._log(level.Error, "Error registering at the Broker " + JSON.stringify(err));
      });
    }
    // end registerAtBroker

    /**
     * Run the actial probing between the agents.
     * We will go though the list this.turnAgentList
     */
  }, {
    key: '_runProbing',
    value: function _runProbing() {
      for (var i = 0; i < this.turnAgentList.length; i++) {
        this._callRemoteAgent(this.turnAgentList[i]);
      }
    }
    // end runProbing

    /**
     * Executes the actual call towards the other Agents
     * @param host The host we want to address, the requested format: <name/ipAddress>:port
     */
  }, {
    key: '_callRemoteAgent',
    value: function _callRemoteAgent(host) {
      var _this2 = this;

      try {
        this._log(level.Debug, " _callRemoteAgent: Calling the remote agent http://" + host + '/performance/ping/' + this.servingAreaName + '/' + microtime.now());
        requestify.get("http://" + host + '/performance/ping/' + this._ownId + '/' + microtime.now()).then(function (response) {
          var body = response.getBody();
          var now = microtime.now();
          var calc = Math.round((now - parseInt(body.reqTimestamp)) / 1000);
          _this2._log(level.Debug, " _callRemoteAgent [" + host + ", " + body.agentId + "] Response received,  RTT: " + calc + "ms");
          _this2._storeProbe(body.agentId, host, calc);
        }, function (err) {
          _this2._log(level.Error, "Error trying to ping the given agent address (" + host + " ): " + err.message);
        });
      } catch (err) {
        this._log(level.Error, "Catched error: " + err.message);
      }
    }
    // end callRemoteAgent

    /**
     * Store the received probe into the interal array
     * we push that in another interval towards the broker
     * @param agentId The Id of the answering agent, to see the relationship between the agents
     * @param host The Host Name/IP Address + Port
     * @param calc The actual calculated Round-Trip Time
     */
  }, {
    key: '_storeProbe',
    value: function _storeProbe(agentId, host, calc) {

      this._log(level.Debug, " _storeProbe: storing the current probe " + agentId + ", " + host + ", " + calc);

      var turnAgent = this.turnProbes[agentId];
      if (turnAgent == undefined) {
        this.turnProbes[agentId] = {
          agentAddress: host,
          lastProbes: [calc],
          meanProbes: [calc],
          lastSeen: new Date().getTime()
        };
      } else {

        var lastProbes = this.turnProbes[agentId].lastProbes;

        // Check if the probes are already stored, if not create new element for lastProbes
        // and store the first value
        if (lastProbes == undefined) {
          lastProbes = [calc];
        } else {
          // check if the maximum number of probe samples are reached
          // if yes, remove the oldest
          if (lastProbes.length > this.maxProbes) {
            lastProbes.shift(); // remove the first element (oldest)
          }
          // push the probe to the array
          lastProbes.push(calc);
        }

        // Calculate the mean of the probes
        var meanProbes = 0;
        for (var i = 0; i < lastProbes.length; i++) {
          meanProbes += lastProbes[i];
        }
        meanProbes = meanProbes / lastProbes.length;

        // push everything into the overall objet turnProbes
        this.turnProbes[agentId] = {
          agentAddress: host,
          lastProbes: lastProbes,
          meanProbes: parseInt(meanProbes),
          lastSeen: new Date().getTime()
        };
      }
    }

    /**
     * Validate the given content and response with true or false
     */
  }, {
    key: '_validate',
    value: function _validate(what, content) {
      var ret = true;

      switch (what) {
        case "registerResponse":
          ret = this._validate("isJSON", content);
          ret = ret && content.resultCode == 200;
          break;

        case "isJSON":
          try {
            JSON.parse(content);
            ret = true;
          } catch (e) {
            ret = false;
          }
          break;

        case "isUndefined":
          ret = content == undefined;
          break;

      }
      // return the validation result
      return ret;
    }
    // end validate

    /**
     * The initializing Function
     */
  }, {
    key: 'initialize',
    value: function initialize() {
      this._log(level.Debug, "Using the following IP Address: " + myProcess.argv[2]);
      if (myProcess.argv[2]) {
        this.ownIp = myProcess.argv[2];
      }

      this._log(level.Debug, "Using the following port: " + myProcess.argv[3]);
      if (myProcess.argv[3] && !isNaN(myProcess.argv[3])) {
        this.restPort = myProcess.argv[3];
      }

      this._log(level.Debug, "Agent type: " + myProcess.argv[4]);
      if (myProcess.argv[4] && myProcess.argv[4] == "access") {
        this.isAccessAgent = true;
      } else {
        this.isAccessAgent = false;
      }

      this.startREST();
    }
    // end initialize

    /**
     * Register the REST Resources for the agent performance measurements
     */
  }, {
    key: 'startREST',
    value: function startREST() {
      var _this3 = this;

      // Check if the system is an Access Agent,
      // If not, we have a TURN Agent and hence,
      // we activate the REST Interface for ping/pong the agents
      if (this.isAccessAgent) {
        this._log(level.Debug, "Running as Access Agent Service, no REST Interface will be started");
      } else {
        //ok, lets activate the REST for TURN Agents
        this._log(level.Debug, "Starting the REST Interface for TURN Agents");

        this.restApp.get('/performance/ping/:requestingAgent/:reqTimestamp', function (req, res) {

          var responeJSON = {};
          var statusCode = 200;

          // Check for valid timestamp
          if (!isNaN(req.params.reqTimestamp)) {
            responeJSON = {
              "reqTimestamp": req.params.reqTimestamp,
              "agentId": _this3._ownId,
              "pingpong": "pong"
            };
            statusCode = 200;

            _this3._log(level.Debug, "Received successful ping request from " + req.params.requestingAgent + ", responding accordingly");
          } else {
            responeJSON = {
              "error": "Timestamp is not set."
            };
            statusCode = 400;
          }
          res.status(statusCode).send(responeJSON);
        });

        this.restApp.listen(this.restPort);

        this._log(level.Debug, "Running Agent at HTTP/REST Port: " + this.restPort);
      }
    }
    // end startRest

    /**
     * Generate a UUID
     * (credits go to http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript)
     * @return uuid {String} ... the generated unique Identifier
     **/
  }, {
    key: '_generateUUID',
    value: function _generateUUID() {
      var d = new Date().getTime();
      var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (d + Math.random() * 16) % 16 | 0;
        d = Math.floor(d / 16);
        return (c == 'x' ? r : r & 0x3 | 0x8).toString(16);
      });
      return uuid;
    }
    // end GenerateUUID

  }, {
    key: '_randomInt',
    value: function _randomInt(low, high) {
      return Math.floor(Math.random() * (high - low) + low);
    }
  }, {
    key: '_log',
    value: function _log(lvl, str) {
      //console.log("The applicationLogLevel: " + this.applicationLogLevel);

      if (this._validate("isJSON", str)) {
        str = JSON.stringify(str);
      }

      switch (lvl) {
        case level.Debug:
          if (this.applicationLogLevel <= lvl) {
            str = "[Debug] " + str;
            console.log(str);
          }
          break;

        case level.Information:
          if (this.applicationLogLevel <= lvl) {
            str = "[Information] " + str;
            console.log(str);
          }
          break;

        case level.Warning:
          if (this.applicationLogLevel <= lvl) {
            str = "[Warning] " + str;
            console.log(str);
          }
          break;

        case level.Error:
          if (this.applicationLogLevel <= lvl) {
            str = "[Error] " + str;
            console.log(str);
          }
          break;

        case level.Fatal:
          if (this.applicationLogLevel <= lvl) {
            str = "[Fatal] " + str;
            console.log(str);
          }
          break;

        default:
          str = "[Debug] " + str;
          console.log(str);
          break;
      }
    }
    // end log
  }]);

  return Agent;
})();

exports['default'] = Agent;
module.exports = exports['default'];

},{"../log/level.json":13,"colors/safe":11,"express":undefined,"http":undefined,"microtime":undefined,"process":undefined,"requestify":undefined}],13:[function(require,module,exports){
module.exports={
  "Debug": 100,
  "Information": 200,
  "Warning":  300,
  "Error": 400,
  "Fatal": 500
}

},{}],14:[function(require,module,exports){
'use strict';

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

var _agentAgentJs = require('./agent/agent.js');

var _agentAgentJs2 = _interopRequireDefault(_agentAgentJs);

// Create agent object
var agent = new _agentAgentJs2['default']();

// Start the Agent
agent.initialize();
agent.start();

},{"./agent/agent.js":12}]},{},[14]);
