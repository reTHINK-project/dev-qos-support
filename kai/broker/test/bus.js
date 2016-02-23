
export default class Bus {
  constructor(callback, doLog) {
    this.doLog = doLog;
    this.callback = callback;
  }

  postMessage(msg) {
    if (this.doLog)
      console.log('Bus "%s" got msg: %s', msg);

    if ( this.callback )
      this.callback(msg);
  }

}
