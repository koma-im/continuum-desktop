<a href="https://matrix.org" target="_blank">
<img align="right" width="98" height="41" src="https://continuum.link/images/made-for-matrix.png"></a>

# Continuum

[![Build Status](https://travis-ci.org/koma-im/continuum-desktop.svg?branch=master)](https://travis-ci.org/koma-im/continuum-desktop)

Pure Kotlin Matrix client.

* Contains zero lines of HTML or any form of XML.
  The entire UI is defined in Kotlin.
  Rendering is done using native implementations.

* Features inherited from the [Matrix](https://matrix.org/) platform, such as:
  * Communicate with users of other platforms without falling back to plain text.
    There exist various [bridges](https://matrix.org/bridges) that map rich features to and from Matrix.
  * You can set up your own server to take control of security and availability.

* Use async programming (`kotlinx.coroutines`) for performance, code clarity and multi-platform portability.

* Built on the Kotlin library [koma](https://github.com/koma-im/koma-library).
  Android and iOS versions will share the same library compiled for each platform.
  UI will be recreated using each native SDK.
  Code will be shared as much as is reasonable, but not any more.

* Small things that may come in handy
  * Links in plaintext messages are identified and preview is shown whenever applicable
  * Known media type are auto-loaded, image links are converted to images

* Display Emoji on all OS using [Emoji One](https://github.com/emojione/emojione/)
  * Built-in virtual emoji keyboard

* Fetch chat history to allow you to read early messages

* Store all data to disk. So it starts very fast and previous chats appear instantly. Incremental sync puts less stress on servers and also make sync much faster

* Reasonably sized: smaller than 50MB when packaged as a stand-alone binary.

### Screenshots

![screenshot](https://continuum.link/screenshots/primary-preview.png)

### Download

Builds are provided on the
[Releases page](https://github.com/koma-im/continuum-desktop/releases).

#### Option a: Single binary executable

This is the easiest option, download a single file and launch with one click.

For Linux, download a file with the `.AppImage` extension.

Note that you may need to mark the file as executable.

#### Option b: Executable jar

If you have a
[JRE](https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=openj9)
(version 11 or newer recommended) installed,
Continuum can run on it.
The advantage of this option is smaller package size (about 20MB).


Download a jar file for your platform (the macOS version is marked as `osx`).
Then you can launch Continuum with the command `java -jar ` followed by the jar file.
Or, depending on the set-up, you may be able to launch Continuum with a double-click.


Note that the jar files are built for each platform as they contain natively compiled code.

### Signing in

Use your matrix ID, such as `@jane:matrix.org` (Support for other forms of username coming soon) and your password to login

A token will be saved so the next time you login, password doesn't need to be entered.
