**A pure JAVA H264 Decoder ported from FFmpeg (libavcodec) library.**

Everything is re-implemented in pure JAVA language.
**h264j** can decode **Baseline / Main / Extend / High profile**.

**To run the project**, use:

java com.twilight.h264.player.H264Player _<H264 Raw File>_

You can use sample H264 Raw files in /sample\_clips directory.


---

Decoder source code, those are ported from FFmpeg, are in package com.twilight.h264.decoder.

Package com.twilight.h264.util provides utility classes for JAVA system (and some for J2ME).

To use decoder, please see example in class com.twilight.h264.player.H264Player.

Enjoy :)

Chulayuth A.


---

_I would like to say thank you to Stan (from jcodec project - jcodec.googlecode.com) as his work has been inspiring me a lot in implementing pure JAVA library for decoding H264 High Profile movies_.