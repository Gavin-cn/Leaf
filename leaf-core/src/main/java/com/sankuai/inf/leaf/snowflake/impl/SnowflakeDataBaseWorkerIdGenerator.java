package com.sankuai.inf.leaf.snowflake.impl;

import com.sankuai.inf.leaf.snowflake.WorkerIdGenerator;
import com.sankuai.inf.leaf.snowflake.exception.WorkerIdOutOfRangeException;
import com.sankuai.inf.leaf.snowflake.impl.entity.IdWorkerHolder;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.sankuai.inf.leaf.common.DBUtil.initDataTable;
import static com.sankuai.inf.leaf.common.DBUtil.stopCloseable;

@Slf4j
public class SnowflakeDataBaseWorkerIdGenerator implements WorkerIdGenerator {
    private int workerId = 0;
    private DataSource dataSource;
    private String serviceName;
    private String serviceHost;

    public SnowflakeDataBaseWorkerIdGenerator(DataSource dataSource, String serviceName, String serviceHost){
        this.dataSource = dataSource;
        this.serviceHost = serviceHost;
        this.serviceName = serviceName;
    }
    private LocalDateTime now(){
        ZoneId zoneId = ZoneId.of("UTC+8");
        return LocalDateTime.now(zoneId);
    }
    @Override
    public boolean init() throws Exception {
        initDataTable(dataSource);
        this.workerId = generateWorkerId(serviceName, serviceHost);
        scheduledUpdateWorkerIdHolder(dataSource, serviceName, serviceHost);
        return true;
    }

    private Boolean insertIdWorkerHolder(IdWorkerHolder holder, Connection conn) throws Exception {
        PreparedStatement ps = null;
        boolean isOk = false;
        try {
            StringBuilder insert = new StringBuilder();
            insert.append("INSERT INTO ID_WORKER_HOLDER(SERVICE_HOST,SERVICE_NAME,TIMESTAMP,WORKER_ID) VALUES(");
            insert.append("?,?,?,?)");
            ps = conn.prepareStatement(insert.toString());
            int paramIndex = 1;
            ps.setString(paramIndex++, holder.getServiceHost());
            ps.setString(paramIndex++, holder.getServiceName());
            ps.setTimestamp(paramIndex++, Timestamp.valueOf(holder.getTimestamp()));
            ps.setInt(paramIndex++, holder.getWorkerId());
            isOk = ps.execute();
        } catch (SQLException e) {
            throw e;
        }
        finally {
            stopCloseable(ps);
        }
        return isOk;
    }

    private List<IdWorkerHolder> getIdWorkerHolders(String serviceName, Connection conn) throws Exception {
        List<IdWorkerHolder> holers = new ArrayList<>();
        PreparedStatement ps = null;
        try {
            StringBuilder select = new StringBuilder();
            select.append("select SERVICE_HOST as serviceHost,SERVICE_NAME as serviceName,TIMESTAMP as timeStamp,WORKER_ID as workerId from ID_WORKER_HOLDER ");
            select.append("where SERVICE_NAME = ? for update");
            ps = conn.prepareStatement(select.toString());
            ps.setString(1, serviceName);
            ResultSet resultSet = ps.executeQuery();
            IdWorkerHolder holder;
            while (resultSet.next()){
                holder = IdWorkerHolder.builder().workerId(resultSet.getInt("workerId"))
                        .timestamp(resultSet.getTimestamp("timeStamp").toLocalDateTime())
                        .serviceHost(resultSet.getString("serviceHost"))
                        .serviceName(resultSet.getString("serviceName")).build();
                holers.add(holder);
            }
        } catch (SQLException e) {
            throw e;
        }
        finally {
            stopCloseable(ps);
        }
        return holers;
    }

    private int updateWokerHoldersTimestamp(String serviceName, String serviceHost, Connection conn) throws Exception {
        PreparedStatement ps = null;
        int rownum = 0;
        try {
            StringBuilder insert = new StringBuilder();
            insert.append("UPDATE ID_WORKER_HOLDER SET TIMESTAMP = ? ");
            insert.append("WHERE SERVICE_NAME = ? and SERVICE_HOST = ?");
            ps = conn.prepareStatement(insert.toString());
            int paramIndex = 1;
            ps.setTimestamp(1, Timestamp.valueOf(now()));
            ps.setString(2, serviceName);
            ps.setString(3, serviceHost);
            rownum = ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
        finally {
            stopCloseable(ps);
        }
        return rownum;
    }

    private int generateWorkerId(String serviceName, String serviceHost) throws Exception {
        Connection conn = null;
        int workerId = 0;
        boolean isExsitsInDB = false;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            List<IdWorkerHolder> holers = getIdWorkerHolders(serviceName, conn);
            if(holers != null && holers.size() > 0){
                Optional<IdWorkerHolder> matchHolder = holers.stream().filter(holer -> holer.getServiceHost().equalsIgnoreCase(serviceHost) && holer.getServiceName().equalsIgnoreCase(serviceName)).findFirst();
                if(matchHolder.isPresent()){
                    workerId = matchHolder.get().getWorkerId();
                    isExsitsInDB = true;
                }
                else {
                    while (workerId <= 1024) {
                        final int currentId = workerId;
                        boolean hasSameValue = holers.stream().anyMatch(holer -> holer.getWorkerId() == currentId);
                        if (hasSameValue) {
                            workerId++;
                        } else {
                            break;
                        }
                    }
                }
            }
            if(workerId > 1024){
                throw new WorkerIdOutOfRangeException("服务"+serviceName+"的workerId超出了1024的范围，请查看启动的服务数是否超出了该值，或清除无效的服务");
            }
            if(isExsitsInDB){
                updateWokerHoldersTimestamp(serviceName, serviceHost, conn);
            }
            else {
                IdWorkerHolder holder = IdWorkerHolder.builder().workerId(workerId)
                        .serviceHost(serviceHost).serviceName(serviceName)
                        .timestamp(now()).build();
                insertIdWorkerHolder(holder, conn);
            }
            conn.commit();
        } catch (Exception exception) {
            conn.rollback();
        } finally {
            stopCloseable(conn);
        }
        return workerId;
    }
    private int deleteOutofDateWorkerIdHolder(String serviceName){
        Connection conn = null;
        int rownum = 0;
        try {
            conn = dataSource.getConnection();
            PreparedStatement prep = conn.prepareStatement("DELETE FROM ID_WORKER_HOLDER WHERE SERVICE_NAME = ? and TIMESTAMP < ?");
            prep.setString(1, serviceName);
            prep.setTimestamp(2, Timestamp.valueOf(now().minusMinutes(36)));
            rownum = prep.executeUpdate();
        } catch (Exception e) {
            log.error("deleteOutofDateWorkerIdHolder异常：", e);
        } finally {
            try {
                stopCloseable(conn);
            } catch (Exception e) {
                log.error("deleteOutofDateWorkerIdHolder-关闭conn异常信息：", e);
            }
        }
        if(rownum>0) {
            log.info("针对服务{}删除了{}条无效的workeridholder数据", serviceName, rownum);
        }
        return rownum;
    }
    private void scheduledUpdateWorkerIdHolder(final DataSource dataSource, final String serviceName, final String serviceHost) {
        final WorkerIdGenerator generator = this;
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "dbworkerid-" + serviceHost + "-" + serviceName);
                thread.setDaemon(true);
                return thread;
            }
        }).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Connection conn = null;
                try {
                    conn = dataSource.getConnection();
                    //上报数据，更新对应的数据库中的key
                    int rowNum = updateWokerHoldersTimestamp(serviceName, serviceHost, conn);
                    //如果更新数据行为0，也就是当前服务对应的数据已将因为长时间掉线而被清理，则重新获取
                    if(rowNum <= 0){
                        generator.init();
                    }
                    //清理超过超时时间未进行上报的服务的WorkerID占用
                    deleteOutofDateWorkerIdHolder(serviceName);
                } catch (Exception e) {
                    log.error("scheduledUpdateWorkerIdHolder异常：", e);
                } finally {
                    try {
                        stopCloseable(conn);
                    } catch (Exception e) {
                        log.error("scheduledUpdateWorkerIdHolder-关闭conn异常信息：", e);
                    }
                }
            }
        }, 6L, 6L, TimeUnit.MINUTES); // 每6分钟上报数据
    }


    @Override
    public int getWorkerID() {
        return workerId;
    }
}
