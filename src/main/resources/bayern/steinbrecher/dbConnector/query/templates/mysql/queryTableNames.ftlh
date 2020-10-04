<#-- @ftlvariable name="dbName" type="java.lang.String" -->

SELECT ${quoteIdentifier("TABLE_NAME")}
FROM ${quoteIdentifier("INFORMATION_SCHEMA.TABLES")}
WHERE ${quoteIdentifier("TABLE_SCHEMA")} = '${dbName}';
