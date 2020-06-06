package bayern.steinbrecher.database.connection;

import bayern.steinbrecher.database.SupportedDatabases;
import bayern.steinbrecher.database.connection.credentials.SimpleCredentials;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public final class SimpleConnection extends DBConnection {

    private static final Logger LOGGER = Logger.getLogger(SimpleConnection.class.getName());
    private static final Map<SupportedDatabases, String> DRIVER_PROTOCOLS
            = Map.of(SupportedDatabases.MY_SQL, "jdbc:mysql://");
    private Connection connection;

    /**
     * @since 0.1
     */
    public SimpleConnection(SupportedDatabases dbms, String databaseHost, int databasePort, String databaseName,
                            SimpleCredentials credentials) throws AuthException, DatabaseNotFoundException {
        super(databaseName, dbms);
        String databaseHostPrefix = databaseHost;
        if (databaseHostPrefix.endsWith("/")) {
            databaseHostPrefix = databaseHostPrefix.substring(0, databaseHostPrefix.length() - 1);
        }
        String databaseAddress = databaseHostPrefix + ":" + databasePort + "/";
        try {
            connection = DriverManager.getConnection(DRIVER_PROTOCOLS.get(dbms) + databaseAddress
                            + databaseName + "?verifyServerCertificate=false&useSSL=true&zeroDateTimeBehavior=CONVERT_TO_NULL"
                            + "&serverTimezone=UTC",
                    credentials.getUsername(), credentials.getPassword());
//        } catch (CommunicationsException ex) { // FIXME Reintroduce exception case
//            throw new UnknownHostException(ex.getMessage()); //NOPMD - UnknownHostException does not accept a cause.
        } catch (SQLSyntaxErrorException ex) {
            if (ex.getMessage().toLowerCase().contains("unknown database")) {
                throw new DatabaseNotFoundException("The database " + databaseName + " was not found.", ex);
            } else {
                throw new Error("The internal implementation generates invalid SQL.", ex);
            }
        } catch (SQLException ex) {
            throw new AuthException("The authentication to the database failed.", ex);
        }
    }

    @Override
    public List<List<String>> execQuery(String sqlCode) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlCode);
             ResultSet resultset = preparedStatement.executeQuery()) {
            List<List<String>> resultTable = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = 1; i <= resultset.getMetaData().getColumnCount(); i++) {
                labels.add(resultset.getMetaData().getColumnLabel(i));
            }
            resultTable.add(labels);

            while (resultset.next()) {
                List<String> columns = new ArrayList<>();
                for (String l : labels) {
                    columns.add(resultset.getString(l));
                }
                resultTable.add(columns);
            }

            return resultTable;
        }
    }

    @Override
    public void execUpdate(String sqlCode) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlCode)) {
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }
}
