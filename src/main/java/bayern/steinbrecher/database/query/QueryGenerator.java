package bayern.steinbrecher.database.query;

import bayern.steinbrecher.database.SupportedDatabases;
import bayern.steinbrecher.database.scheme.TableScheme;

// FIXME Instead of returning strings return statement which associates a result type
// FIXME Should each method also return the required rights for it?
public final class QueryGenerator {
    private QueryGenerator() {
    }

    public static String generateCreateTableStatement(
            SupportedDatabases dbms, String databaseName, TableScheme<?, ?> scheme) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static String generateRequestForColumnNamesAndTypes(
            SupportedDatabases dbms, String databaseName, TableScheme<?, ?> scheme) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static String generateRequestForTableNames(SupportedDatabases dbms, String databaseName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static String generateRequestForExistenceOfDatabase(SupportedDatabases dbms, String databaseName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
