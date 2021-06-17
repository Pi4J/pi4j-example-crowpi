# Pi4J V2 :: CrowPi Examples

[![Code Build Status](https://img.shields.io/github/workflow/status/Pi4J/pi4j-example-crowpi/CrowPi%20CI?label=code)](https://github.com/Pi4J/pi4j-example-crowpi/actions/workflows/crowpi.yml)
[![Docs Build Status](https://img.shields.io/github/checks-status/Pi4J/pi4j-example-crowpi/gh-pages?label=docs)](https://pi4j.com/pi4j-example-crowpi/)
[![Contributors](https://img.shields.io/github/contributors/Pi4J/pi4j-example-crowpi)](https://github.com/ppmathis/fhnw-crowpi/graphs/contributors)
[![License](https://img.shields.io/github/license/Pi4J/pi4j-example-crowpi)](https://github.com/Pi4J/pi4j-example-crowpi/blob/main/LICENSE)

This project contains both example applications and ready-made component classes for interacting with the
[CrowPi](https://www.elecrow.com/crowpi-compact-raspberry-pi-educational-kit.html) using the Pi4J (V2) library. You can easily get started
with electronics programming by testing and modifying the bundled examples or even write your own application.

## COMPONENTS

The provided component classes as part of this library provide an implementation for every available component of the CrowPi. The following
table provides an overview of all supported components with a link to their implementation and example app:

| **Component** | **Example App** | **Implementation** |
|---|---|---|
| Button | [ButtonApp.java](src/main/java/com/pi4j/crowpi/applications/ButtonApp.java) | [ButtonComponent.java](src/main/java/com/pi4j/crowpi/components/ButtonComponent.java) |
| ButtonMatrix | [ButtonApp.java](src/main/java/com/pi4j/crowpi/applications/ButtonApp.java) | [ButtonComponent.java](src/main/java/com/pi4j/crowpi/components/ButtonMatrixComponent.java) |
| Buzzer | [BuzzerApp.java](src/main/java/com/pi4j/crowpi/applications/BuzzerApp.java) | [BuzzerComponent.java](src/main/java/com/pi4j/crowpi/components/BuzzerComponent.java) |
| IR Receiver | [IrReceiverApp.java](src/main/java/com/pi4j/crowpi/applications/IrReceiverApp.java) | [IrReceiverComponent.java](src/main/java/com/pi4j/crowpi/components/IrReceiverComponent.java) |
| LCD Display | [IrReceiverApp.java](src/main/java/com/pi4j/crowpi/applications/IrReceiverApp.java) | [IrReceiverComponent.java](src/main/java/com/pi4j/crowpi/components/LcdDisplayComponent.java) |
| LED Matrix | [IrReceiverApp.java](src/main/java/com/pi4j/crowpi/applications/IrReceiverApp.java) | [IrReceiverComponent.java](src/main/java/com/pi4j/crowpi/components/LcdDisplayComponent.java) |
| Light Sensor | [LightSensorApp.java](src/main/java/com/pi4j/crowpi/applications/LightSensorApp.java) | [LightSensorComponent.java](src/main/java/com/pi4j/crowpi/components/LightSensorComponent.java) |
| PIR Motion Sensor | [PirMotionSensorApp.java](src/main/java/com/pi4j/crowpi/applications/PirMotionSensorApp.java) | [PirMotionSensorComponent.java](src/main/java/com/pi4j/crowpi/components/PirMotionSensorComponent.java) |
| Relay | [RelayApp.java](src/main/java/com/pi4j/crowpi/applications/RelayApp.java) | [RelayComponent.java](src/main/java/com/pi4j/crowpi/components/RelayComponent.java) |
| RFID | [RfidApp.java](src/main/java/com/pi4j/crowpi/applications/RfidApp.java) | [RfidComponent.java](src/main/java/com/pi4j/crowpi/components/RfidComponent.java) |
| Servo Motor | [ServoMotorApp.java](src/main/java/com/pi4j/crowpi/applications/ServoMotorApp.java) | [ServoMotorComponent.java](src/main/java/com/pi4j/crowpi/components/ServoMotorComponent.java) |
| Seven Segment Display | [SevenSegmentApp.java](src/main/java/com/pi4j/crowpi/applications/SevenSegmentApp.java) | [SevenSegmentComponent.java](src/main/java/com/pi4j/crowpi/components/SevenSegmentComponent.java) |
| Step Motor | [StepMotorApp.java](src/main/java/com/pi4j/crowpi/applications/StepMotorApp.java) | [StepMotorComponent.java](src/main/java/com/pi4j/crowpi/components/StepMotorComponent.java) |
| Tilt Sensor | [StepMotorApp.java](src/main/java/com/pi4j/crowpi/applications/StepMotorApp.java) | [StepMotorComponent.java](src/main/java/com/pi4j/crowpi/components/TiltSensorComponent.java) |
| Touch Sensor | [TouchSensorApp.java](src/main/java/com/pi4j/crowpi/applications/TouchSensorApp.java) | [TouchSensorComponent.java](src/main/java/com/pi4j/crowpi/components/TouchSensorComponent.java) |
| Ultrasonic Distance Sensor | [UltrasonicDistanceSensorApp.java](src/main/java/com/pi4j/crowpi/applications/UltrasonicDistanceSensorApp.java) | [UltrasonicDistanceSensorComponent.java](src/main/java/com/pi4j/crowpi/components/UltrasonicDistanceSensorComponent.java) |
| Vibration Motor | [VibrationMotorApp.java](src/main/java/com/pi4j/crowpi/applications/VibrationMotorApp.java) | [VibrationMotorComponent.java](src/main/java/com/pi4j/crowpi/components/VibrationMotorComponent.java) |

## CUSTOM OS IMAGE

Each [tagged GitHub release](https://github.com/Pi4J/pi4j-example-crowpi/releases/latest) provides a pre-built version of the custom OS
image called "CrowPi OS". It is based on the official "Raspberry Pi OS 2021-03-25" and automatically built using Packer. Using this image
provides the following set of benefits:

- Preconfigured locale (en_US), keyboard (US) and timezone (Europe/Zurich)
- Preconfigured wireless country (Switzerland) by default
- Remote management via SSH and VNC possible without configuration
- Preinstalled OpenJDK 11 with Gluon JavaFX to get quickly started
- Preconfigured `/boot/config.txt` which supports all components out of the box
- Dynamic wallpaper which shows Ethernet/WLAN address and hostname
- Comes with `lirc` preinstalled to run the IR receiver component

You may download the zip-compressed archive `crowpi.img.zip`, extract it and flash it with the imaging tool of your choice to get started.
The default installation provides an user account `pi` with the password `crowpi` and sudo privileges.

## FRAMEWORK

To simplify adding and launching new applications, a custom launcher has been built using PicoCLI. The [Launcher.java](src/main/java/com/pi4j/crowpi/Launcher.java)
class contains a static list of available targets called `APPLICATIONS` which has to be adjusted when adding new applications to the 
project.

By default, an interactive menu gets shown which allows selecting a single target to launch. After executing this target, the 
application will automatically end. You may optionally specify the name of an application as the first argument, i.e. `BuzzerApp`, to 
directly launch this specific application.

If you want to comfortably test all supported components at once, you may specify the flag `--demo` which will return to the interactive 
launcher menu once a target has been executed.

Creating your own applications is as simple as implementing the provided [Application.java](src/main/java/com/pi4j/crowpi/Application.java)
interface, which only requires a single `void execute(Context pi4j)` method.

## BUILD SYSTEM

This project uses Maven for building, testing and running the various applications. While it can be used directly on a Raspberry Pi /
CrowPi, it also supports compiling everything together locally, then pushing the artifacts to the device and running them remotely. The
build system defaults to local deployments, but the following set of Maven properties can be set for remote deployments:

- **`crowpi.remote.host` (required):** Current IP address or DNS name of the CrowPi, e.g. `192.168.1.2`, used for SCP/SSH
- **`crowpi.remote.port` (optional):** Port to use for SCP/SSH communication, defaults to `22`
- **`crowpi.remote.username` (optional):** Username to use for SCP/SSH, defaults to `pi`
- **`crowpi.remote.password` (optional):** Password to use for SCP/SSH, defaults to `crowpi`
- **`crowpi.remote.target` (optional):** Default directory to temporarily store built artifacts, defaults to `/home/pi/deploy`
- **`crowpi.remote.jvmOptions` (optional):** Additional JVM options, defaults to an empty string

In case of a remote deployment, the artifacts get pushed via SCP and will be automatically executed using SSH. Please note that any 
existing files in the deployment folder are being automatically overwritten.

## RUNTIME DEPENDENCIES

This project has the following runtime dependency requirements:

- [**Pi4J V2**](https://pi4j.com/)
- [**SLF4J (API)**](https://www.slf4j.org/)
- [**SLF4J-SIMPLE**](https://www.slf4j.org/)
- [**PIGPIO Library**](http://abyz.me.uk/rpi/pigpio) (for the Raspberry Pi)

## LICENSE

This repository is licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
limitations under the License.
