# Capsule Daemon

A [caplet](https://github.com/puniverse/capsule#what-are-caplets) that wraps a [capsule](https://github.com/puniverse/capsule) wrapped as an Unix [jsvc](http://commons.apache.org/proper/commons-daemon/jsvc.html) service.

TODO procrun

## Requirements

In addition to [Capsule's](https://github.com/puniverse/capsule):

  * [jsvc](http://commons.apache.org/proper/commons-daemon/jsvc.html) correctly installed.

TODO procrun

## Usage

The Gradle-style dependency you need to embed in your Capsule JAR, which you can generate with the tool you prefer (f.e. with plain Maven/Gradle as in [Photon](https://github.com/puniverse/photon) and [`capsule-gui-demo`](https://github.com/puniverse/capsule-gui-demo) or higher-level [Capsule build plugins](https://github.com/puniverse/capsule#build-tool-plugins)), is `co.paralleluniverse:capsule-daemon:0.1.0`. Also include the caplet class in your Capsule manifest, for example:

``` gradle
    Caplets: MavenCapsule DaemonCapsule
```

`capsule-osv` can also be run as a wrapper capsule without embedding it:

``` bash
$ java -Dcapsule.log=verbose -jar capsule-daemon-0.1.0.jar my-capsule.jar my-capsule-arg1 ...
```

It can be both run against (or embedded in) plain (e.g. "fat") capsules and [Maven-based](https://github.com/puniverse/capsule-maven) ones.

## Additional Capsule manifest entries

The following additional manifest entries can be used (see the [jsvc docs](http://commons.apache.org/proper/commons-daemon/jsvc.html) for further details):

TODO list

## License

    Copyright (c) 2015, Parallel Universe Software Co. and Contributors. All rights reserved.

    This program and the accompanying materials are licensed under the terms
    of the Eclipse Public License v1.0 as published by the Eclipse Foundation.

        http://www.eclipse.org/legal/epl-v10.html
