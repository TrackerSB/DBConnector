<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="columnsToSelect" type="java.util.Collection<bayern.steinbrecher.dbConnector.DBConnection.Column>" -->
<#-- @ftlvariable name="conditions" type="java.util.Collection<bayern.steinbrecher.dbConnector.query.QueryCondition>" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->
<#-- @ftlvariable name="table" type="bayern.steinbrecher.dbConnector.DBConnection.Table" -->

SELECT <#list columnsToSelect as column>${quoteIdentifier(column.name)}<#sep>, </#sep><#else>*</#list>
FROM ${quoteIdentifier(dbName)}.${quoteIdentifier(table.tableScheme.tableName)}
<#list conditions>WHERE <#items as condition>${condition.sqlExpression}<#sep> AND </#sep></#items></#list>;
