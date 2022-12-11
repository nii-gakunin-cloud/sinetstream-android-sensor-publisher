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

## Data types handled by this application
### Sensor readings available on the device
Basically, this application handles all the sensor types described
in the Android developers document.
[Sensors Overview](https://developer.android.com/guide/topics/sensors/sensors_overview)
Having said that, actually available sensor types depend on the running
conditions such as hardware implementation and Android OS version.
Note also that some privacy-sensitive sensor types such as relating to
biometrics require runtime permissions from user.
In the main screen `Main` of this application, list of actually available
sensor types on the device (excluding the permission denied ones) will
be shown.

### Location of the device
By following the settings screen of this application `Settings -> Sensors -> Location`,
user can optionally add the Android device location to the `device.location`
section of the output JSON data (default: disabled).
This application uses one of following 3 types as the location source.

* [GPS](https://developer.android.com/reference/android/location/LocationManager#GPS_PROVIDER)
* [FUSED](https://developers.google.com/location-context/fused-location-provider)
* FIXED

Note also that appropriate system settings and runtime permissions
of this application are required to handle this kind of information.

### Cellular reception signal strengths
As a means of connectivity, Android device uses a cellular network.
By following the settings screen of this application `Settings -> Sensors -> Cellular`,
user can optionally add the connecting network type (4G, 5G,...)
and reception signal strengths to the `device.cellular` section
of the output JSON data (default: disabled).

Note also that appropriate system settings and runtime permissions
of this application are required to handle this kind of information.

### User info
This application works as a sender side (`Writer`) in the
`Pub/Sub messaging model`, and it sends messages to the peer`Broker`
by specifying a `topic`.
Since the `Broker` handles each message on `topic` basis, there may
be cases that each `Writer` sharing the same `topic` needs to be
distinguished somehow.

By following the settings screen of this application `Settings -> User Data`,
user can optionally add the `Writer` information to the `device.userinfo`
section of the output JSON data (default: disabled).


## Operating Environment

* Android 8.0 (API level 26) or higher
* A server which hosts `Broker` functionality
* IP reachability between Android and the server


## How to use

See section `TUTORIAL - STEP2: Run sensor information collecor
(sinetstream-android-sensor-publisher)` in
[Quick Start Guide (Android)](https://www.sinetstream.net/docs/tutorial-android/)
for details.


## Known issues
### 1) The outdated `Paho Mqtt Android` library
From `Android 12` and later, an exception error occurs in the
[Paho Mqtt Android Service](https://github.com/eclipse/paho.mqtt.android)
library, as soon as the `Broker` connection has established.
This is because the library has not maintained for years and does not
follow the changes in latest Android system API.
As a temporally solution, this application uses a locally-fixed
version of the library `pahomqttandroid-bugfix`.

### 2) Glitches in the collective operation in the `Main` screen
The `Select all sensor types` button to check/uncheck all sensor
types has added in the `Main` screen for convenience.
However, while scrolling up/down the sensor types list, weird
behavior randomly occurs some time.
Since the internal operation works as expected, this issue will
take some time until resolved.


## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

