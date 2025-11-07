package cn.qingweico.database;

import cn.hutool.core.util.ReflectUtil;
import cn.qingweico.concurrent.ObjectPool;
import cn.qingweico.concurrent.pool.NamedThreadFactory;
import cn.qingweico.constants.PathConstants;
import cn.qingweico.model.enums.DatabaseTypeEnum;
import cn.qingweico.model.enums.DbConProperty;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * 数据库连接工具
 *
 * @author zqw
 * @date 2022/6/25
 */
@Slf4j
public final class DatabaseHelper {

    private DatabaseHelper() {
    }
    /*these field values can be revised before using*/

    static String driveClassName;
    static String dbUlr;
    static String username;
    static String password;
    static String db = PathConstants.DB_CONFIG_FILE_PATH;

    private static final int DEFAULT_POOL_SIZE = 10;

    static {
        Properties properties = loadDbConfig();
        driveClassName = properties.getProperty(DbConProperty.DRIVE_CLASS_NAME.getProperty());
        dbUlr = properties.getProperty(DbConProperty.JDBC_URL.getProperty());
        username = properties.getProperty(DbConProperty.USERNAME.getProperty());
        password = properties.getProperty(DbConProperty.PASSWORD.getProperty());
    }

    private static final String TABLE_SQL_MYSQL = " SELECT COUNT(1) as count FROM information_schema.tables WHERE table_schema = ? AND table_name = ? ";

    private static final String VIEW_SQL_MYSQL = " SELECT COUNT(1) as count FROM information_schema.views WHERE table_schema = ? AND table_name = ? ";

    public static Properties loadDbConfig() {
        Properties properties = new Properties();
        FileInputStream fin;
        // 设置 JDBC 日志流到控制台
        DriverManager.setLogWriter(new PrintWriter(new PrintStream(System.out), true, Charset.defaultCharset()));
        try {
            fin = new FileInputStream(db);
            properties.load(fin);
        } catch (IOException e) {
            log.error("load {} error, {}", db, e.getMessage());
        }
        return properties;
    }

    public static MysqlDataSource getDatasource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(dbUlr);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    public static HikariDataSource getHikari() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setDataSource(getDatasource());
        config.setThreadFactory(new NamedThreadFactory("[Hikari]"));
        HikariDataSource hikariDataSource = new HikariDataSource(config);
        hikariDataSource.setLogWriter(new PrintWriter(System.out));
        hikariDataSource.setLoginTimeout(3);
        return hikariDataSource;
    }

    public static DataSource wrappedDataSource() {
        return new TransactionAwareDataSourceProxy(getDatasource());
    }

    public static Connection getConnection() {
        try {
            Class.forName(driveClassName);
        } catch (ClassNotFoundException e) {
            log.error("drive class not found, {}", e.getMessage());
        }
        try {
            return DriverManager.getConnection(dbUlr, username, password);
        } catch (SQLException e) {
            log.error("get connection error, {}", e.getMessage());
        }
        return null;
    }

    private static void close(Connection conn, Statement st, ResultSet rs) {
        // 关闭连接
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("关闭连接失败 Connection ", e);
            }
        }
        // 关闭statement
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                log.error("关闭连接失败 Statement ", e);
            }
        }
        // 关闭结果集
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("关闭连接失败 ResultSet ", e);
            }
        }
    }

    public static ObjectPool<Connection, Object> getPool() {
        return getPool(DEFAULT_POOL_SIZE);
    }

    public static ObjectPool<Connection, Object> getPool(int poolSize) {
        Connection connection = getConnection();
        return new ObjectPool<>(poolSize, connection);
    }

    public static <T> List<T> queryForList(Class<T> cls, String sql, Object... obj) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            if (conn == null) {
                throw new RuntimeException("Jdbc Connection is null");
            }
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < obj.length; i++) {
                ps.setObject(i + 1, obj[i]);
            }
            if (log.isDebugEnabled()) {
                log.debug("Executing SQL: {}", sql);
            }
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            Map<String, Field> fieldMap = ReflectUtil.getFieldMap(cls);
            List<T> resultList = new ArrayList<>();
            while (rs.next()) {
                T instance = cls.getDeclaredConstructor().newInstance();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    Field field = fieldMap.get(columnName);
                    if (field != null) {
                        ReflectUtil.setFieldValue(instance, field, value);
                    }
                }
                resultList.add(instance);
            }
            return resultList;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            close(conn, ps, rs);
        }
    }

    public static String getDatabaseType() {
        Connection connection = DatabaseHelper.getConnection();
        if (connection == null) {
            return null;
        }
        try {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查MySQL数据库中指定的表或视图是否存在
     *
     * @param schema    数据库schema名称
     * @param tableName 表/视图名称
     * @return 当且仅当满足以下条件时返回true
     * 1. 当前数据库是MySQL
     * 2. 指定schema中存在该表或视图
     */
    public static boolean queryTableOrViewExisted(String schema, String tableName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String databaseType = getDatabaseType();
        if (DatabaseTypeEnum.MYSQL
                .equals(DatabaseTypeEnum.getDataSourceType(databaseType))) {
            return false;
        }
        try {
            conn = getConnection();
            if (conn == null) {
                return false;
            }
            ps = conn.prepareStatement(TABLE_SQL_MYSQL);
            ps.setString(1, schema);
            ps.setString(2, tableName);
            rs = ps.executeQuery();
            ResultSetMetaData data = rs.getMetaData();

            int count = data.getColumnCount();
            int tableCount = 0;
            while (rs.next()) {
                tableCount = rs.getInt(count);
            }
            if (tableCount < 1) {
                ps = conn.prepareStatement(VIEW_SQL_MYSQL);
                ps.setString(1, schema);
                ps.setString(2, tableName);
                rs = ps.executeQuery();
                data = rs.getMetaData();
                count = data.getColumnCount();
                while (rs.next()) {
                    tableCount = rs.getInt(count);
                }
            }
            return tableCount > 0;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            close(conn, ps, rs);
        }
    }
}
