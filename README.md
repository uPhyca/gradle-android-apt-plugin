[![Build Status](https://travis-ci.org/uPhyca/gradle-android-apt-plugin.png?branch=master)](http://travis-ci.org/uPhyca/gradle-android-apt-plugin)


Gradle Android APT Plugin
==================================

A Gradle plugin which enables annotations processing for Android builds.


Usage
-----

Add the plugin to your `buildscript`'s `dependencies` section:
```groovy
classpath 'com.uphyca.gradle:gradle-android-apt-plugin:0.9.+'
```

Apply the `android-apt` plugin:
```groovy
apply plugin: 'android-apt'
```

Add annotation processors dependencies using the `apt` configuration:
```groovy
compile 'com.squareup.dagger:dagger:1.1.+'
apt 'com.squareup.dagger:dagger-compiler:1.1.+'
```

License
-------

    Copyright 2013 uPhyca, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.