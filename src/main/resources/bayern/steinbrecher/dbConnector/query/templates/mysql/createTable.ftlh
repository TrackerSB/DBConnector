<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->
<#-- @ftlvariable name="tableScheme" type="bayern.steinbrecher.dbConnector.scheme.TableScheme" -->

CREATE TABLE ${quoteIdentifier(dbName)}.${quoteIdentifier(tableScheme.tableName)} (
<#list tableScheme.requiredColumns as column>
    ${quoteIdentifier(column.realColumnName)} ${getSQLType(column)}<#if !column.isAllowNull()> NOT NULL</#if><#if column.hasDefaultValue()> DEFAULT ${getSQLDefaultValue(column)}</#if>,
</#list>
PRIMARY KEY (
<#list tableScheme.requiredColumns?filter(c -> c.isPrimaryKey()) as key>
    ${quoteIdentifier(key.realColumnName)}<#sep>, </#sep>
</#list>
)
);
