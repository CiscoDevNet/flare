/* global module:false */
module.exports = function(grunt) {
    var port = grunt.option('port') || 8006;
    var base = grunt.option('base') || '.';
    var liveReloadPort = grunt.option('lrport') || true;

    grunt.initConfig({
        pkg: grunt.file.readJSON('package-version.json'),

        less: {
            dist: {
                files: [
                    {
                        expand: true,
                        cwd: 'less',
                        src: '*.less',
                        dest: 'css/generated',
                        ext: '.css'
                    },
                    {
                      expand: true,
                      cwd: 'less/themes',
                      src: '*.less',
                      dest: 'css/generated/themes',
                      ext: '.css'
                    }
                ]
            }
        },

        cssmin: {
            compress: {
                files: {
                  'css/generated/default.min.css': [ 'css/generated/default.css' ]
                }
            }
        },

        jshint: {
            options: {
                curly: false,
                eqeqeq: true,
                immed: true,
                latedef: true,
                newcap: true,
                noarg: true,
                sub: true,
                undef: true,
                eqnull: true,
                laxbreak:true,
                browser: true,
                expr: true,
                globals: {
                  console: false,
                  angular: false,
                  io: false,
                  $: false,
                  TweenLite: false,
                  TimelineLite: false,
                  TweenMax: false,
                  TimelineMax: false,
                  Linear: false,
                  Power0: false,
                  Power1: false,
                  Power2: false,
                  Power3: false,
                  Power4: false,
                  d3: false,
                  enableModalView: false,
                  healthStatus: false,
                  showMetricUpdateAnimation: false,
                  Constants: true,
                  Clipboard: false
                }
            },
            files: ['Gruntfile.js', 'js/**/*.js']
        },

        jscs: {
          src: ['js/*.js', 'js/*/*.js'],
          options: {
            config: ".jscsrc",
            esnext: true, // If you use ES6 http://jscs.info/overview.html#esnext
            verbose: true, // If you need output with rule names http://jscs.info/overview.html#verbose
            fix: true // Autofix code style violations when possible.
          }
        },

        connect: {
            server: {
                options: {
                    port: port,
                    base: base,
                    livereload: liveReloadPort,
                    open: true,
                    useAvailablePort: true
                }
            }
        },

        compress: {
            main: {
                options: {
                    mode: 'tgz',
                    archive: '<%= pkg.name %>-<%= pkg.version %>.tar.gz'
                },
                expand: true,
                src: ['MANIFEST', 'package.json', 'conf/*.json', '*.html', 'css/**', 'fonts/**', 'js/**', 'partials/**', 'help/*.json'],
                dest: '<%= pkg.name %>-<%= pkg.version %>'
            }
        },

        watch: {
            options: {
                livereload: liveReloadPort
            },
            js: {
                files: ['Gruntfile.js', 'js/**/*.js'],
                tasks: 'js',
                options: {
                  spawn: false
                }
            },
            less: {
                files: ['less/*.less', 'less/*/*.less'],
                tasks: 'less'
            },
            css: {
                files: ['css/*.css', 'css/generated/**.css']
            },
            html: {
                files: ['*.html', 'partials/**']
            }
        }
    });

    // Load dependencies
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-connect');
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-jscs');
    grunt.loadNpmTasks('grunt-contrib-compress');

    grunt.registerTask('default', ['css', 'js', 'jshint']);
    grunt.registerTask('js', ['jshint', 'jscs']);
    grunt.registerTask('css', ['less', 'cssmin']);
    grunt.registerTask('package', ['default', 'compress']);
    grunt.registerTask('serve', ['connect', 'watch']);
};
