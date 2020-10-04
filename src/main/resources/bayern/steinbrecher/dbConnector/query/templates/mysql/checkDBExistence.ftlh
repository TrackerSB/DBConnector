<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->

SELECT COUNT(*)
FROM ${quoteIdentifier("INFORMATION_SCHEMA.SCHEMATA")}
WHERE ${quoteIdentifier("SCHEMA_NAME")} = '${dbName}';