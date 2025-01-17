DROP table ID_WORKER_HOLDER;
CREATE TABLE ID_WORKER_HOLDER
(
    SERVICE_HOST VARCHAR2(128) NOT NULL ENABLE,
    SERVICE_NAME VARCHAR2(60) NOT NULL ENABLE,
    TIMESTAMP    DATE NOT NULL ENABLE,
    WORKER_ID    NUMBER(4) NOT NULL ENABLE,
    CONSTRAINT ID_WORKER_HOLDER_PK PRIMARY KEY (SERVICE_NAME, SERVICE_HOST),
    CONSTRAINT "UNIQUE_SERVICE_WORKER" UNIQUE ("SERVICE_NAME", "WORKER_ID")
);
COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.SERVICE_HOST IS '服务的注册ip和port组成的host地址';
COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.SERVICE_NAME IS '服务的服务名称';
COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.TIMESTAMP IS '服务器与客户端连接时间，如果长时间未连接则会被删除';
COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.WORKER_ID IS '生成的workid，取值在0-1024之间';


/*使得dbms_output.put_line有效打印*/
set serveroutput on;
declare cnt number;
begin
select count(*)into cnt from user_tables where table_name='ID_WORKER_HOLDER';
if cnt < 1 then
    dbms_output.put_line('ID_WORKER_HOLDER表不存在,进行创建');
    execute immediate 'CREATE TABLE ID_WORKER_HOLDER
                   (
                       SERVICE_HOST VARCHAR2(128) NOT NULL ENABLE,
                       SERVICE_NAME VARCHAR2(60) NOT NULL ENABLE,
                       TIMESTAMP    DATE NOT NULL ENABLE,
                       WORKER_ID    NUMBER(4) NOT NULL ENABLE,
                       CONSTRAINT ID_WORKER_HOLDER_PK PRIMARY KEY (SERVICE_NAME, SERVICE_HOST),
                       CONSTRAINT "UNIQUE_SERVICE_WORKER" UNIQUE ("SERVICE_NAME", "WORKER_ID")
                   )';
    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.SERVICE_HOST IS ''服务的注册ip和port组成的host地址''';
    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.SERVICE_NAME IS ''服务的服务名称''';
    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.TIMESTAMP IS ''服务器与客户端连接时间，如果长时间未连接则会被删除''';
    execute immediate 'COMMENT ON COLUMN CRCC_ECC_PURCHASE_FAT.ID_WORKER_HOLDER.WORKER_ID IS ''生成的workid，取值在0-1024之间''';
else
    dbms_output.put_line('ID_WORKER_HOLDER表已存在');
end if;
end;