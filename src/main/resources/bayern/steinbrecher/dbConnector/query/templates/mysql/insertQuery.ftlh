<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="fields" type="java.util.Map<String, String>" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->
<#-- @ftlvariable name="table" type="bayern.steinbrecher.dbConnector.DBConnection.Table" -->

INSERT INTO ${quoteIdentifier(dbName)}.${quoteIdentifier(table.tableScheme.tableName)} <#list changes?keys>(<#items as fieldName>${quoteIdentifier(fieldName)}<#sep>, </#sep></#items>)</#list>
<#list changes?keys>VALUES (<#items as fieldName>${changes[fieldName]}<#sep>, </#sep></#items>)</#list>
