package cn.qingweico.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据库类型枚举
 *
 * @author zqw
 * @date 2025/7/27
 */
@AllArgsConstructor
@Getter
public enum DatabaseTypeEnum {
    /**
     * Oracle
     */
    ORACLE,
    /**
     * SQLServer
     */
    SQLSERVER,
    /**
     * MySQL
     */
    MYSQL;

    public static DatabaseTypeEnum getDataSourceType(String datasourceType) {
        if (StringUtils.isEmpty(datasourceType)) {
            return null;
        }
        return valueOf(datasourceType.toUpperCase());
    }
}
