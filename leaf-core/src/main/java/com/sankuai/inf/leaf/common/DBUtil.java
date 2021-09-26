package com.sankuai.inf.leaf.common;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class DBUtil {

    public static void executeWithoutThrow(DataSource dataSource, String sql){
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            log.error(String.format("执行如下语句失败：\n%s\n异常信息为：",sql), e);
        }
        finally {
            try {
                stopCloseable(statement);
                stopCloseable(connection);
            }
            catch (Exception e){

            }
        }
    }

    public static void execute(DataSource dataSource, String sql) throws Exception {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        }
        finally {
            stopCloseable(statement);
            stopCloseable(connection);
        }
    }

    public static void stopCloseable(AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.error("关闭" + closeable.getClass().getName() + "异常", e);
                throw e;
            }
        }
    }
    public static class NotSupportDBException extends RuntimeException {
        public NotSupportDBException(String message){
            super(message);
        }
    }

    public static void initDataTable(final DataSource dataSource) throws Exception {
        String initTableSql = getInitTableSql(dataSource);
        execute(dataSource, initTableSql);
    }
    /**
     * @param dataSource
     * @return
     */
    private static String getInitTableSql(DataSource dataSource) throws SQLException {
        String DBType = "oracle";
        DBType = getDataBaseType(dataSource);
        switch (DBType){
            case DB_MYSQL:
                return "CREATE TABLE IF NOT EXISTS ID_WORKER_HOLDER  (\n" +
                        "    service_host varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服务的注册ip和port组成的host地址',\n" +
                        "    service_name varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服务的服务名称',\n" +
                        "    timestamp datetime NOT NULL COMMENT '服务器与客户端连接时间，如果长时间未连接则会被删除',\n" +
                        "    worker_id int NOT NULL COMMENT '生成的workid，取值在0-1024之间',\n" +
                        "    PRIMARY KEY (service_name, service_host),\n" +
                        "    UNIQUE INDEX UNIQUE_SERVICE_WORKER(service_name, worker_id)\n" +
                        ")";
            case DB_ORACLE:
                return "declare cnt number;\n" +
                        "begin\n" +
                        "select count(*)into cnt from user_tables where table_name='ID_WORKER_HOLDER';\n" +
                        "if cnt < 1 then\n" +
                        "    execute immediate 'CREATE TABLE ID_WORKER_HOLDER\n" +
                        "                   (\n" +
                        "                       SERVICE_HOST VARCHAR2(128) NOT NULL ENABLE,\n" +
                        "                       SERVICE_NAME VARCHAR2(60) NOT NULL ENABLE,\n" +
                        "                       TIMESTAMP    DATE NOT NULL ENABLE,\n" +
                        "                       WORKER_ID    NUMBER(4) NOT NULL ENABLE,\n" +
                        "                       CONSTRAINT ID_WORKER_HOLDER_PK PRIMARY KEY (SERVICE_NAME, SERVICE_HOST),\n" +
                        "                       CONSTRAINT \"UNIQUE_SERVICE_WORKER\" UNIQUE (\"SERVICE_NAME\", \"WORKER_ID\")\n" +
                        "                   )';\n" +
                        "    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.SERVICE_HOST IS ''服务的注册ip和port组成的host地址''';\n" +
                        "    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.SERVICE_NAME IS ''服务的服务名称''';\n" +
                        "    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.TIMESTAMP IS ''服务器与客户端连接时间，如果长时间未连接则会被删除''';\n" +
                        "    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.WORKER_ID IS ''生成的workid，取值在0-1024之间''';\n" +
                        "end if;\n" +
                        "end;";
            default:
                throw new NotSupportDBException(String.format("不支持%s类型的数据", DBType));
        }
    }
    public final static String DB_MYSQL = "mysql";

    public final static String DB_SQLSERVER = "sqlserver";

    public final static String DB_ORACLE = "oracle";

    public final static String DB_POSTGRESQL = "postgresql";

    public final static String DB_DB2 = "db2";


    public static String getDataBaseType(final DataSource dataSource) throws SQLException {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        String databaseProductName = metaData.getDatabaseProductName().toLowerCase();
        String dbType = null;
        if (databaseProductName.contains(DB_MYSQL)) {
            dbType = DB_MYSQL;
        } else if (databaseProductName.contains(DB_SQLSERVER)) {
            dbType = DB_SQLSERVER;
        } else if (databaseProductName.contains(DB_ORACLE)) {
            dbType = DB_ORACLE;
        }else if (databaseProductName.contains(DB_POSTGRESQL)) {
            dbType = DB_POSTGRESQL;
        }else if (databaseProductName.contains(DB_DB2)) {
            dbType = DB_DB2;
        }
        return dbType;
    }
}
