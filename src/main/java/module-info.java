module bayern.steinbrecher.DBConnector {
    exports bayern.steinbrecher.dbConnector;
    exports bayern.steinbrecher.dbConnector.credentials;
    exports bayern.steinbrecher.dbConnector.query;
    exports bayern.steinbrecher.dbConnector.scheme;
    exports bayern.steinbrecher.dbConnector.utility;

    requires bayern.steinbrecher.jsch;
    requires com.google.common;
    requires java.logging;
    requires java.sql;
    requires org.jetbrains.annotations;
}