CREATE TABLE IF NOT EXISTS ID_WORKER_HOLDER  (
    service_host varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服务的注册ip和port组成的host地址',
    service_name varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服务的服务名称',
    timestamp datetime NOT NULL COMMENT '服务器与客户端连接时间，如果长时间未连接则会被删除',
    worker_id int NOT NULL COMMENT '生成的workid，取值在0-1024之间',
    PRIMARY KEY (service_name, service_host),
    UNIQUE INDEX UNIQUE_SERVICE_WORKER(service_name, worker_id)
);
