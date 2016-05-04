var instanceName = "qos-agent";

var gulp = require('gulp');
var exec = require('child_process').exec;
var shell = require('gulp-shell');
var util = require('gulp-util');

var spawn = require('child_process').spawnSync;

// Task and dependencies to distribute for all environments;
var babel = require('babelify');
var browserify = require('browserify');
var source = require('vinyl-source-stream');
var exec = require('child_process');

// Gulp task to generate development documentation;
gulp.task('doc_alt', function (done) {

	console.log('Generating documentation... (this exec stuff does not really work)');
	exec('node_modules/.bin/jsdoc -R readme.md -d docs src/*', function (err, stdout, stderr) {
		if (err) return done(err);
		console.log('Documentation generated in "docs" directory');
		done();
	});

});

gulp.task('build', function () {
	var stubBundler = browserify(['./src/main.js'], {
		debug: false
	}).
	exclude('express').
	exclude('requestify').
	exclude('microtime').
	exclude('http').
	exclude('process').
	exclude('body-parser').
	exclude('telnet-client').
	exclude('fs').
	transform(babel);

	function rebundle() {
		stubBundler.bundle()
			.on('error', function (err) {
				console.error(err);
				this.emit('end');
			}).
			pipe(source('qosbroker-agent.js')).
			pipe(gulp.dest('./app'));
	}
	rebundle();
});

gulp.task('docker-image', ['dist'], function () {

	console.log('\nPlease check your configuration (./config/) files, otherwise it may not functioning.\n');
  console.log('Building instance...');

	execCmd('docker', 'build -t rethink/' + instanceName + ' ./app/');

});

gulp.task('docker-run', ['docker-image'], function () {

	execCmd('docker', 'stop ' + instanceName);
	execCmd('docker', 'rm ' + instanceName);
	execCmd('docker', 'run -t -d --net="host" --name="' + instanceName + '" rethink/' + instanceName);
	
});

gulp.task('dist', ['build'], shell.task(
	[
		'mkdir -p app',
		'cp Dockerfile ./app/Dockerfile',
		'cp package.json ./app/package.json',
		'cp src/bash/launchagent.sh ./app/launchagent.sh',
		'cp -r ./config ./app/',
		'sleep 1'
	]
));

gulp.task('start', ['dist'], shell.task([
	'sleep 1',
	'cd app && node qosbroker-agent.js'
]));

gulp.task('default', ['help'], shell.task([]));

gulp.task('help', function () {
	console.log('\nThe following gulp tasks are available:\n');
	console.log('gulp' + ' ' + 'help\t\t' + '# show this help\n');
	console.log('gulp' + ' ' + 'build\t\t' + '# transpile and bundle the Qos Broker Agent Sources\n');
	console.log('gulp' + ' ' + 'dist\t\t' + '# creates app folder with transpiled code (depends on build)\n');
	console.log('gulp' + ' ' + 'docker-image\t\t' + '# creates a docker image with the built code. Please update your ./config/ files!\n');
	console.log('gulp' + ' ' + 'docker-run\t\t' + '# creates a docker image with the built code. Please update your ./config/ files!\n');
	console.log('gulp' + ' ' + 'start\t\t' + '# starts the QoSBroker Agent from dist folder (depends on dist)\n');
});

function execCmd(cmd, param) {
	var params = param.split(" ");
	var command = spawn(cmd, params);
	console.log(command.stdout.toString());
	console.log(command.stderr.toString());
}
