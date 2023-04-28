Solar4Africa Solar Car Project
===============================
One of the solar products S4A subsidizes/provides are solar-powered electric cars. These greatly improve the mobility and therefore the economic productivity of their users. But they are relatively expensive compared to S4A’s other projects and therefore require
significant donor support. To generate this, reliable data needs to be collected to help quantify the impact that
these solar cars have on the welfare of the Malawian public. Until now, the only form of data collection for the cars’ use has come from inconsistent,
irregular and unreliable manual reporting in notebooks kept by the users themselves. The aim of this project is to develop an application that can replace this reliance on user
input and improve upon it. 

The app will:

○ Need to run on cheap android phones (Robert will bring a few when he comes
back from Malawi so that we can test and evaluate which specific models within a
specific price range are the most optimized for the applications’ demands)

○ Need to collect GPS data on the cars’ mileage and journeys, and preferably
create visualizations to help analyze it

○ Take periodic photos of the car’s power meter

○ Need to begin recording data based on the ignition of the car and stop once the
car is switched off

○ Need to convert the 12v of power outputted by the car to the phone’s individual
voltage

○ Need to involve no user involvement in the entire process

○ Assume little to no Wi-Fi connection or 3G signal

Project Task
1. Ensuring the app can acquire GPS location and write to text file with timestamp
2. A) Transfer text file to U.S.-based website and make U.S. based website and B) Transfer
text file to other local phone via wifi or Bluetooth (optional)
3. A) Take periodic photos of power meter and B) use OCR to convert power meter photos
to character data and write to text file
4. The physical mounting of the phone’s stand and lockable casing, within the solar car

For GPS tracking, this project makes use of the Codelab learning repo below:


While-in-use Location Codelab Repository
===============================

This repository is to be used with the while-in-use location codelab:
https://developers.google.com/codelabs/while-in-use-location

It teaches you how to handle new location permissions added in Android 10.

Documentation on while-in-use location:
https://developer.android.com/preview/privacy/device-location

License
-------

Copyright 2019 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
