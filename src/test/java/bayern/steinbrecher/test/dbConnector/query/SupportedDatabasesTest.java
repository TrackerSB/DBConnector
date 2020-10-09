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
    @DisplayName("Check creation of DBMS instances")
    public void testCreationOfInstances() {
        // FIXME Resources not found since getResource(...) is called in classes directory instead of packed jar
        // for (SupportedDatabases dbms : SupportedDatabases.values()) {
        //     System.out.println(dbms);
        // }
    }
}
