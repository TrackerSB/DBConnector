<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->
<#-- @ftlvariable name="table" type="bayern.steinbrecher.dbConnector.DBConnection.Table" -->

SELECT ${quoteIdentifier("COLUMN_NAME")}, ${quoteIdentifier("DATA_TYPE")}
FROM ${quoteIdentifier("INFORMATION_SCHEMA.COLUMNS")}
WHERE ${quoteIdentifier("TABLE_SCHEMA")} = '${dbName}'
    AND ${quoteIdentifier("TABLE_NAME")} = '${table.tableScheme.tableName}';
