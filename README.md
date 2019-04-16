Matrix client built for desktop platforms based on the [koma library](https://github.com/koma-im/koma-library)

[![Build Status](https://travis-ci.org/koma-im/continuum-desktop.svg?branch=master)](https://travis-ci.org/koma-im/continuum-desktop)

# Introduction

![screenshot](https://continuum.link/screenshots/primary-preview.png)

* designed to bridge existing chat apps, so you can switch to an open platform _at now_, thanks to the [Matrix](https://matrix.org/) project:
  * matrix.org provides bridge for Gitter, Slack, the whole freenode IRC network
  * There are projects providing support for almost all platforms such as xmpp and [Telegram](https://github.com/tulir/mautrix-telegram)
  * bridges are well-integrated, users on other platforms appear as individual users with avatar here
* Multimedia capability
  * Links in plaintext messages are identified and preview is shown whenever applicable
  * Known services are treated in the most reasonable way, for a Github repo, this could be the number of stars, forks, the README.md
  * Known media type are auto-loaded, image links are converted to images

* Display Emoji on all OS using [Emoji One](https://github.com/emojione/emojione/)
  * Built-in virtual emoji keyboard

* Fetch chat history to allow you to read early messages

* Store all data to disk. So it starts very fast and previous chats appear instantly. Incremental sync puts less stress on servers and also make sync much faster

* GUI created using [tornadofx](https://github.com/edvin/tornadofx), uses much less resouces than packaged html and js

# Download and run

The Kotlin code currently runs on JVM, make sure you have Java runtime environment not older than version 11.

To make running it as easy as possible,
releases with all dependencies and native modules bundled are
built for Mac and Linux platforms, download the latest version from
[Releases page](https://github.com/koma-im/continuum-desktop/releases).

Double-click on the jar file to launch the program.
Alternatively, you can use the command-line: `java -jar continuum.jar`.

# Usage

Use your matrix ID, such as `@jane:matrix.org` (Support for other forms of username coming soon) and your password to login

If the server allows it, you can also enter a new ID and click register to get an account.

A token will be saved so the next time you login, password doesn't need to be entered.


# Feature development progress

## In progress

- [ ] Support more types of multimedia messages
  - [x] video
  - [ ] audio

- [ ] Parse plain text messages and display some content in a better way. Display known kinds of links in line.
  - [x] Preview links to images
  - [ ] Preview for Github repo

## To be implemented

- [ ] Notifications

- [ ] Tab completion for nicks

- [ ] Internationalization, support multiple languages

- [ ]Remember all positions where the user finished reading previously, to make it easy to read all history messages, without manually finding previous unread messages.

- [ ]Advanced filtering operations based on any combination of keyword, chatroom, user name and more to reduce distraction and help focusing

- [ ]Smart notification to help people focus on what's important and don't get distracted


# Contributing

## Try it

Just try it and when you find anything unhandy, tell us about it.
Your idea will help to focus on the most needed features, and a handy client for Matrix will be a reality sooner.

If you know someone who might be interested in open-source communication, star or share the project, more usage and feedback will always be helpful.

## Source

If you are interested in or have experience with Kotlin or Matrix, feel free to click "Fork" and experiment with the source.
There are a lot of ways to make improvements.

It's a good idea to open an issue before you start working, in order to coordinate work and avoid duplicate work.
