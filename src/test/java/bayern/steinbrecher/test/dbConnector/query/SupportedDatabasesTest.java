package bayern.steinbrecher.test.dbConnector.query;

import bayern.steinbrecher.dbConnector.query.SupportedDatabases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Stefan Huber
 * @since 0.9
 */
public class SupportedDatabasesTest {

    @Test
    @DisplayName("Check creation of DBMS instances and validity of query templates")
    public void testCreationOfInstances() {
        SupportedDatabases[] dbmsInstances = SupportedDatabases.values();
        System.out.printf("Created %d instances for DBMSs%n", dbmsInstances.length);
    }
}
