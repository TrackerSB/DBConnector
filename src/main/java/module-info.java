module bayern.steinbrecher.DBConnector {
    exports bayern.steinbrecher.dbConnector;
    exports bayern.steinbrecher.dbConnector.credentials;
    exports bayern.steinbrecher.dbConnector.query;
    exports bayern.steinbrecher.dbConnector.scheme;
    exports bayern.steinbrecher.dbConnector.utility;

    requires bayern.steinbrecher.Utility;
    requires bayern.steinbrecher.jsch;
    requires com.google.common;
    requires freemarker;
    requires java.logging;
    requires java.sql;
    requires javafx.base;
    requires javafx.controls;
    requires org.jetbrains.annotations;

    // FIXME Open to whom?
    opens bayern.steinbrecher.dbConnector.query.templates.mysql;
}