var gulp = require('gulp');
var exec = require('child_process').exec;
var shell = require('gulp-shell');
var util = require('gulp-util');



console.log(util.env);
// Gulp task to generate development documentation;
gulp.task('doc_alt', function(done) {

  console.log('Generating documentation... (this exec stuff does not really work)');
  exec('node_modules/.bin/jsdoc -R readme.md -d docs src/*', function(err, stdout, stderr) {
    if (err) return done(err);
    console.log('Documentation generated in "docs" directory');
    done();
  });

});

gulp.task('doc', [], shell.task([
  'node_modules/.bin/jsdoc -R README.md -d docs src/**'
]));

// Task and dependencies to distribute for all environments;
var babel = require('babelify');
var browserify = require('browserify');
var source = require('vinyl-source-stream');
var exec = require('child_process');

gulp.task('build', function() {
  var stubBundler = browserify(['./src/main.js'],
  {
    debug: false
  }).
  exclude('express').
  exclude('requestify').
  exclude('microtime').
  exclude('http').
  exclude('process').
  exclude('body-parser').
  transform(babel);

  function rebundle() {
    stubBundler.bundle()
      .on('error', function(err) {
        console.error(err);
        this.emit('end');
      })
      .pipe(source('qosbroker-agent.js'))
      .pipe(gulp.dest('./dist'));
  }
  rebundle();
});


gulp.task('dist', ['build'], shell.task([
  'mkdir -p dist',
  'cd ./dist && mkdir -p node_modules',
  'cd ./dist/node_modules && ln -s -f ../../node_modules/promise',
  'cd ./dist/node_modules && ln -s -f ../../node_modules/url',
  'cd ./dist/node_modules && ln -s -f ../../node_modules/websocket',
  'cd ./dist/node_modules && ln -s -f ../../node_modules/express'
]))

gulp.task('start', ['dist'], shell.task([
  'sleep 1',
  'cd dist && node qosbroker-agent.js ' + util.env.address + ' ' + util.env.port + ' ' + util.env.type
]));

gulp.task('test', ['stoptest', 'dist'], shell.task([
  //'cd ../broker/dist && screen -dmS qosbroker node qosbroker.js',
  'screen -dmS a1 gulp start --address 127.0.0.1 --port 10000 --type access',
  //'screen -dmS a2 gulp start --address 127.0.0.1 --port 10001 --type access',
  //'screen -dmS a3 gulp start --address 127.0.0.1 --port 10002 --type turn',
  //'screen -ls'
]));

gulp.task('stoptest', shell.task([
  'screen -X -S "agent1" quit',
  'screen -X -S "agent2" quit',
  'screen -X -S "agent3" quit',
  'screen -X -S "qosbroker" quit'
], {ignoreErrors:true}));

gulp.task('default', ['help'], shell.task([
]));

gulp.task('help', function() {
  console.log('\nThe following gulp tasks are available:\n');
  console.log('gulp' + ' ' + 'help\t\t' + '# show this help\n');
  console.log('gulp' + ' ' + 'doc\t\t' + '# generates documentation in docs folder\n');
  console.log('gulp' + ' ' + 'build\t\t' + '# transpile and bundle the Qos Broker Agent Sources\n');
  console.log('gulp' + ' ' + 'dist\t\t' + '# creates dist folder with transpiled code (depends on build)\n');
  console.log('gulp' + ' ' + 'start --address [address] --port [port] --type [access|turn]\t\t' + '# starts the QoSBroker Agent from dist folder (depends on dist)\n');
  console.log('gulp' + ' ' + 'test\t\t' + '# executes the test cases\n');
});
