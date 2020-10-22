package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.scheme.ColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.SimpleColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.TableScheme;
import com.google.common.collect.BiMap;
import freemarker.core.Environment;
import freemarker.ext.beans.StringModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// FIXME Instead of returning strings return statement which associates a result type
// FIXME Should each method also return the required rights for it?
/*
 * @since 0.5
 */
public class QueryGenerator {
    private static final Logger LOGGER = Logger.getLogger(QueryGenerator.class.getName());
    // NOTE Allow creation only within {@link SupportedDatabases}
    private static final Class<SupportedDatabases> TEMPLATE_PROVIDING_CLASS = SupportedDatabases.class;
    @SuppressWarnings("Convert2Lambda")
    private static final Path TEMPLATE_DIR_BASE_PATH = new Callable<>() {
        @Override
        public Path call() {
            String[] packageNameParts = TEMPLATE_PROVIDING_CLASS.getPackageName()
                    .split("\\.");
            return Arrays.stream(packageNameParts)
                    .reduce(Paths.get(""),
                            Path::resolve,
                            Path::resolve);
        }
    }.call();

    private final BiMap<Class<?>, SQLTypeKeyword> types;
    private final char identifierQuoteSymbol;

    private final Template checkDBExistenceTemplate;
    private final Template createTableColumnTemplate;
    private final Template queryTableNamesTemplate;
    private final Template queryColumnNamesAndTypesTemplate;
    private final Template searchQueryTemplate;

    /**
     * @param templateDirectoryPath Specify either an absolute path where the root is the root of this JAR or a
     *                              relative path where the base directory is the package of {@link SupportedDatabases}.
     */
    QueryGenerator(@NotNull Path templateDirectoryPath, @NotNull BiMap<Class<?>, SQLTypeKeyword> types,
                   char identifierQuoteSymbol) {
        this.types = Objects.requireNonNull(types);
        this.identifierQuoteSymbol = identifierQuoteSymbol;

        Configuration templateConfig = new Configuration(Configuration.VERSION_2_3_30);
        templateConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
        templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        templateConfig.setLogTemplateExceptions(false);
        templateConfig.setWrapUncheckedExceptions(true);
        templateConfig.setFallbackOnNullLoopVariable(false);
        try {
            templateConfig.setClassLoaderForTemplateLoading(SupportedDatabases.class.getClassLoader(),
                    TEMPLATE_DIR_BASE_PATH.resolve(Objects.requireNonNull(templateDirectoryPath)).toString());

            checkDBExistenceTemplate = templateConfig.getTemplate("checkDBExistence.ftlh");
            createTableColumnTemplate = templateConfig.getTemplate("createTable.ftlh");
            queryTableNamesTemplate = templateConfig.getTemplate("queryTableNames.ftlh");
            queryColumnNamesAndTypesTemplate = templateConfig.getTemplate("queryColumnNamesAndTypes.ftlh");
            searchQueryTemplate = templateConfig.getTemplate("searchQuery.ftlh");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Returns the class used for representing values of the given SQL type.
     *
     * @param sqlType The type to get a class for.
     * @return An {@link Optional} containing the {@link Class} representing the appropriate SQL type. Returns
     * {@link Optional#empty()} if and only if for {@code sqlType} no class is defined.
     */
    @NotNull
    public Optional<Class<?>> getType(@NotNull String sqlType) {
        return Optional.ofNullable(types.inverse().get(new SQLTypeKeyword(sqlType)));
    }

    @NotNull
    public Optional<String> getType(@NotNull Class<?> columnType) {
        return Optional.ofNullable(types.get(columnType))
                .map(SQLTypeKeyword::getSqlTypeKeyword);
    }

    @NotNull
    public String quoteIdentifier(@NotNull String identifier) {
        return Arrays.stream(identifier.split("\\."))
                .map(i -> identifierQuoteSymbol
                        + i.replaceAll(String.valueOf(identifierQuoteSymbol), "\\" + identifierQuoteSymbol)
                        + identifierQuoteSymbol
                )
                .collect(Collectors.joining("."));
    }

    private String populateTemplate(Template template, Object dataModel) throws GenerationFailedException {
        try (StringWriter writer = new StringWriter()) {
            Environment processingEnvironment = template.createProcessingEnvironment(dataModel, writer);
            processingEnvironment.setVariable("quoteIdentifier", new QuoteIdentifierMethod());
            processingEnvironment.setVariable("getSQLType", new GetSQLTypeMethod());
            processingEnvironment.setVariable("getSQLDefaultValue", new GetSQLDefaultValueMethod());
            processingEnvironment.process();
            return writer.toString();
        } catch (TemplateException | IOException ex) {
            throw new GenerationFailedException("Could not populate template with data", ex);
        }
    }

    /**
     * Since database connections currently (220-10-22) require to specify a database already on creation there not too
     * much use in checking the existence of the database already connected to, i.e. this query is not too useful.
     */
    @NotNull
    public String generateCheckDatabaseExistenceStatement(@NotNull String dbName) throws GenerationFailedException {
        return populateTemplate(checkDBExistenceTemplate, Map.of("dbName", Objects.requireNonNull(dbName)));
    }

    @NotNull
    public String generateCreateTableStatement(@NotNull String dbName, @NotNull TableScheme<?, ?> tableScheme)
            throws GenerationFailedException {
        return populateTemplate(createTableColumnTemplate, Map.of(
                "dbName", dbName,
                "tableScheme", Objects.requireNonNull(tableScheme)
        ));
    }

    @NotNull
    public String generateQueryTableNamesStatement(@NotNull String dbName) throws GenerationFailedException {
        return populateTemplate(queryTableNamesTemplate, Map.of("dbName", Objects.requireNonNull(dbName)));
    }

    @NotNull
    public String generateQueryColumnNamesAndTypesStatement(
            @NotNull String dbName, @NotNull DBConnection.Table<?, ?> table) throws GenerationFailedException {
        return populateTemplate(
                queryColumnNamesAndTypesTemplate, Map.of(
                        "dbName", Objects.requireNonNull(dbName),
                        "table", Objects.requireNonNull(table)
                ));
    }

    /**
     * @param columnsToSelect If empty all columns are selected ({@code SELECT *}).
     * @param conditions      List of conditions which is combined as conjunction.
     */
    @NotNull
    public String generateSearchQueryStatement(@NotNull String dbName, @NotNull DBConnection.Table<?, ?> table,
                                               @NotNull Iterable<DBConnection.Column<?>> columnsToSelect,
                                               @NotNull Iterable<QueryCondition<?>> conditions)
            throws GenerationFailedException {
        // FIXME Check whether any involved columns are contained by the specified table
        return populateTemplate(
                searchQueryTemplate, Map.of(
                        "dbName", Objects.requireNonNull(dbName),
                        "table", Objects.requireNonNull(table),
                        "columnsToSelect", Objects.requireNonNull(columnsToSelect),
                        "conditions", Objects.requireNonNull(conditions)
                ));
    }

    private class QuoteIdentifierMethod implements TemplateMethodModelEx {

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.isEmpty()) {
                throw new TemplateModelException("The identifier to quote is missing");
            } else {
                if(arguments.size() > 1){
                    LOGGER.log(Level.WARNING, "quoteIdentifier(...) got more than just one argument");
                }
                Object identifierCandidate = arguments.get(0);
                if (identifierCandidate instanceof SimpleScalar) {
                    return quoteIdentifier(((SimpleScalar) identifierCandidate).getAsString());
                } else {
                    throw new TemplateModelException(
                            "The given argument is not of type " + SimpleScalar.class.getSimpleName());
                }
            }
        }
    }

    private class GetSQLTypeMethod implements TemplateMethodModelEx {

        @Override
        public String exec(List arguments) throws TemplateModelException {
            if (arguments.isEmpty()) {
                throw new TemplateModelException("The parameter for the column to get its type from is missing");
            } else {
                Object columnPatternCandidate = arguments.get(0);
                if(columnPatternCandidate instanceof StringModel) {
                    Object wrappedColumnPattern = ((StringModel) columnPatternCandidate).getWrappedObject();
                    if (wrappedColumnPattern instanceof ColumnPattern) {
                        ColumnPattern<?, ?> columnPattern = (ColumnPattern<?, ?>) wrappedColumnPattern;
                        Class<?> columnType = columnPattern.getParser()
                                .getType();
                        return getType(columnType)
                                .orElseThrow(
                                        () -> new TemplateModelException(
                                                "No SQL type for " + columnType.getSimpleName() + " available"));
                    } else {
                        throw new TemplateModelException("The given model does not wrap an object of type "
                                + ColumnPattern.class.getSimpleName());
                    }
                } else {
                    throw new TemplateModelException(
                            "The given argument is not of type " + StringModel.class.getSimpleName());
                }
            }
        }
    }

    private static class GetSQLDefaultValueMethod implements TemplateMethodModelEx {

        @Override
        public String exec(List arguments) throws TemplateModelException {
            if (arguments.isEmpty()) {
                throw new TemplateModelException("The parameter for the column to get its type from is missing");
            } else {
                Object columnPatternCandidate = arguments.get(0);
                if (columnPatternCandidate instanceof SimpleColumnPattern) {
                    SimpleColumnPattern<?, ?> columnPattern = (SimpleColumnPattern<?, ?>) columnPatternCandidate;
                    if (columnPattern.hasDefaultValue()) {
                        return columnPattern.getSQLDefaultValue();
                    } else {
                        throw new TemplateModelException(
                                "Can not retrieve default value of column "
                                        + columnPattern.getRealColumnName()
                                        + " since it does not specify a default value");
                    }
                } else {
                    throw new TemplateModelException(
                            "The given argument is not of type " + SimpleColumnPattern.class.getSimpleName());
                }
            }
        }
    }
}
