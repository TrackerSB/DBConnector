module bayern.steinbrecher.DBConnector {
    exports bayern.steinbrecher.utility;
    exports bayern.steinbrecher.database;
    exports bayern.steinbrecher.database.connection;
    exports bayern.steinbrecher.database.connection.credentials;
    exports bayern.steinbrecher.database.query;
    exports bayern.steinbrecher.database.scheme;

    requires bayern.steinbrecher.jsch;
    requires com.google.common;
    requires java.logging;
    requires java.sql;
    requires org.jetbrains.annotations;
}