package org.x.codehelper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeHelper {
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final static String QUERY_STRUCT_SQL =
            "select column_name, data_type, extra from information_schema.columns " +
                    "where table_schema=:schemaName and table_name=:tableName order by ordinal_position asc";

    public void generateCode() {
        String tableName = "metric_report_app_conf_trace";
        List<ColumnInfo> columnInfoList = loadTableColumnInfo("cmg_112", tableName);
        if (null == columnInfoList || columnInfoList.isEmpty()) {
            return;
        }

        System.out.println();
        String className = CaseUtils.toCamelCase(tableName, true, new char[]{'_'});
        System.out.println("@Data");
        System.out.println("public class " + className + " {");
        for (ColumnInfo col : columnInfoList) {
            System.out.println(columnToAttributeStr(col));
        }
        System.out.println("}");
        System.out.println();

        // generateDbSql(columnInfoList, tableName);
        generateMapper(columnInfoList, tableName, className);
    }

    private void generateMapper(List<ColumnInfo> columnInfoList, String tableName, String className) {
        StringBuilder columnStr = new StringBuilder();
        StringBuilder valueStr = new StringBuilder();
        StringBuilder updateSql = new StringBuilder("update " + tableName + " set ");

        boolean isFirst = true;
        for (ColumnInfo col : columnInfoList) {
            if ("auto_increment".equalsIgnoreCase(col.getExtra())) {
                continue;
            }

            if (isFirst) {
                isFirst = false;
            } else {
                columnStr.append(",");
                valueStr.append(",");
                updateSql.append(",");
            }

            String colName = col.getColumnName();
            String attrName = CaseUtils.toCamelCase(colName, false, new char[]{'_'});
            columnStr.append(colName);
            valueStr.append("#{" + attrName + "}");
            updateSql.append(colName).append("=#{" + attrName + "}");
        }

        String insertSql = "insert into " + tableName + "(" + columnStr + ") values (" + valueStr + ")";
        printMapperCode(className, insertSql, updateSql.toString());
    }

    private void printMapperCode(String className, String insertSql, String updateSql) {
        String objParamDefineStr = className + " " + StringUtils.uncapitalize(className);
        System.out.println("@Mapper");
        System.out.println("public interface " + className + "Mapper {");
        System.out.println("");
        System.out.println("\t@Insert(\"" + insertSql + "\")");
        System.out.println("\tvoid save(" + objParamDefineStr + ");");
        System.out.println();
        System.out.println("\t@Update(\"" + updateSql + "\")");
        System.out.println("\tint update(" + objParamDefineStr + ");");
        System.out.println("}");
    }

    private void generateDbSql(List<ColumnInfo> columnInfoList, String tableName) {
        StringBuilder columnStr = new StringBuilder();
        StringBuilder valueStr = new StringBuilder();
        StringBuilder updateSql = new StringBuilder("update " + tableName + " set ");

        boolean isFirst = true;
        for (ColumnInfo col : columnInfoList) {
            if ("auto_increment".equalsIgnoreCase(col.getExtra())) {
                continue;
            }

            if (isFirst) {
                isFirst = false;
            } else {
                columnStr.append(",");
                valueStr.append(",");
                updateSql.append(",");
            }

            String colName = col.getColumnName();
            String attrName = CaseUtils.toCamelCase(colName, false, new char[]{'_'});
            columnStr.append(colName);
            valueStr.append("#{" + attrName + "}");
            updateSql.append(colName).append("=#{" + attrName + "}");
        }

        String insertSql = "insert into " + tableName + "(" + columnStr + ") values (" + valueStr + ")";
        System.out.println(insertSql);
        System.out.println();
        System.out.println(updateSql);
        System.out.println();
    }

    private String columnToAttributeStr(ColumnInfo col) {
        String attrType = transDataType(col.getDataType().toLowerCase());
        String attrName = CaseUtils.toCamelCase(col.getColumnName(), false, new char[]{'_'});
        return "\tprivate " + attrType + " " + attrName + ";";
    }

    public List<ColumnInfo> loadTableColumnInfo(String schemaName, String tableName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("schemaName", schemaName);
        paramMap.put("tableName", tableName);

        try {
            return namedParameterJdbcTemplate.query(QUERY_STRUCT_SQL, paramMap, new BeanPropertyRowMapper<>(ColumnInfo.class));
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    private String transDataType(String type) {
        switch (type) {
            case "bigint":
                return "Long";
            case "int":
            case "tinyint":
                return "Integer";
            case "datetime":
                return "LocalDateTime";
            default:
                return "String";
        }
    }
}
