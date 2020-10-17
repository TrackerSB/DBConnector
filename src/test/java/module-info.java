module bayern.steinbrecher.test.DBConnector {
    requires bayern.steinbrecher.DBConnector;
    requires org.junit.jupiter.api;

    opens bayern.steinbrecher.test.dbConnector.query to org.junit.platform.commons;
}