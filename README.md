# Capsule Daemon

A [caplet](https://github.com/puniverse/capsule#what-are-caplets) that runs a [capsule](https://github.com/puniverse/capsule) as a Unix service through [jsvc](http://commons.apache.org/proper/commons-daemon/jsvc.html) and as a Windows service through [procrun](http://commons.apache.org/proper/commons-daemon/procrun.html).

## Requirements

In addition to [Capsule's](https://github.com/puniverse/capsule), only if the platform is not Windows, Linux 64 bit nor Mac OS X then [jsvc](http://commons.apache.org/proper/commons-daemon/jsvc.html) must be correctly installed locally.

## Usage

The Gradle-style dependency you need to embed in your Capsule JAR, which you can generate with the tool you prefer (f.e. with plain Maven/Gradle as in [Photon](https://github.com/puniverse/photon) and [`capsule-gui-demo`](https://github.com/puniverse/capsule-gui-demo) or higher-level [Capsule build plugins](https://github.com/puniverse/capsule#build-tool-plugins)), is `co.paralleluniverse:capsule-daemon:0.1.0`. Also include the caplet class in your Capsule manifest, for example:

``` gradle
    Caplets: MavenCapsule DaemonCapsule
```

`capsule-daemon` can also be run as a wrapper capsule without embedding it:

``` bash
$ java -Dcapsule.log=verbose -jar capsule-daemon-0.1.0.jar my-capsule.jar my-capsule-arg1 ...
```

It can be both run against (or embedded in) plain (e.g. "fat") capsules and [Maven-based](https://github.com/puniverse/capsule-maven) ones.

## Additional Capsule manifest entries

The following additional manifest entries and system properties can be used to customize `capsule-daemon`'s behaviour (see the [jsvc docs](http://commons.apache.org/proper/commons-daemon/jsvc.html) and the [procrun docs](http://commons.apache.org/proper/commons-daemon/procrun.html) for further details):

 - Both Unix and Windows:
   - Manifest entries:
     - `Daemon-Start-Class`: class containing the `start` method (default: app's main).
     - `Daemon-Start-Method`: static `String[] -> void` service start method short name run as the specified, if any (default: app's main).
     - `Daemon-Stop-Class`: class containing the `stop` method (default: none).
     - `Daemon-Stop-Method`: static `String[] -> void` service stop method short name run as the specified, if any (default: none).
     - `Daemon-User`: the username under which the service will run. The `capsule.daemon.user` system property can override it.
     - `Daemon-Cwd`: working directory of start/stop (default: `/` on Unix). The `capsule.daemon.cwd` system property can override it.
     - `Daemon-Stdout-File`: stdout (default: `/dev/null` on Unix, `<logpath>/service-stdout.YEAR-MONTH-DAY.log` on Windows). The `capsule.daemon.stdoutFile` system property can override it.
     - `Daemon-Stderr-File`: stdout (default: `/dev/null` on Unix, `<logpath>/service-stderr.YEAR-MONTH-DAY.log` on Windows). . The `capsule.daemon.stderrFile` system property can override it.
     - `Daemon-PID-File`: PID file (default: `/var/run/<appid>.pid` on Unix, `<logpath>/<appid>.pid` on Windows). The `capsule.daemon.pidFile` system property can override it.
   - System properties:
     - `capsule.daemon.stop`: if `true` or barely present will stop a running service rather than starting one.
 - Only Unix:
   - System properties:
     - `capsule.daemon.checkOnly`: `jsvc` check run, won't start the service.
     - `capsule.daemon.debug`: turn on debug `jsvc` logging.
     - `capsule.daemon.verbose`: turn on verbose `jsvc` logging.
     - `capsule.daemon.jsvc`: specifies the pathname of a system-installed `jsvc` command to be used instead of the one provided by `capsule-daemon`.
   - Manifest entries:
     - `Init-Class`: class containing the `init` method (default: none).
     - `Init-Method`: static `String[] -> String[]` service initialization method, it will be run as `root`; the return value will be passed to the `Start` method (default: none).
     - `Destroy-Class`: class containing the `destroy` method (default: none).
     - `Destroy-Method`: static `void -> void` cleanup method, it will be run as `root` (default: none).
     - `No-Detach`: don't detach from the parent process. The `capsule.daemon.noDetach` system property can override it.
     - `Keep-Stdin`: don't redirect the standard input to `/dev/null`. The `capsule.daemon.keepStdin` system property can override it.
     - `Wait-Secs`: Wait seconds for service readiness, must be multiple of 10. The `capsule.daemon.waitSecs` system property can override it.
 - Only Windows
   - `Daemon-Password`: the password of the user under which the service will run (default: none). The `capsule.daemon.password` system property can override it.
   - `Daemon-Java-Exec-User`: the password of the user that will execute the final Java process (default: none). The `capsule.daemon.javaExecUser` system property can override it.
   - `Daemon-Java-Exec-Password`: the password of the user that will execute the final Java process (default: none). The `capsule.daemon.javaExecPassword` system property can override it.
   - `Daemon-Service-Name`: the service internal name (default: app ID). The `capsule.daemon.serviceName` system property can override it.
   - `Daemon-Display-Name`: the service display name (default: app ID). The `capsule.daemon.displayName` system property can override it.
   - `Daemon-Description`: the service description (default: app ID). The `capsule.daemon.description` system property can override it.
   - `Daemon-Startup`: the service startup mode, either `auto` or `manual` (default: `manual`). The `capsule.daemon.startup` system property can override it.
   - `Daemon-Type`: the service type, it can be `interactive` (default: none). The `capsule.daemon.type` system property can override it.
   - `Daemon-DependsOn`: the list of service dependencies (default: none). The `capsule.daemon.dependsOn` system property can override it.
   - `Daemon-Stop-Params`: the list of service stop parameters (default: none). The `capsule.daemon.stopParams` system property can override it.
   - `Daemon-Stop-Timeout`: service stop timeout in seconds (default: none). The `capsule.daemon.stopTimeout` system property can override it.
   - `Daemon-Log-Path`: the log path (default: `%SystemRoot%\System32\LogFiles\Apache`). The `capsule.daemon.logPath` system property can override it.
   - `Daemon-Log-Prefix`: the log prefix (default: app ID). The `capsule.daemon.logPrefix` system property can override it.
   - `Daemon-Log-Level`: the log level between `error`, `info`, `warn` and `debug` (default: `info`). The `capsule.daemon.logLevel` system property can override it.

## Notes

* `jsvc` with default settings (due to the default PID file location) and `procrun` in any case (for service installation, uninstallation and upgrade) require resp. `root` and administrative privileges.
* Launch, Java and service execution users must all be able to access the same Capsule's cache directory. You can set it to a commonly accessible location (for example in `/tmp/capsule`) through the `CAPSULE_CACHE_DIR` environment variable.

## License

    Copyright (c) 2015, Parallel Universe Software Co. and Contributors. All rights reserved.

    This program and the accompanying materials are licensed under the terms
    of the Eclipse Public License v1.0 as published by the Eclipse Foundation.

        http://www.eclipse.org/legal/epl-v10.html
