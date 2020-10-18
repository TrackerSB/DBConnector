module bayern.steinbrecher.test.DBConnector {
    requires bayern.steinbrecher.DBConnector;
    requires exec;
    requires mariaDB4j.core;
    requires org.jetbrains.annotations;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    opens bayern.steinbrecher.test.dbConnector.query to org.junit.platform.commons;
}