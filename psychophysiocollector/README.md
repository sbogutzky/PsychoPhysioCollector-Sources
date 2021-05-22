# PsychoPhysioCollector: A Smartphone-Based Data Collection App for Psychophysiological Research
Authors: Simon Bogutzky, Jan Christoph Schrader

License: [MIT](https://opensource.org/licenses/MIT)

Version: 2.0.5

Document version: 1.0.9 

Date: 08/04/2016

[![DOI](https://zenodo.org/badge/23671/sbogutzky/PsychoPhysioCollector.svg)](https://zenodo.org/badge/latestdoi/23671/sbogutzky/PsychoPhysioCollector) [![status](http://joss.theoj.org/papers/aacbdea63ce8d4896a3c84d89f4c5ee0/status.svg)](http://joss.theoj.org/papers/aacbdea63ce8d4896a3c84d89f4c5ee0)

## What is the PsychoPhysioCollector for Android?
The PsychoPhysioCollector (PPC) is an App for Android OS to collect physiological, kinematical data by internal and external sensors and subjective data by questionnaires. It supports:

* Zephyr BioHarness 3
* Shimmer R2 inertial measurement units (IMU)s with Shimmer ECG-Modul and Shimmer Gyro-Modul

Additionally it provides questionnaires that can show up at the end of a session and optionally on certain time intervals.

## Using the PsychoPhysioCollector

### Supported Devices
* Android Devices with API 17 (4.2)+

### Supported Features
* Collect Data from internal sensors (Accelerometer, Gyroscope, Magnetometer, Linear Accelerometer, GPS)
* Collect Data from Zephyr BioHarness 3
   * RR-Interval and ECG-Data implemented
   * Accelerometer and Breathing available
* Collect Data from Shimmer R2 IMUs with Shimmer ECG-Modul and Shimmer Gyro-Modul
* Export collected data in seperated files with synchronized timestamps
* Display questionnaires that can be displayed at the end of a session and optionally on certain time intervals
* Display data of the Shimmer IMUs in realtime for checking the setup

### Documentation
Start the App and enable the Bluetooth. Open the option menu, search and add external sensors (see supported sensors). After establishing a connection by tapping on 'Connect Sensors', you are able to configure each sensor by tapping in the table activity. Use 'Settings' in the option menu to add the name of the participant, the name of the activity and to choose a questionnaire. In the 'Settings' you are able to configure also an interval contingent protocol with variable intervals from five to 60 minutes and interval variance from zero to 180 seconds. BEFORE equipping your participant with the Smartphone and start the session you can check the data of the Shimmer IMUs visually on the Smartphone by tapping on the table activity and choosing 'Show Graph'. Tap in option menu on 'Start Session' to start a session. If an interval contingent protocol is configured, the participant is prompted to answer a questionnaire based on the configured interval. By tapping and by the keyboard input the participant is able to answer the questions. Tap 'Stop Session' to stop the data collection. A last questionnaire will always displayed. Use another App or the android monitor to get the data of the Android file system (see psychophysiocollector/ACTIVITY_NAME/PARTICIPANT_NAME).

### Installation Instructions
Get the [latest version](https://github.com/sbogutzky/PsychoPhysioCollector/releases/latest) (2.0.5). It has only one questionnaire -- the Flow-Short-Scale by Rheinberg et al. (2003). 

If you are familiar with importing and running a project via [Android Studio](https://developer.android.com/studio/index.html) on your Smartphone, you can use the following API to create your own questionnaires in JSON in the folders assets/questionnaires/LOCALISATION_CODE/ (e.g. en or de). 

1. Import the project in Android Studio
2. Grandle will setup all dependencies
3. After that, you can create questionnaires
4. Connect your Smartphone and run the PPC on it

#### Questionnaire Types
* Rating

```json
{
   "type": "rating",
   "stars": 7,
   "question": "The Question.",
   "ratings": ["Low", "High"]
}
```

* Hidden (will not be displayed but appears in output file with "N/A")

```json
{
   "type": "hidden"
}
```

* Text

```json
{
   "type": "text",
   "question": "The Question."
}
```

* True/False

```json
{
   "type": "truefalse",
   "question": "The Question."
}
```

### Example Usage
The pilot deployment was successfully used in the research project Flow-Machines ("Flow-Machines: Body Movement and Sound", 2012-2015) at the University of Applied Sciences Bremen and funded by German Federal Ministry of Education and Research (BMBF; Förderkennzeichen: 03FH084PX2).

### Used Libraries
The Zephyr Development Tools have been used that can be found on [their website](http://www.zephyranywhere.com/zephyr-labs/development-tools).
Also the Shimmer Android driver has been used which is available on [their website](http://www.shimmersensing.com/shop/shimmer-android-id).

## Author and Contribution
As by the License this is free software released by the University of Applied Sciences Bremen. The authors (Simon Bogutzky and Jan Christoph Schrader) welcome external contributors to freely use and extend this software. If you need some help, please write an [issue](https://github.com/sbogutzky/PsychoPhysioCollector/issues). 

## Acknowledgement
This work is part of the research project Flow-Machines ("Flow-Machines: Body Movement and Sound", 2012-2015) at the University of Applied Sciences Bremen and funded by German Federal Ministry of Education and Research (BMBF; Förderkennzeichen: 03FH084PX2).
