**A pure JAVA H264 Decoder ported from FFmpeg (libavcodec) library.**

Everything is re-implemented in pure JAVA language.
**h264j** can decode **Baseline / Main / Extend / High profile**.

---

**To build the project**,
```
cd h264j
mvn package
```
Your built JAR file should be in target directory

---

**To run the project**,

Use JAVA to run com.twilight.h264.player.H264Player with parameter H264 Raw File

You can use sample H264 Raw files in /h264j/sample\_clips directory.

>Example
```
java -cp h264j-1.0-SNAPSHOT.jar com.twilight.h264.player.H264Player ../../h264j/sample_clips/admiral.264
```

---

Decoder source code, those are ported from FFmpeg, are in package com.twilight.h264.decoder.

Package com.twilight.h264.util provides utility classes for JAVA system (and some for J2ME).

To use decoder, please see example in class com.twilight.h264.player.H264Player.


Enjoy :)


Chulayuth A.
