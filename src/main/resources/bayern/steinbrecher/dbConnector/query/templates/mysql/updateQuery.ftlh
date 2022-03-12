<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="conditions" type="java.util.Collection<bayern.steinbrecher.dbConnector.query.QueryCondition>" -->
<#-- @ftlvariable name="changes" type="java.util.Map<String, String>" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->
<#-- @ftlvariable name="table" type="bayern.steinbrecher.dbConnector.DBConnection.Table" -->

UPDATE ${quoteIdentifier(dbName)}.${quoteIdentifier(table.tableScheme.tableName)}
<#list changes?keys>SET <#items as fieldName>${quoteIdentifier(fieldName)} = ${changes[fieldName]}<#sep>, </#sep></#items></#list>
<#list conditions>WHERE <#items as condition>${condition.sqlExpression}<#sep> AND </#sep></#items></#list>;
