# koma

# Features

* designed to bridge existing chat apps, so you can switch to an open platform _at now_, thanks to the [Matrix](https://matrix.org/) project:
  * matrix.org provides bridge for Gitter, Slack, the whole freenode IRC network
  * There are projects providing support for almost all platforms such as xmpp and [Telegram](https://github.com/tulir/mautrix-telegram)
  * bridges are well-integrated, users on other platforms appear as individual users with avatar here
* Multimedia capability
  * Links in plaintext messages are identified and preview is shown whenever applicable
  * Known services are treated in the most reasonable way, for a Github repo, this could be the number of stars, forks, the README.md
  * Known media type are auto-loaded, image links are converted to images
  * Koma is going to save you quite a few clicks

* Display Emoji on all OS using [Emoji One](https://github.com/emojione/emojione/)
  * Built-in virtual emoji keyboard

* Fetch chat history to allow you to read early messages

* Store all data to disk. So it starts very fast and previous chats appear instantly. Incremental sync puts less stress on servers and also make sync much faster

* GUI created using [tornadofx](https://github.com/edvin/tornadofx), uses much less resouces than packaged html and js

# Getting the application

## Requirements

Java runtime version 9 or higher.

##### Notes for Linux users

JavaFx 9 may not be included if Java is installed using the package manager.
Usually, the easiest solution is to install Java
from the
[official site](http://www.oracle.com/technetwork/java/javase/downloads/index.html),
which has JavaFx bundled.

## Get a compiled package

[Click to download](https://github.com/koma-im/koma/releases/download/0.7.3/koma-0.7.3-standalone.jar)

## Alternatively, use the source

Clone this repo: `git clone https://github.com/koma-im/koma.git`

Build a package using maven: `mvn package`

Find the jar file in the target folder

# Usage

Use your matrix ID, such as `@jane:matrix.org` (Support for other forms of username coming soon) and your password to login

If the server allows it, you can also enter a new ID and click register to get an account.

A token will be saved so the next time you login, password doesn't need to be entered.

![screenshot](https://raw.githubusercontent.com/koma-im/koma/master/koma-preview.png)

# Todo List

Notifications

Tab completion for nicks

Internationalization, support multiple languages

Remember all positions where the user finished reading previously, to make it easy to read all history messages, without manually finding previous unread messages.

Support more types of messages, such as audio and video

Parse plain text messages and display some content in a better way. Display known kinds of links in line, save clicks. Such as replace links to images with actual images, use webview to display a tweet.

Advanced filtering operations based on any combination of keyword, chatroom, user name and more to reduce distraction and help focusing

Smart notification based on big data and artificial intelligence

Support for blockchains

If you find any of the above a bad idea, or if you would like to suggest new ideas, feel free to open an issue.

# Contributing

## Try it

Just try it and when you find anything unhandy, tell us about it.
Your idea will help to focus on the most needed features, and a handy client for Matrix will be a reality sooner.

If you know someone who might be interested in open-source communication, star or share the project, more usage and feedback will always be helpful.

## Source

If you are interested in or have experience with Kotlin or Matrix, feel free to click "Fork" and experiment with the source.
There are a lot of ways to make improvements.

It's a good idea to open an issue before you start working, in order to coordinate work and avoid duplicate work.
