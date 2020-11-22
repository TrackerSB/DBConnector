package bayern.steinbrecher.test.dbConnector.query;

import bayern.steinbrecher.dbConnector.AuthException;
import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.DBConnection.Column;
import bayern.steinbrecher.dbConnector.DBConnection.Table;
import bayern.steinbrecher.dbConnector.DatabaseNotFoundException;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bayern.steinbrecher.test.dbConnector.utility.AssumptionUtility.assumeDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Stefan Huber
 * @since 0.9
 */
// FIXME Implement dependencies between test methods
public class SupportedDatabasesTest {
    private static final Logger LOGGER = Logger.getLogger(SupportedDatabasesTest.class.getName());
    private static final String DB_NAME = "TestDB";
    private static final String DB_USERNAME = "user";
    private static final String DB_PASSWORD = "password";
    private static final SimpleCredentials DB_SIMPLE_CREDENTIALS = new SimpleCredentials(DB_USERNAME, DB_PASSWORD);

    private static final String MYSQL_DB_HOST = "localhost";
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
                SupportedDatabases.MY_SQL, MYSQL_DB_HOST, MYSQL_DB_PORT, DB_NAME, DB_SIMPLE_CREDENTIALS, false
        ));
    }

    @AfterAll
    static void stopDatabases() throws ManagedProcessException {
        CONNECTIONS.forEach(DBConnection::close);
        MYSQL_DB.stop();
    }

    /**
     * @since 0.10
     */
    @ParameterizedTest(name = "Check existence of databases")
    @MethodSource("provideConnections")
    void checkExistenceOfTestDatabase(@NotNull DBConnection connection){
        assertTrue(assertDoesNotThrow(connection::databaseExists));
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
        assertDoesNotThrow(connection::getAllTables, "Could not request tables of database");
    }

    /**
     * @since 0.10
     */
    @ParameterizedTest(name = "Check creation of tables")
    @MethodSource("provideConnections")
    void createTable(@NotNull DBConnection connection) {
        Set<Table<?, ?>> tables = assumeDoesNotThrow(connection::getAllTables, "Could not request existing tables");
        assumeTrue(
                tables.stream()
                        .noneMatch(t -> t.getTableName().equals(TEST_TABLE_SCHEME.getTableName())),
                "The database already contains a table which has the name of the test table to be created"
        );
        assertDoesNotThrow(() -> connection.createTableIfNotExists(TEST_TABLE_SCHEME), "Could not create table");
        tables = assumeDoesNotThrow(connection::getAllTables, "Could not request existing tables");
        assertTrue(
                tables.stream()
                        .anyMatch(t -> t.getTableName().equalsIgnoreCase(TEST_TABLE_SCHEME.getTableName())),
                "The test table was not created"
        );

    }

    /**
     * @since 0.10
     */
    @ParameterizedTest(name = "Check whether required columns of tables are created correctly")
    @MethodSource("provideConnections")
    void requestExistingColumns(@NotNull DBConnection connection) {
        assertDoesNotThrow(() -> connection.getAllColumns(TEST_TABLE_SCHEME),
                String.format("Could not request columns of '%s'", TEST_TABLE_SCHEME.getTableName()));
    }

    /**
     * @since 0.10
     */
    @ParameterizedTest(name = "Check whether required columns of tables are created correctly")
    @MethodSource("provideConnections")
    void checkNamesAndTypesOfRequiredColumns(@NotNull DBConnection connection) {
        Map<String, Class<?>> actualColumnInformation
                = assumeDoesNotThrow(() -> connection.getAllColumns(TEST_TABLE_SCHEME),
                String.format("Could not request columns of '%s'", TEST_TABLE_SCHEME.getTableName()))
                .stream()
                .collect(Collectors.toMap(Column::getName, Column::getColumnType));
        boolean tableCorrectlyCreated = true;
        for (SimpleColumnPattern<?, ?> pattern : REQUIRED_COLUMNS) {
            String requiredColumnName = pattern.getRealColumnName();
            if (actualColumnInformation.containsKey(requiredColumnName)) {
                Class<?> expectedColumnType = pattern.getParser()
                        .getType();
                Class<?> actualColumnType = actualColumnInformation.get(requiredColumnName);
                if (expectedColumnType != actualColumnType) {
                    LOGGER.log(Level.INFO, String.format(
                            "The column '%s' has type '%s' but '%s' is expected", requiredColumnName,
                            actualColumnType.getSimpleName(), expectedColumnType.getSimpleName()));
                    tableCorrectlyCreated = false;
                }
            } else {
                LOGGER.log(Level.INFO,
                        String.format("The required column '%s' was not created", requiredColumnName));
                tableCorrectlyCreated = false;
            }
        }
        assertTrue(tableCorrectlyCreated, "The table was not correctly created (see logger output)");
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

        public TestTableEntry setAssociatedValue(Integer key, String value) {
            associatedValues.put(key, value);
            return this;
        }
    }
}
