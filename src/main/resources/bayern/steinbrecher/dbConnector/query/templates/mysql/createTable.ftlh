<#-- @ftlvariable name="" type="bayern.steinbrecher.dbConnector.query.QueryGenerator" -->
<#-- @ftlvariable name="dbName" type="java.lang.String" -->
<#-- @ftlvariable name="scheme" type="bayern.steinbrecher.dbConnector.scheme.TableScheme" -->

CREATE TABLE ${quoteIdentifier(dbName)}.${quoteIdentifier(scheme.tableName)} (
<#list scheme.requiredColumns as column>
    ${quoteIdentifier(column.realColumnName)} ${getSQLType(column)}<#if !column?isAllowNull> IS NOT NULL</#if><#if column?hasDefault> DEFAULT ${getSQLDefaultValue(column)}</#if>,
</#list>
PRIMARY KEY (
<#list scheme.requiredColumns?filter(c -> c.isPrimaryKey()) as key>
    ${quoteIdentifier(key.realColumnName)}<#sep>, </#sep>
</#list>
)
);
