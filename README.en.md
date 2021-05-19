<!--
Copyright (C) 2020-2021 National Institute of Informatics

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

[日本語](README.md)

## Overview

This application implements the `Writer` functionality, which
refers to `Publisher` client defined in Pub/Sub messaging model.  
Most of Android hardware has several built-in sensor devices.
Once user enables these sensors via GUI, readout values will be
notified from
[SINETStreamHelper](https://www.sinetstream.net/docs/userguide/libhelper.html)
library as a JSON data. Then it will be sent as a
`SINETStream message` to the peer `Broker` via
[SINETStream for Android](https://www.sinetstream.net/docs/userguide/android.html)
library.  
On the backend server side, the received message content will be
stored and analyzed accordingly, for the purpose of data
visualization etc.

```
     Android Application
    +---------------------------------+
    |  +--------+ JSON +-----------+  |
    |  | Writer | <----| libhelper |  |
    |  +--------+      +-----------+  |
    |      |                A         |
    |      V                | Raw data|
    |  +-------------+  +--------+    |
    |  | SINETStream |  | Sensor |    |
    |  | for Android |  | Devices|    |                    Backend
    |  +-------------+  +--------+    |                    System
    +------|--------------------------+                   +----------+
           |                             (         )      |          |
           |                             (         )      |          |
           |                             ( Network )      | +------+ |
           +---------------------------->(         )----->| |Broker| |
               [message]                 (         )      | +------+ |
                                                          +----------+
```

## Operating Environment

* Android 8.0 (API level 26) or higher
* A server which hosts `Broker` functionality
* IP reacherbility between Android and the server


## How to use

See section `TUTORIAL - STEP2: Run sensor information collecor
(sinetstream-android-sensor-publisher)` in
[Quick Start Guide (Android)](https://www.sinetstream.net/docs/tutorial-android/)
for details.

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

