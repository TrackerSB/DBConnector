module bayern.steinbrecher.DBConnector {
    exports bayern.steinbrecher.database;
    exports bayern.steinbrecher.database.connection;
    exports bayern.steinbrecher.database.connection.credentials;
    exports bayern.steinbrecher.database.scheme;

    requires com.google.common;
    requires java.logging;
    requires java.sql;
    requires javafx.base;
    requires jsch;
    requires org.jetbrains.annotations;
}