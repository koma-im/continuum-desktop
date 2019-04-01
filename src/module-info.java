module link.continuum.desktop {
    requires javafx.controls;
    requires result;
    requires koma.library;
    requires kotlinx.coroutines.core;
    requires kotlin.logging;
    requires java.desktop;
    requires java.prefs;
    requires de.jensd.fx.glyphs.fontawesome;
    requires tornadofx;
    requires javafx.web;
    requires jdk.jsobject;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.javafx;
    requires okhttp3;
    requires de.jensd.fx.glyphs.materialicons;
    requires javafx.media;
    requires controlsfx;
    requires com.squareup.moshi;
    requires kotlin.stdlib.jdk8;
    requires org.junit.jupiter.api;
    requires emoji.java;
    requires cache2k.api;
    requires retrofit2;
    requires okio;
    requires org.slf4j;
    requires de.jensd.fx.glyphs.commons;
    requires java.logging;
    // Error occurred during initialization of boot layer
    // java.lang.module.FindException: Module javafx.fxml not found, required by de.jensd.fx.glyphs.commons
    requires javafx.fxml;
    opens koma.storage.persistence.account to com.squareup.moshi;
    opens koma.storage.rooms.state to com.squareup.moshi;
    opens koma.storage.users.state to com.squareup.moshi;
    exports koma.storage.message.piece to com.squareup.moshi;
    opens koma.storage.message.piece to com.squareup.moshi;
    exports koma.koma_app to javafx.graphics;
    exports koma.gui.view to tornadofx;
    exports koma.gui.view.window.roomfinder.publicroomlist.listcell to tornadofx;
    exports koma.gui.view.window.start to tornadofx;
    exports koma.gui.view.window.preferences.tab.network to tornadofx;
    exports koma.gui.view.chatview to tornadofx;
}
