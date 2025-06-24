Eve Rat Timer Tracker

A lightweight, always-on-top overlay for tracking multiple ratting belt timers in EVE Online. The transparent GUI is fully draggable and resizable.

Features

Multiple Belt Timers: Configurable number of belt panels (1–20).

Per-Belt Stopwatches: Each panel shows an elapsed-time clock and a Reset button.

Transparent Overlay: Adjustable opacity via a slider.

Drag & Move: Click-and-drag handles on top and bottom to reposition.

Resizable: Bottom-right resize grip to adjust window size.

Persistent Config: Saves window position, size, opacity, and belt count in config.properties.



Getting Started

Prerequisites

Java 11 or higher installed.

(Optional) Eclipse or other Java IDE for building from source.

Building

Clone the repository:

git clone https://github.com/<tndmadman/evebelttimer.git
cd evebelttimer

Build a runnable JAR using Gradle or Eclipse:

With Gradle (if you add build.gradle):

./gradlew clean shadowJar

The JAR will be in build/libs/evebelttimer.jar.

With Eclipse:

Import the project as a Java project.

Right-click the project → Export → Runnable JAR file.

Choose com.evetimer.App as the main class and bundle required libraries.

(Optional) Wrap the JAR into a Windows executable with Launch4j.

Running

Double-click the generated JAR (or EXE) to launch. If Java isn’t associated with JAR files, run from command line:

java -jar evebelttimer.jar

Usage

Set Belt Count: Choose how many timers you need (1–20).

Adjust Opacity: Use the slider to change transparency.

Start Timers: Each belt panel starts counting automatically.

Reset a Timer: Click its Reset button after a belt clear.

Move the Window: Drag by the top or bottom drag handles.

Resize Window: Use the bottom-right grip.

Configuration

Stored in config.properties alongside the JAR:

x=100
y=100
width=400
height=200
beltCount=5
opacity=0.85

Modify or delete this file to reset settings.

Contributing

Fork the repo.

Create a feature branch.

Submit a pull request.

HASH FOR EvETimer1.1a.jar 1759d410f30168c5de6567ccb14bce6561edccc077606288e11dc687d5f7981a