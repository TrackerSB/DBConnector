package bayern.steinbrecher.test.dbConnector.query;

import bayern.steinbrecher.dbConnector.AuthException;
import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.DatabaseNotFoundException;
import bayern.steinbrecher.dbConnector.SchemeCreationException;
import bayern.steinbrecher.dbConnector.SimpleConnection;
import bayern.steinbrecher.dbConnector.credentials.SimpleCredentials;
import bayern.steinbrecher.dbConnector.query.SupportedDatabases;
import bayern.steinbrecher.dbConnector.scheme.ColumnParser;
import bayern.steinbrecher.dbConnector.scheme.RegexColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.SimpleColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.TableScheme;
import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Stefan Huber
 * @since 0.9
 */
// FIXME Implement dependencies between test methods
public class SupportedDatabasesTest {
    private static final String DB_NAME = "TestDB";
    private static final String DB_USERNAME = "user";
    private static final String DB_PASSWORD = "password";
    private static final SimpleCredentials DB_SIMPLE_CREDENTIALS = new SimpleCredentials(DB_USERNAME, DB_PASSWORD);

    private static final int MYSQL_DB_PORT = 3306;
    private static DB MYSQL_DB;

    private static final List<DBConnection> CONNECTIONS = new ArrayList<>();

    private static final List<SimpleColumnPattern<?, TestTableEntry>> REQUIRED_COLUMNS = List.of(
            new SimpleColumnPattern<>("stringColumn", ColumnParser.STRING_COLUMN_PARSER,
                    TestTableEntry::setAString, Optional.empty(), true, false),
            new SimpleColumnPattern<>("booleanColumn", ColumnParser.BOOLEAN_COLUMN_PARSER,
                    TestTableEntry::setABoolean),
            new SimpleColumnPattern<>("doubleColumn", ColumnParser.DOUBLE_COLUMN_PARSER,
                    TestTableEntry::setADouble),
            new SimpleColumnPattern<>("integerColumn", ColumnParser.INTEGER_COLUMN_PARSER,
                    TestTableEntry::setAnInt),
            new SimpleColumnPattern<>("localDateColumn", ColumnParser.LOCALDATE_COLUMN_PARSER,
                    TestTableEntry::setALocalDate)
    );
    private static final TableScheme<List<TestTableEntry>, TestTableEntry> TEST_TABLE_SCHEME
            = new TableScheme<>(
            "TestTable",
            REQUIRED_COLUMNS,
            List.of(
                    new SimpleColumnPattern<>("optionalColumn", ColumnParser.INTEGER_COLUMN_PARSER,
                            TestTableEntry::setAnOptionalInteger),
                    new RegexColumnPattern<>("^regexColumn\\d+$", ColumnParser.STRING_COLUMN_PARSER,
                            TestTableEntry::setAssociatedValue,
                            columnName -> Integer.parseInt(columnName.substring("regexColumn".length())))
            ),
            TestTableEntry::new,
            e -> e.collect(Collectors.toList())
    );

    /**
     * @since 0.10
     */
    @BeforeAll
    static void setupDatabases() throws ManagedProcessException, AuthException, DatabaseNotFoundException {
        // FIXME Testing SSH connection is not supported yet

        // MySQL
        DBConfigurationBuilder configuration = DBConfigurationBuilder.newBuilder()
                .setPort(MYSQL_DB_PORT);
        MYSQL_DB = DB.newEmbeddedDB(configuration.build());
        MYSQL_DB.start();
        MYSQL_DB.createDB(DB_NAME, DB_USERNAME, DB_PASSWORD);
        CONNECTIONS.add(new SimpleConnection(
                SupportedDatabases.MY_SQL, "localhost", MYSQL_DB_PORT, DB_NAME, DB_SIMPLE_CREDENTIALS, false
        ));
    }

    @AfterAll
    static void stopDatabases() throws ManagedProcessException {
        MYSQL_DB.stop();
    }

    private static Stream<Arguments> provideConnections() {
        return CONNECTIONS.stream()
                .map(Arguments::of);
    }

    /**
     * @since 0.10
     */
    @ParameterizedTest(name = "Check requesting all existing tables")
    @MethodSource("provideConnections")
    void requestAllExistingTables(@NotNull DBConnection connection) {
        assertTrue(connection.getAllTables().isEmpty(), "The database contains expected tables");
    }

    /**
     * @since 0.10
     */
    @ParameterizedTest(name = "Check requesting all existing tables")
    @MethodSource("provideConnections")
    void createTable(@NotNull DBConnection connection) throws SchemeCreationException {
        assumeTrue(
                connection.getAllTables()
                        .stream()
                        .noneMatch(t -> t.getTableName().equals(TEST_TABLE_SCHEME.getTableName())),
                "The database already contains a table which has the name of the test table to be created"
        );
        connection.createTableIfNotExists(TEST_TABLE_SCHEME);
        assertTrue(
                connection.getAllTables()
                        .stream()
                        .anyMatch(t -> t.getTableName().equals(TEST_TABLE_SCHEME.getTableName())),
                "The test table was not created"
        );
        assertEquals(REQUIRED_COLUMNS.size(), connection.getAllColumns(TEST_TABLE_SCHEME).size());
        // FIXME Check names and types of actually existing columns
    }

    private static class TestTableEntry {
        public String aString = null;
        public Boolean aBoolean = null;
        public Double aDouble = null;
        public Integer anInt = null;
        public LocalDate aLocalDate = null;
        public Integer anOptionalInteger = null;
        public final Map<Integer, String> associatedValues = new HashMap<>();

        public TestTableEntry setAString(String aString) {
            this.aString = aString;
            return this;
        }

        public TestTableEntry setABoolean(boolean aBoolean) {
            this.aBoolean = aBoolean;
            return this;
        }

        public TestTableEntry setADouble(double aDouble) {
            this.aDouble = aDouble;
            return this;
        }

        public TestTableEntry setAnInt(int anInt) {
            this.anInt = anInt;
            return this;
        }

        public TestTableEntry setALocalDate(LocalDate aLocalDate) {
            this.aLocalDate = aLocalDate;
            return this;
        }

        public TestTableEntry setAnOptionalInteger(Integer anOptionalInteger) {
            this.anOptionalInteger = anOptionalInteger;
            return this;
        }

        public TestTableEntry setAssociatedValue(Integer key, String value){
            associatedValues.put(key, value);
            return this;
        }
    }
}
