module bayern.steinbrecher.DBConnector.Test {
    requires bayern.steinbrecher.DBConnector;
    requires org.junit.jupiter.api;

    opens bayern.steinbrecher.test.dbConnector.query to org.junit.platform.commons;
}