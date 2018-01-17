# koma

# Features

Display avatars and images

Built-in virtual emoji keyboard

Fetch chat history to allow you to read early messages

Store all data to disk. So it starts very fast and previous chats appear instantly. Incremental sync puts less stree on servers and also make sync much faster

GUI created using JavaFx, uses much less resouces than packaged html and js, also looks better than traditional AWT or Swing

# Getting the application

## Requirements

Java runtime version 8, with JavaFx. You may need to install openjfx if you are using Linux OS.

## Get a compiled package

[Click to download](https://jitpack.io/com/github/koma-im/koma/8e4ad9d/koma-8e4ad9d-jar-with-dependencies.jar)

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

Remember all positions where the user finished reading previously, to make it easy to read all history messages, without manually finding previous unread messages.

Support more types of messages, such as audio and video

Parse plain text messages and display some content in a better way. Display known kinds of links in line, save clicks. Such as replace links to images with actual images, use webview to display a tweet.

Advanced filtering operations based on any combination of keyword, chatroom, user name and more to reduce distraction and help focusing

Smart notification based on big data and artificial intelligence

Support for blockchains

