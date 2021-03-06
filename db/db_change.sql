-- 添加小红书快递代码
alter table `pousheng_trade_express_code` add `xhs_code` VARCHAR(64) NULL COMMENT '小红书快递代码' after `kaola_code`;

-- 增大parana_sku_orders表buyer_name字段长度
alter table `parana_sku_orders` modify column `buyer_name` varchar(64);
-- 添加网易考拉快递代码
alter table `pousheng_trade_express_code` add `kaola_code` VARCHAR(64) NULL COMMENT '网易考拉快递代码' after `codoon_code`;

ALTER TABLE `parana_shops` change  `user_id` `user_id` bigint(20)  NULL COMMENT '商家id';
ALTER TABLE `parana_shops` change  `user_name` `user_name` VARCHAR(32)  NULL COMMENT '商家名称';

drop table if exists `pousheng_warehouse_shop_returns`;
CREATE TABLE `pousheng_warehouse_shop_returns` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(128) NULL COMMENT '店铺名称',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `warehouse_name` varchar(128)  NULL COMMENT '仓库名称',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wsr_shop_id` (`shop_id`)
)COMMENT='店铺的退货仓库';


alter table pousheng_users add column `roles_json` varchar(512) DEFAULT NULL COMMENT '用户角色信息' AFTER `mobile`;
alter table pousheng_users add column `type`  SMALLINT  NOT NULL    COMMENT '用户类型' AFTER `mobile`;
drop table if exists `pousheng_spu_materials`;

CREATE TABLE `pousheng_spu_materials` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `material_id` varchar(32) NOT NULL COMMENT '货品id',
  `material_code` varchar(32) NOT NULL COMMENT '货品编码',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_psm_spu_id` (spu_id),
  KEY `idx_psm_material_id` (material_id)
) COMMENT='spu与material_id的关联' ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- 修改字段长度
ALTER TABLE `parana_shipments` CHANGE `extra_json` `extra_json` VARCHAR(8192);
ALTER TABLE `parana_refunds` CHANGE `extra_json` `extra_json` VARCHAR(4096);


drop table if exists `pousheng_warehouses`;

CREATE TABLE `pousheng_warehouses` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NULL COMMENT '仓库编码',
  `name` varchar(64) NOT NULL COMMENT '仓库名称',
  `type` tinyint(4)  NULL COMMENT '仓库类型',
  `status` tinyint(4) NOT NULL COMMENT '仓库状态',
  `address` varchar(128) NULL COMMENT '仓库地址',
  `owner_id` bigint(20)  NULL COMMENT '负责人id',
  `is_default` tinyint(4) NULL COMMENT '是否默认发货仓',
  `extra_json` varchar(2048) NULL COMMENT '附加信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_code` (`code`)
) COMMENT='仓库';

drop index idx_spus_spu_code on `parana_spus`;
CREATE INDEX idx_spus_spu_code ON `parana_spus` (`spu_code`);

drop table if exists `pousheng_warehouse_address_rules`;

CREATE TABLE `pousheng_warehouse_address_rules`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `address_id` bigint(20) NOT NULL COMMENT '地址id',
  `address_name` varchar(20) NOT NULL COMMENT '地址名称',
  `rule_id` bigint(20)  NOT NULL COMMENT '规则id',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_address_rules_rid` (`rule_id`),
  KEY `idx_warehouse_address_rules_aid` (`address_id`),
  KEY `idx_warehouse_address_rules_sid` (`shop_id`)
)COMMENT='地址和仓库规则的关联';

drop table if exists `pousheng_stock_bills`;


-- 删除spu spu_code 唯一索引
drop index idx_spus_spu_code on parana_spus ;
-- 创建spu spu_code 索引
create index idx_spus_spu_code on parana_spus (spu_code) ;




ALTER TABLE `pousheng_warehouse_company_rules` DROP COLUMN `shop_id`;
ALTER TABLE `pousheng_warehouse_company_rules` DROP COLUMN `shop_name`;
-- 买家名称允许为空
ALTER TABLE `parana_shop_orders` change  `buyer_name` `buyer_name` VARCHAR(64)  NULL COMMENT '买家名称';

-- 发票信息中user_id允许为空
ALTER TABLE `parana_invoices` change  `user_id` `user_id` bigint(20)  NULL COMMENT '用户id';
-- spu归组规则新增字段rule_detail(规则详情)
ALTER TABLE `pousheng_sku_group_rules` ADD COLUMN `rule_detail` VARCHAR(20) COMMENT '规则详情' AFTER `last_start`;


drop table if exists `pousheng_stock_push_logs`;
CREATE TABLE `pousheng_stock_push_logs`
(
  `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id`  BIGINT(20) NOT NULL  COMMENT '店铺id',
  `shop_name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `quantity`  BIGINT(20) NOT NULL COMMENT 'sku数量',
  `status` INT(1) NOT NULL COMMENT '1:推送成功,2:推送失败',
  `cause` VARCHAR(512) COMMENT '失败原因',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_stock_push_shop_name` (`shop_name`),
  KEY `index_stock_push_shop_id` (`shop_id`),
  KEY `index_stock_push_sku_code` (`sku_code`)
)COMMENT='宝胜库存推送日志';

drop table if exists `pousheng_settlement_pos`;
CREATE TABLE `pousheng_settlement_pos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pos_type` tinyint(4) NOT NULL COMMENT 'pos单类型:1.正常销售,2.售后订单',
  `ship_type` tinyint(4) NOT NULL COMMENT '发货类型:1.销售发货单,2.换货发货单,3.售后',
  `order_id` bigint(20) NOT NULL COMMENT '订单号或者售后单号',
  `shipment_id` bigint(20) DEFAULT NULL COMMENT '发货单号',
  `pos_serial_no` varchar(60) NOT NULL COMMENT 'pos单号',
  `pos_amt` bigint(20) NOT NULL COMMENT 'pos单金额',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `pos_created_at` datetime NOT NULL COMMENT 'POS单创建时间',
  `pos_done_at` datetime NOT NULL COMMENT 'POS单完成时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_settlement_pos_serial_no` (`pos_serial_no`),
  KEY `index_settlement_order_id` (`order_id`)
) COMMENT='宝胜结算管理pos单';

drop table if exists `pousheng_gift_activity`;
create table `pousheng_gift_activity`
(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '活动名称',
  `order_rule` tinyint(4) NOT NULL COMMENT '类型1.订单满足多少钱不限定活动商品,2.订单满足多少钱限定活动商品,3.订单满足多少件不限定活动商品,4.订单满足多少件限定活动商品',
  `order_fee` BIGINT(20)   COMMENT '满足赠品的订单金额',
  `order_quantity`  BIGINT(20) COMMENT '满足赠品的订单数量',
  `total_price`  BIGINT(20) COMMENT '赠品总的金额',
  `status` SMALLINT NOT NULL COMMENT '状态:0.未发布，1.未开始,2.进行中,3.已结束，4.已结束',
  `quantity_rule` tinyint(4) NOT NULL COMMENT '类型:1.不限制前多少人参与活动,2.限制前多少位参与活动',
  `already_activity_quantity` BIGINT(20)  COMMENT '已经有多少人参与活动',
  `activity_quantity` BIGINT(20)  COMMENT '前多少位可以参与活动,为空则不限制(前端输入0，但是后端控制输入0时不会记录)',
  `extra_json` varchar(1024) COMMENT '赠品商品信息',
  `activity_start_at` datetime NOT NULL,
  `activity_end_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_middle_gift_name` (`name`)
)COMMENT='宝胜中台赠品活动表';

-- 库存表添加mpos标识,公司代码，公司名称。
alter table `pousheng_warehouses` add `is_mpos` tinyint default 0 after `is_default`;
alter table `pousheng_warehouses` add `company_id` varchar(64) after `address`;
alter table `pousheng_warehouses` add `company_name` varchar(64) after `company_id`;
alter table `pousheng_warehouses` add index `index_middle_warehouse_company` (`company_id`);


-- 增大shop表extra字段长度
alter table `parana_shops` modify column `extra_json` varchar(2048);

drop table if exists `pousheng_auto_compensation`;
CREATE TABLE `pousheng_auto_compensation` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL COMMENT '任务类型 1:同步无法派单商品至mpos',
  `extra_json` varchar(2048) NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='自动补偿失败任务表';


-- 库存表存储退货仓信息
alter table `pousheng_warehouses` add `tags_json` VARCHAR(2048) NULL COMMENT 'tag信息, json表示' after `extra_json`;


drop table if exists `open_push_order_task`;
CREATE TABLE `open_push_order_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `source_order_id` VARCHAR(200) NOT NULL COMMENT '来源单号',
  `channel` VARCHAR(50) NOT NULL COMMENT '渠道',
  `extra_json` mediumtext NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='外部订单处理失败补偿任务表';


-- 库存表存储退货仓信息
alter table `pousheng_warehouses` add `tags_json` VARCHAR(2048) NULL COMMENT 'tag信息, json表示' after `extra_json`;

-- 添加mpos快递码
alter table `pousheng_trade_express_code` add `mpos_code` VARCHAR(64) NULL COMMENT 'mpos快递代码' after `fenqile_code`;


drop table if exists `shipment_amount`;
CREATE TABLE `shipment_amount` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) COMMENT '中台发货单号',
  `buyer_nick` VARCHAR(50) COMMENT '买家昵称',
  `order_mon` VARCHAR(50) COMMENT '订单总金额',
  `fee_mon` VARCHAR(50) COMMENT '订单总运费',
  `real_mon`VARCHAR(50) COMMENT '买家应付金额',
  `shop_id` VARCHAR(50) COMMENT '下单店铺代码',
  `performance_shop_id` VARCHAR(50) COMMENT '绩效店铺代码',
  `stock_id` VARCHAR(50) COMMENT '仓库编码',
  `online_type` VARCHAR(50) COMMENT '订单来源',
  `online_order_no` VARCHAR(50) COMMENT '来源单号',
  `order_sub_no` VARCHAR(50) COMMENT '中台子订单号',
  `bar_code` VARCHAR(50) COMMENT '中台skuCode',
  `num` VARCHAR(50) COMMENT '数量',
  `preferential_mon` VARCHAR(50) COMMENT '中台折扣',
  `sale_price` VARCHAR(50) COMMENT '销售单价',
  `total_price` VARCHAR(50) COMMENT '总价',
  `hk_order_no` VARCHAR(50) COMMENT '恒康单号',
  `pos_no` VARCHAR(50) COMMENT 'pos单号',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='发货单同步恒康数据表';


drop table if exists `refund_amount`;
CREATE TABLE `refund_amount` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `refund_no`VARCHAR(50) COMMENT '中台退货单号',
  `order_no` VARCHAR(50) COMMENT '中台发货单号',
  `shop_id` VARCHAR(50) COMMENT '下单店铺代码',
  `performance_shop_id` VARCHAR(50) COMMENT '绩效店铺代码',
  `stock_id` VARCHAR(50) COMMENT '仓库编码',
  `refund_order_amount` VARCHAR(50) COMMENT '页面上的退款金额',
  `type` VARCHAR(50) COMMENT '售后类型',
  `total_refund` VARCHAR(50) COMMENT '总的退款金额',
  `online_order_no` VARCHAR(50) COMMENT '来源单号',
  `hk_order_no` VARCHAR(50) COMMENT '恒康单号',
  `pos_no` VARCHAR(50) COMMENT 'pos单号',
  `refund_sub_no` VARCHAR(50) COMMENT '中台退货子订单号',
  `order_sub_no` VARCHAR(50) COMMENT '原销售子订单号',
  `bar_code` VARCHAR(50) COMMENT '货品条码',
  `item_num` VARCHAR(50) COMMENT '售后数量',
  `sale_price` VARCHAR(50) COMMENT '销售价格',
  `refund_amount` VARCHAR(50) COMMENT '商品总进价',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='售后单同步恒康数据表';

-- 补偿任务添加重试次数
ALTER TABLE `pousheng_auto_compensation` ADD time tinyint(4) COMMENT '重试次数' after status;
-- 添加未处理原因筛选
alter table parana_shop_orders add handle_status tinyint(1) after buyer_note;

-- 添加无头件查询条件
alter table parana_order_shipments add spu_codes varchar(512) default null comment '货号' after order_type, add province_id bigint(20) default null comment '省份id' after spu_codes, add city_id bigint(20) default null comment '市id' after province_id, add region_id bigint(20) default null comment '区id' after city_id;

-- 添加订单前缀
-- 补偿任务添加重试次数
ALTER TABLE `pousheng_auto_compensation` ADD time tinyint(4) COMMENT '重试次数' after status;

--添加未处理原因筛选
alter table parana_shop_orders add handle_status tinyint(1) after buyer_note;

--添加无头件查询条件
alter table parana_order_shipments add spu_codes varchar(512) default null comment '货号' after order_type, add province_id bigint(20) default null comment '省份id' after spu_codes, add city_id bigint(20) default null comment '市id' after province_id, add region_id bigint(20) default null comment '区id' after city_id;

--添加订单前缀
alter table parana_shop_orders add order_code varchar(25) after id;
alter table parana_shipments add shipment_code varchar(25) after id;
alter table parana_order_shipments add shipment_code varchar(25) after shipment_id;
alter table parana_order_shipments add after_sale_order_code varchar(25) after after_sale_order_id;
alter table parana_order_shipments add order_code varchar(25) after order_id;
alter table parana_refunds add refund_code varchar(25) after id;
alter table parana_refunds add rele_order_code varchar(25) after after_sale_id;
alter table parana_order_refunds add refund_code varchar(25) after refund_id;
alter table parana_order_refunds add order_code varchar(25) after order_id;

DROP TABLE IF EXISTS `pousheng_item_group_skus`;

-- 库存同步日志添加真实同步时间
alter table pousheng_stock_push_logs add sync_at DATETIME after cause;

alter table pousheng_stock_push_logs add material_id VARCHAR after sku_code;


-- 创建两个临时表来处理库存
-- 库存任务分片表
CREATE TABLE `pousheng_temp_sku_stock_push_partitioner` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `start`       BIGINT      NOT NULL COMMENT '开始id',
  `end`         BIGINT      NOT NULL COMMENT '截止id',
  `status`      VARCHAR(16) NOT NULL COMMENT '状态: INIT/PROCESSING/DONE',
  `machine`     VARCHAR(16) DEFAULT NULL COMMENT '机器名称',
  `created_at`  DATETIME    NOT NULL,
  `updated_at`  DATETIME    NOT NULL,
  PRIMARY KEY (`id`)
);

-- 库存任务推送表
CREATE TABLE `pousheng_temp_sku_stock_push_id` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `sku_code`        VARCHAR(40) NOT NULL COMMENT 'sku编码',
  `push_finish_at`  DATETIME    NULL COMMENT '完成推送时间',
  PRIMARY KEY (`id`)
);

-- 全量库存同步任务处理临时表
CREATE TABLE `pousheng_temp_sku_stock_updated` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` bigint(20) DEFAULT NULL COMMENT '库存同步任务ID',
  `sku_code` varchar(40) DEFAULT '',
  `status` varchar(10) DEFAULT '' COMMENT '状态, 0 待处理，1处理完成',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sku_code` (`sku_code`)
);
-- 库存新增记录或库存数量更新 写入临时表
DROP trigger IF EXISTS `trigger_insert_on_sku_stocks`;
DELIMITER ;;
create trigger trigger_insert_on_sku_stocks after insert  on pousheng_warehouse_sku_stocks FOR EACH ROW begin
 insert into pousheng_temp_sku_stock_updated(sku_code,status,created_at) values(NEW.SKU_CODE,'0',now() );
end
;;
DELIMITER ;

DROP trigger IF EXISTS `trigger_update_on_sku_stocks`;
DELIMITER ;;
create trigger trigger_update_on_sku_stocks after update  on pousheng_warehouse_sku_stocks FOR EACH ROW begin
 if new.base_stock!=old.base_stock then
	insert into pousheng_temp_sku_stock_updated(sku_code,status,created_at) values(NEW.SKU_CODE,'0',now() );
 end if;
	 end
;;
DELIMITER ;
-- 库存同步任务增加类型字段
ALTER TABLE `pousheng_sku_stock_tasks` add `type` VARCHAR(4)
 NULL
 DEFAULT 'INCR'
 COMMENT '同步类型，FULL:全量；INCR:增量'
 AFTER `status`;

CREATE TABLE `pousheng_item_group_skus` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL COMMENT '分组Id',
  `sku_id` int(11) NOT NULL COMMENT '商品skuId',
  `type` tinyint(4) NOT NULL COMMENT '0表示排除商品 1表示组内商品',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_group_sku` (`group_id`,`sku_id`),
  KEY `idx_item_group_skus_gid` (`group_id`),
  KEY `idx_item_group_skus_sid` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组与商品映射关系表';



DROP TABLE IF EXISTS `pousheng_item_groups`;

CREATE TABLE `pousheng_item_groups` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL DEFAULT '' COMMENT '分组名称',
  `group_rule_json` varchar(2048) DEFAULT NULL COMMENT '分组规则',
  `related_num` int(11) NOT NULL DEFAULT '0' COMMENT '关联的货品数量',
  `auto` tinyint(4) NOT NULL DEFAULT '0' COMMENT '自动分组 0不自动，1自动',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组信息表';


DROP TABLE IF EXISTS `pousheng_item_rule_groups`;

CREATE TABLE `pousheng_item_rule_groups` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL COMMENT '商品规则id',
  `group_id` int(11) NOT NULL COMMENT '分组id',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idex_item_rule_id` (`rule_id`),
  KEY `idex_item_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则与分组关系映射表';


DROP TABLE IF EXISTS `pousheng_item_rule_shops`;

CREATE TABLE `pousheng_item_rule_shops` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL COMMENT '商品规则id',
  `shop_id` int(11) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idex_item_rule_shop_id` (`shop_id`),
  KEY `idex_item_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则与店铺关系映射表';

DROP TABLE IF EXISTS `pousheng_item_rules`;

CREATE TABLE `pousheng_item_rules` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(12) DEFAULT NULL COMMENT '规则名称',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则表';

DROP TABLE IF EXISTS `pousheng_schedule_task`;

CREATE TABLE `pousheng_schedule_task` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL COMMENT '任务类型',
  `user_id` int(11) DEFAULT NULL COMMENT '用户id',
  `business_id` int(11) DEFAULT NULL COMMENT '业务id',
  `business_type` int(11) DEFAULT NULL COMMENT '业务类型',
  `status` int(11) NOT NULL COMMENT '当前状态',
  `extra_json` varchar(4096) DEFAULT NULL COMMENT '定时任务的相关参数',
  `result` varchar(1024) DEFAULT NULL COMMENT '执行结果',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='任务信息表';


alter table parana_shipments modify column shipment_serial_no varchar(256)



-- 添加仓库派单优先级类型
alter table `pousheng_warehouse_rules` add `item_priority_type` tinyint(4)  NULL COMMENT '仓库优先级类型 1距离 2排序' after `shop_group_id`;

-- 仓库增加out字段
ALTER TABLE pousheng_warehouses ADD COLUMN `out_code` varchar(64) NULL COMMENT '外部编码' AFTER `code`;
update pousheng_warehouses  set  out_code= JSON_UNQUOTE(json_extract(extra_json,'$.outCode[0]'))​​;

-- 2018.01.08 增加会员应用日志记录表
DROP TABLE IF EXISTS `mc_application_logs`;
CREATE TABLE `mc_application_logs` (
  `id`              BIGINT        UNSIGNED NOT NULL AUTO_INCREMENT,
  `operator_id`     VARCHAR(32)   NOT NULL     COMMENT '操作者Id',
  `root_id`         VARCHAR(32)   DEFAULT NULL COMMENT '外部id',
  `root_key_id`     VARCHAR(32)   NOT NULL     COMMENT '模板key id',
  `metadata`        TEXT          NOT NULL     COMMENT '操作内容',
  `ext1`            VARCHAR(64)   DEFAULT NULL COMMENT '额外字段1',
  `ext2`            VARCHAR(64)   DEFAULT NULL COMMENT '额外字段2',
  `ext3`            VARCHAR(64)   DEFAULT NULL COMMENT '额外字段3',
  `created_at`      DATETIME      NOT NULL     COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL     COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '会员应用日志记录';
CREATE INDEX idx_mc_al_root_id ON mc_application_logs(root_id);
CREATE INDEX idx_mc_al_root_key_id ON mc_application_logs(root_key_id);
CREATE INDEX idx_mc_al_operator_id ON mc_application_logs(operator_id);
CREATE INDEX idx_mc_al_created_at ON mc_application_logs(created_at);

-- 2018.01.22 增加会员应用日志模板key类型
DROP TABLE IF EXISTS `mc_application_log_keys`;
CREATE TABLE `mc_application_log_keys`(
  `id`                      BIGINT        UNSIGNED NOT NULL AUTO_INCREMENT,
  `root_key_class_name`     VARCHAR(128)  NOT NULL     COMMENT '模板key类名',
  `root_key_method_name`    VARCHAR(64)   NOT NULL     COMMENT '模板key方法名',
  `description`             VARCHAR(256)  NOT NULL     COMMENT '任务的描述',
  `root_id_way`             TINYINT       NOT NULL     COMMENT '外部id生成方式',
  `created_at`              DATETIME      NOT NULL     COMMENT '创建时间',
  `updated_at`              DATETIME      NOT NULL     COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '增加会员应用日志模板key类型';
CREATE UNIQUE INDEX idx_mc_ald_root_key_name on mc_application_log_keys(root_key_class_name,root_key_method_name);

drop table if exists `pousheng_stock_record_log`;
CREATE TABLE `pousheng_stock_record_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_id` bigint(20) COMMENT '发货单id',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `shop_id` bigint(20) COMMENT '店铺id',
  `sku_code` varchar(40) NOT NULL DEFAULT '' COMMENT '商品skuId',
  `context` LONGTEXT  COMMENT '明细内容',
  `type` VARCHAR(20)  COMMENT '日志类型',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='库存查询日志';

CREATE TABLE `pousheng_item_rule_warehouses` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL COMMENT '商品规则id',
  `warehouse_id` int(11) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idex_item_rule_warehouse_id` (`warehouse_id`),
  KEY `idex_item_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则与仓库关系映射表';

alter table pousheng_item_groups add type  tinyint  not null default 0  comment '0全国销售,1同公司销售' after name;

alter table pousheng_item_rules add type  tinyint  not null default 0  comment '0店铺分组,1仓库分组' after id;

alter table pousheng_item_group_skus add mark  tinyint  not null default 0  comment '0自动打标,1人工打标' after sku_code ;

alter table parana_order_shipments add part_ship  tinyint  not null default 0  comment '1部分发货' after type ;




alter table `parana_order_shipments` change `province_id` `province` varchar(50) DEFAULT NULL COMMENT '省';
alter table `parana_order_shipments` change `city_id` `city` varchar(50) DEFAULT NULL COMMENT '市';
alter table `parana_order_shipments` change `region_id` `region` varchar(50) DEFAULT NULL COMMENT '区';

alter table pousheng_stock_push_logs add out_id  varchar(64)  not null default 0  comment '店铺外码' after shop_name ;

alter table pousheng_stock_push_logs add material_id  varchar(64)  not null default 0  comment '货号' after shop_name ;

ALTER TABLE `parana_shops` change  `user_id` `user_id` bigint(20)  NULL COMMENT '商家id';
ALTER TABLE `parana_shops` change  `user_name` `user_name` VARCHAR(32)  NULL COMMENT '商家名称';

drop table if exists `pousheng_warehouse_shop_returns`;
CREATE TABLE `pousheng_warehouse_shop_returns` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(128) NULL COMMENT '店铺名称',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `warehouse_name` varchar(128)  NULL COMMENT '仓库名称',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wsr_shop_id` (`shop_id`)
)COMMENT='店铺的退货仓库';


alter table pousheng_users add column `roles_json` varchar(512) DEFAULT NULL COMMENT '用户角色信息' AFTER `mobile`;
alter table pousheng_users add column `type`  SMALLINT  NOT NULL    COMMENT '用户类型' AFTER `mobile`;
drop table if exists `pousheng_spu_materials`;

CREATE TABLE `pousheng_spu_materials` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `material_id` varchar(32) NOT NULL COMMENT '货品id',
  `material_code` varchar(32) NOT NULL COMMENT '货品编码',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_psm_spu_id` (spu_id),
  KEY `idx_psm_material_id` (material_id)
) COMMENT='spu与material_id的关联' ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- 修改字段长度
ALTER TABLE `parana_shipments` CHANGE `extra_json` `extra_json` VARCHAR(8192);
ALTER TABLE `parana_refunds` CHANGE `extra_json` `extra_json` VARCHAR(4096);


drop table if exists `pousheng_warehouses`;

CREATE TABLE `pousheng_warehouses` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NULL COMMENT '仓库编码',
  `name` varchar(64) NOT NULL COMMENT '仓库名称',
  `type` tinyint(4)  NULL COMMENT '仓库类型',
  `status` tinyint(4) NOT NULL COMMENT '仓库状态',
  `address` varchar(128) NULL COMMENT '仓库地址',
  `owner_id` bigint(20)  NULL COMMENT '负责人id',
  `is_default` tinyint(4) NULL COMMENT '是否默认发货仓',
  `extra_json` varchar(2048) NULL COMMENT '附加信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_code` (`code`)
) COMMENT='仓库';

drop index idx_spus_spu_code on `parana_spus`;
CREATE INDEX idx_spus_spu_code ON `parana_spus` (`spu_code`);

drop table if exists `pousheng_warehouse_address_rules`;

CREATE TABLE `pousheng_warehouse_address_rules`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `address_id` bigint(20) NOT NULL COMMENT '地址id',
  `address_name` varchar(20) NOT NULL COMMENT '地址名称',
  `rule_id` bigint(20)  NOT NULL COMMENT '规则id',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_address_rules_rid` (`rule_id`),
  KEY `idx_warehouse_address_rules_aid` (`address_id`),
  KEY `idx_warehouse_address_rules_sid` (`shop_id`)
)COMMENT='地址和仓库规则的关联';

drop table if exists `pousheng_stock_bills`;


-- 删除spu spu_code 唯一索引
drop index idx_spus_spu_code on parana_spus ;
-- 创建spu spu_code 索引
create index idx_spus_spu_code on parana_spus (spu_code) ;




ALTER TABLE `pousheng_warehouse_company_rules` DROP COLUMN `shop_id`;
ALTER TABLE `pousheng_warehouse_company_rules` DROP COLUMN `shop_name`;
-- 买家名称允许为空
ALTER TABLE `parana_shop_orders` change  `buyer_name` `buyer_name` VARCHAR(64)  NULL COMMENT '买家名称';

-- 发票信息中user_id允许为空
ALTER TABLE `parana_invoices` change  `user_id` `user_id` bigint(20)  NULL COMMENT '用户id';
-- spu归组规则新增字段rule_detail(规则详情)
ALTER TABLE `pousheng_sku_group_rules` ADD COLUMN `rule_detail` VARCHAR(20) COMMENT '规则详情' AFTER `last_start`;


drop table if exists `pousheng_stock_push_logs`;
CREATE TABLE `pousheng_stock_push_logs`
(
  `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id`  BIGINT(20) NOT NULL  COMMENT '店铺id',
  `shop_name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `quantity`  BIGINT(20) NOT NULL COMMENT 'sku数量',
  `status` INT(1) NOT NULL COMMENT '1:推送成功,2:推送失败',
  `cause` VARCHAR(512) COMMENT '失败原因',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_stock_push_shop_name` (`shop_name`),
  KEY `index_stock_push_shop_id` (`shop_id`),
  KEY `index_stock_push_sku_code` (`sku_code`)
)COMMENT='宝胜库存推送日志';

drop table if exists `pousheng_settlement_pos`;
CREATE TABLE `pousheng_settlement_pos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pos_type` tinyint(4) NOT NULL COMMENT 'pos单类型:1.正常销售,2.售后订单',
  `ship_type` tinyint(4) NOT NULL COMMENT '发货类型:1.销售发货单,2.换货发货单,3.售后',
  `order_id` bigint(20) NOT NULL COMMENT '订单号或者售后单号',
  `shipment_id` bigint(20) DEFAULT NULL COMMENT '发货单号',
  `pos_serial_no` varchar(60) NOT NULL COMMENT 'pos单号',
  `pos_amt` bigint(20) NOT NULL COMMENT 'pos单金额',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `pos_created_at` datetime NOT NULL COMMENT 'POS单创建时间',
  `pos_done_at` datetime NOT NULL COMMENT 'POS单完成时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_settlement_pos_serial_no` (`pos_serial_no`),
  KEY `index_settlement_order_id` (`order_id`)
) COMMENT='宝胜结算管理pos单';

drop table if exists `pousheng_gift_activity`;
create table `pousheng_gift_activity`
(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '活动名称',
  `order_rule` tinyint(4) NOT NULL COMMENT '类型1.订单满足多少钱不限定活动商品,2.订单满足多少钱限定活动商品,3.订单满足多少件不限定活动商品,4.订单满足多少件限定活动商品',
  `order_fee` BIGINT(20)   COMMENT '满足赠品的订单金额',
  `order_quantity`  BIGINT(20) COMMENT '满足赠品的订单数量',
  `total_price`  BIGINT(20) COMMENT '赠品总的金额',
  `status` SMALLINT NOT NULL COMMENT '状态:0.未发布，1.未开始,2.进行中,3.已结束，4.已结束',
  `quantity_rule` tinyint(4) NOT NULL COMMENT '类型:1.不限制前多少人参与活动,2.限制前多少位参与活动',
  `already_activity_quantity` BIGINT(20)  COMMENT '已经有多少人参与活动',
  `activity_quantity` BIGINT(20)  COMMENT '前多少位可以参与活动,为空则不限制(前端输入0，但是后端控制输入0时不会记录)',
  `extra_json` varchar(1024) COMMENT '赠品商品信息',
  `activity_start_at` datetime NOT NULL,
  `activity_end_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_middle_gift_name` (`name`)
)COMMENT='宝胜中台赠品活动表';

-- 库存表添加mpos标识,公司代码，公司名称。
alter table `pousheng_warehouses` add `is_mpos` tinyint default 0 after `is_default`;
alter table `pousheng_warehouses` add `company_id` varchar(64) after `address`;
alter table `pousheng_warehouses` add `company_name` varchar(64) after `company_id`;
alter table `pousheng_warehouses` add index `index_middle_warehouse_company` (`company_id`);


-- 增大shop表extra字段长度
alter table `parana_shops` modify column `extra_json` varchar(2048);

drop table if exists `pousheng_auto_compensation`;
CREATE TABLE `pousheng_auto_compensation` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL COMMENT '任务类型 1:同步无法派单商品至mpos',
  `extra_json` varchar(2048) NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='自动补偿失败任务表';


-- 库存表存储退货仓信息
alter table `pousheng_warehouses` add `tags_json` VARCHAR(2048) NULL COMMENT 'tag信息, json表示' after `extra_json`;


drop table if exists `open_push_order_task`;
CREATE TABLE `open_push_order_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `source_order_id` VARCHAR(200) NOT NULL COMMENT '来源单号',
  `channel` VARCHAR(50) NOT NULL COMMENT '渠道',
  `extra_json` mediumtext NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='外部订单处理失败补偿任务表';


-- 库存表存储退货仓信息
alter table `pousheng_warehouses` add `tags_json` VARCHAR(2048) NULL COMMENT 'tag信息, json表示' after `extra_json`;

-- 添加mpos快递码
alter table `pousheng_trade_express_code` add `mpos_code` VARCHAR(64) NULL COMMENT 'mpos快递代码' after `fenqile_code`;


drop table if exists `shipment_amount`;
CREATE TABLE `shipment_amount` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) COMMENT '中台发货单号',
  `buyer_nick` VARCHAR(50) COMMENT '买家昵称',
  `order_mon` VARCHAR(50) COMMENT '订单总金额',
  `fee_mon` VARCHAR(50) COMMENT '订单总运费',
  `real_mon`VARCHAR(50) COMMENT '买家应付金额',
  `shop_id` VARCHAR(50) COMMENT '下单店铺代码',
  `performance_shop_id` VARCHAR(50) COMMENT '绩效店铺代码',
  `stock_id` VARCHAR(50) COMMENT '仓库编码',
  `online_type` VARCHAR(50) COMMENT '订单来源',
  `online_order_no` VARCHAR(50) COMMENT '来源单号',
  `order_sub_no` VARCHAR(50) COMMENT '中台子订单号',
  `bar_code` VARCHAR(50) COMMENT '中台skuCode',
  `num` VARCHAR(50) COMMENT '数量',
  `preferential_mon` VARCHAR(50) COMMENT '中台折扣',
  `sale_price` VARCHAR(50) COMMENT '销售单价',
  `total_price` VARCHAR(50) COMMENT '总价',
  `hk_order_no` VARCHAR(50) COMMENT '恒康单号',
  `pos_no` VARCHAR(50) COMMENT 'pos单号',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='发货单同步恒康数据表';


drop table if exists `refund_amount`;
CREATE TABLE `refund_amount` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `refund_no`VARCHAR(50) COMMENT '中台退货单号',
  `order_no` VARCHAR(50) COMMENT '中台发货单号',
  `shop_id` VARCHAR(50) COMMENT '下单店铺代码',
  `performance_shop_id` VARCHAR(50) COMMENT '绩效店铺代码',
  `stock_id` VARCHAR(50) COMMENT '仓库编码',
  `refund_order_amount` VARCHAR(50) COMMENT '页面上的退款金额',
  `type` VARCHAR(50) COMMENT '售后类型',
  `total_refund` VARCHAR(50) COMMENT '总的退款金额',
  `online_order_no` VARCHAR(50) COMMENT '来源单号',
  `hk_order_no` VARCHAR(50) COMMENT '恒康单号',
  `pos_no` VARCHAR(50) COMMENT 'pos单号',
  `refund_sub_no` VARCHAR(50) COMMENT '中台退货子订单号',
  `order_sub_no` VARCHAR(50) COMMENT '原销售子订单号',
  `bar_code` VARCHAR(50) COMMENT '货品条码',
  `item_num` VARCHAR(50) COMMENT '售后数量',
  `sale_price` VARCHAR(50) COMMENT '销售价格',
  `refund_amount` VARCHAR(50) COMMENT '商品总进价',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='售后单同步恒康数据表';

-- 补偿任务添加重试次数
ALTER TABLE `pousheng_auto_compensation` ADD time tinyint(4) COMMENT '重试次数' after status;
-- 添加未处理原因筛选
alter table parana_shop_orders add handle_status tinyint(1) after buyer_note;

-- 添加无头件查询条件
alter table parana_order_shipments add spu_codes varchar(512) default null comment '货号' after order_type, add province_id bigint(20) default null comment '省份id' after spu_codes, add city_id bigint(20) default null comment '市id' after province_id, add region_id bigint(20) default null comment '区id' after city_id;

-- 添加订单前缀
-- 补偿任务添加重试次数
ALTER TABLE `pousheng_auto_compensation` ADD time tinyint(4) COMMENT '重试次数' after status;

--添加未处理原因筛选
alter table parana_shop_orders add handle_status tinyint(1) after buyer_note;

--添加无头件查询条件
alter table parana_order_shipments add spu_codes varchar(512) default null comment '货号' after order_type, add province_id bigint(20) default null comment '省份id' after spu_codes, add city_id bigint(20) default null comment '市id' after province_id, add region_id bigint(20) default null comment '区id' after city_id;

--添加订单前缀
alter table parana_shop_orders add order_code varchar(25) after id;
alter table parana_shipments add shipment_code varchar(25) after id;
alter table parana_order_shipments add shipment_code varchar(25) after shipment_id;
alter table parana_order_shipments add after_sale_order_code varchar(25) after after_sale_order_id;
alter table parana_order_shipments add order_code varchar(25) after order_id;
alter table parana_refunds add refund_code varchar(25) after id;
alter table parana_refunds add rele_order_code varchar(25) after after_sale_id;
alter table parana_order_refunds add refund_code varchar(25) after refund_id;
alter table parana_order_refunds add order_code varchar(25) after order_id;

DROP TABLE IF EXISTS `pousheng_item_group_skus`;

-- 库存同步日志添加真实同步时间
alter table pousheng_stock_push_logs add sync_at DATETIME after cause;

alter table pousheng_stock_push_logs add material_id VARCHAR after sku_code;


-- 创建两个临时表来处理库存
-- 库存任务分片表
CREATE TABLE `pousheng_temp_sku_stock_push_partitioner` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `start`       BIGINT      NOT NULL COMMENT '开始id',
  `end`         BIGINT      NOT NULL COMMENT '截止id',
  `status`      VARCHAR(16) NOT NULL COMMENT '状态: INIT/PROCESSING/DONE',
  `machine`     VARCHAR(16) DEFAULT NULL COMMENT '机器名称',
  `created_at`  DATETIME    NOT NULL,
  `updated_at`  DATETIME    NOT NULL,
  PRIMARY KEY (`id`)
);

-- 库存任务推送表
CREATE TABLE `pousheng_temp_sku_stock_push_id` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `sku_code`        VARCHAR(40) NOT NULL COMMENT 'sku编码',
  `push_finish_at`  DATETIME    NULL COMMENT '完成推送时间',
  PRIMARY KEY (`id`)
);

-- 全量库存同步任务处理临时表
CREATE TABLE `pousheng_temp_sku_stock_updated` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` bigint(20) DEFAULT NULL COMMENT '库存同步任务ID',
  `sku_code` varchar(40) DEFAULT '',
  `status` varchar(10) DEFAULT '' COMMENT '状态, 0 待处理，1处理完成',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sku_code` (`sku_code`)
);
-- 库存新增记录或库存数量更新 写入临时表
DROP trigger IF EXISTS `trigger_insert_on_sku_stocks`;
DELIMITER ;;
create trigger trigger_insert_on_sku_stocks after insert  on pousheng_warehouse_sku_stocks FOR EACH ROW begin
 insert into pousheng_temp_sku_stock_updated(sku_code,status,created_at) values(NEW.SKU_CODE,'0',now() );
end
;;
DELIMITER ;

DROP trigger IF EXISTS `trigger_update_on_sku_stocks`;
DELIMITER ;;
create trigger trigger_update_on_sku_stocks after update  on pousheng_warehouse_sku_stocks FOR EACH ROW begin
 if new.base_stock!=old.base_stock then
	insert into pousheng_temp_sku_stock_updated(sku_code,status,created_at) values(NEW.SKU_CODE,'0',now() );
 end if;
	 end
;;
DELIMITER ;
-- 库存同步任务增加类型字段
ALTER TABLE `pousheng_sku_stock_tasks` add `type` VARCHAR(4)
 NULL
 DEFAULT 'INCR'
 COMMENT '同步类型，FULL:全量；INCR:增量'
 AFTER `status`;

CREATE TABLE `pousheng_item_group_skus` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL COMMENT '分组Id',
  `sku_id` int(11) NOT NULL COMMENT '商品skuId',
  `type` tinyint(4) NOT NULL COMMENT '0表示排除商品 1表示组内商品',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_group_sku` (`group_id`,`sku_id`),
  KEY `idx_item_group_skus_gid` (`group_id`),
  KEY `idx_item_group_skus_sid` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组与商品映射关系表';



DROP TABLE IF EXISTS `pousheng_item_groups`;

CREATE TABLE `pousheng_item_groups` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL DEFAULT '' COMMENT '分组名称',
  `group_rule_json` varchar(2048) DEFAULT NULL COMMENT '分组规则',
  `related_num` int(11) NOT NULL DEFAULT '0' COMMENT '关联的货品数量',
  `auto` tinyint(4) NOT NULL DEFAULT '0' COMMENT '自动分组 0不自动，1自动',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组信息表';


DROP TABLE IF EXISTS `pousheng_item_rule_groups`;

CREATE TABLE `pousheng_item_rule_groups` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL COMMENT '商品规则id',
  `group_id` int(11) NOT NULL COMMENT '分组id',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idex_item_rule_id` (`rule_id`),
  KEY `idex_item_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则与分组关系映射表';


DROP TABLE IF EXISTS `pousheng_item_rule_shops`;

CREATE TABLE `pousheng_item_rule_shops` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL COMMENT '商品规则id',
  `shop_id` int(11) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idex_item_rule_shop_id` (`shop_id`),
  KEY `idex_item_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则与店铺关系映射表';

DROP TABLE IF EXISTS `pousheng_item_rules`;

CREATE TABLE `pousheng_item_rules` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(12) DEFAULT NULL COMMENT '规则名称',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则表';

DROP TABLE IF EXISTS `pousheng_schedule_task`;

CREATE TABLE `pousheng_schedule_task` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL COMMENT '任务类型',
  `user_id` int(11) DEFAULT NULL COMMENT '用户id',
  `business_id` int(11) DEFAULT NULL COMMENT '业务id',
  `business_type` int(11) DEFAULT NULL COMMENT '业务类型',
  `status` int(11) NOT NULL COMMENT '当前状态',
  `extra_json` varchar(4096) DEFAULT NULL COMMENT '定时任务的相关参数',
  `result` varchar(1024) DEFAULT NULL COMMENT '执行结果',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='任务信息表';


alter table parana_shipments modify column shipment_serial_no varchar(256)



-- 添加仓库派单优先级类型
alter table `pousheng_warehouse_rules` add `item_priority_type` tinyint(4)  NULL COMMENT '仓库优先级类型 1距离 2排序' after `shop_group_id`;

-- 仓库增加out字段
ALTER TABLE pousheng_warehouses ADD COLUMN `out_code` varchar(64) NULL COMMENT '外部编码' AFTER `code`;
update pousheng_warehouses  set  out_code= JSON_UNQUOTE(json_extract(extra_json,'$.outCode[0]'))​​;

-- 2018.01.08 增加会员应用日志记录表
DROP TABLE IF EXISTS `mc_application_logs`;
CREATE TABLE `mc_application_logs` (
  `id`              BIGINT        UNSIGNED NOT NULL AUTO_INCREMENT,
  `operator_id`     VARCHAR(32)   NOT NULL     COMMENT '操作者Id',
  `root_id`         VARCHAR(32)   DEFAULT NULL COMMENT '外部id',
  `root_key_id`     VARCHAR(32)   NOT NULL     COMMENT '模板key id',
  `metadata`        TEXT          NOT NULL     COMMENT '操作内容',
  `ext1`            VARCHAR(64)   DEFAULT NULL COMMENT '额外字段1',
  `ext2`            VARCHAR(64)   DEFAULT NULL COMMENT '额外字段2',
  `ext3`            VARCHAR(64)   DEFAULT NULL COMMENT '额外字段3',
  `created_at`      DATETIME      NOT NULL     COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL     COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '会员应用日志记录';
CREATE INDEX idx_mc_al_root_id ON mc_application_logs(root_id);
CREATE INDEX idx_mc_al_root_key_id ON mc_application_logs(root_key_id);
CREATE INDEX idx_mc_al_operator_id ON mc_application_logs(operator_id);
CREATE INDEX idx_mc_al_created_at ON mc_application_logs(created_at);

-- 2018.01.22 增加会员应用日志模板key类型
DROP TABLE IF EXISTS `mc_application_log_keys`;
CREATE TABLE `mc_application_log_keys`(
  `id`                      BIGINT        UNSIGNED NOT NULL AUTO_INCREMENT,
  `root_key_class_name`     VARCHAR(128)  NOT NULL     COMMENT '模板key类名',
  `root_key_method_name`    VARCHAR(64)   NOT NULL     COMMENT '模板key方法名',
  `description`             VARCHAR(256)  NOT NULL     COMMENT '任务的描述',
  `root_id_way`             TINYINT       NOT NULL     COMMENT '外部id生成方式',
  `created_at`              DATETIME      NOT NULL     COMMENT '创建时间',
  `updated_at`              DATETIME      NOT NULL     COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '增加会员应用日志模板key类型';
CREATE UNIQUE INDEX idx_mc_ald_root_key_name on mc_application_log_keys(root_key_class_name,root_key_method_name);

drop table if exists `pousheng_stock_record_log`;
CREATE TABLE `pousheng_stock_record_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_id` bigint(20) COMMENT '发货单id',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `shop_id` bigint(20) COMMENT '店铺id',
  `sku_code` varchar(40) NOT NULL DEFAULT '' COMMENT '商品skuId',
  `context` LONGTEXT  COMMENT '明细内容',
  `type` VARCHAR(20)  COMMENT '日志类型',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='库存查询日志';

CREATE TABLE `pousheng_item_rule_warehouses` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` int(11) NOT NULL COMMENT '商品规则id',
  `warehouse_id` int(11) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idex_item_rule_warehouse_id` (`warehouse_id`),
  KEY `idex_item_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品规则与仓库关系映射表';

alter table pousheng_item_groups add type  tinyint  not null default 0  comment '0全国销售,1同公司销售' after name;

alter table pousheng_item_rules add type  tinyint  not null default 0  comment '0店铺分组,1仓库分组' after id;

alter table pousheng_item_group_skus add mark  tinyint  not null default 0  comment '0自动打标,1人工打标' after sku_code ;

alter table parana_order_shipments add part_ship  tinyint  not null default 0  comment '1部分发货' after type ;




alter table `parana_order_shipments` change `province_id` `province` varchar(50) DEFAULT NULL COMMENT '省';
alter table `parana_order_shipments` change `city_id` `city` varchar(50) DEFAULT NULL COMMENT '市';
alter table `parana_order_shipments` change `region_id` `region` varchar(50) DEFAULT NULL COMMENT '区';

alter table parana_order_receiver_infos modify column receiver_info_json varchar(2048) DEFAULT NULL COMMENT 'json表示的收货信息';
alter table parana_shipments modify column receiver_infos varchar(2048) DEFAULT NULL COMMENT '收货人信息';


alter table parana_receiver_infos modify column `receive_user_name` varchar(100) NOT NULL COMMENT '收货人姓名';
alter table parana_receiver_infos modify column `mobile` varchar(64) NOT NULL DEFAULT '' COMMENT '手机号';

alter table pousheng_stock_push_logs add out_id  varchar(64)  not null default 0  comment '店铺外码' after shop_name ;

alter table pousheng_stock_push_logs add material_id  varchar(64)  not null default 0  comment '货号' after sku_code ;
ALTER TABLE pousheng_stock_push_logs ADD channel_sku_id VARCHAR(64) DEFAULT NULL COMMENT '第三方sku编号' AFTER sku_code;
ALTER TABLE parana_item_mappings ADD ratio TINYINT(4) DEFAULT NULL COMMENT '商品推送比例' AFTER channel_sku_id;

# 2018-09-07 售后单校验快递唯一性
alter table parana_refunds add `shipment_corp_code`  varchar(32)  default null  comment '快递公司代码' after seller_note ;
alter table parana_refunds add `shipment_serial_no`  varchar(32)  default null  comment '快递单号' after shipment_corp_code ;

# 2018-10-09 新增考拉和唯品会快递代码列
alter table pousheng_trade_express_code add column kaola_code varchar(64)  COMMENT '考拉快递代码' after `codoon_code`;
alter table pousheng_trade_express_code add column vip_code varchar(64)  COMMENT '唯品会快递代码' after `kaola_code`;


alter table parana_shipments add `dispatch_type` tinyint DEFAULT NULL comment '派单类型 1为自动派单 2为手动派单' after is_occupy_shipment;
alter table parana_shipments add `must_ship` tinyint DEFAULT 0 comment '是否必发货 0为否 1为是' after is_occupy_shipment;

create table if not exists pousheng_tasks (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `status`          varchar(32) default null comment '状态',
  `type`            varchar(32) default null comment '类型',
  `context_json`    text default null comment '任务描述，应当精炼简介，尽量保存外部信息主键',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='异步任务表';


alter table pousheng_tasks add detail_json text default null comment '任务描述，只读';

alter table pousheng_tasks add detail_json text default null comment '任务描述，只读';
/**
  *2019-05-08 author:bernie
  * 退款单增加标识字段,用二进制位标识；1:代表待退款（同步电商失败 唯品会独有）
 */
ALTER TABLE parana_refunds ADD COLUMN `refund_flag` int DEFAULT 0 COMMENT '标记位' AFTER `trade_no`;

-- 增加快递公司字段，呵呵
ALTER TABLE `pousheng_trade_express_code` ADD `pdd_code` VARCHAR(64)  NULL  DEFAULT NULL  COMMENT '拼多多'  AFTER `hk_code`;
ALTER TABLE `pousheng_trade_express_code` ADD `youzan_code` VARCHAR(64)  NULL  DEFAULT NULL  COMMENT '有赞'  AFTER `pdd_code`;
ALTER TABLE `pousheng_trade_express_code` ADD `yintai_code` VARCHAR(64)  NULL  DEFAULT NULL  COMMENT '银泰'  AFTER `youzan_code`;

/**
  *2019-07-16 author:bernie
  * 逆物流快递交接
 */
CREATE TABLE `pousheng_reverse_express_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transfer_order_id` varchar(32) DEFAULT NULL COMMENT '交接单号',
  `carrier_code` varchar(64) DEFAULT NULL COMMENT '承运商编码',
  `carrier_name` varchar(32) DEFAULT NULL COMMENT '承运方名称',
  `paid_after_delivery` int(11) DEFAULT NULL COMMENT '支付类型（支付前，货到付款）',
  `line_no` varchar(32) DEFAULT NULL COMMENT '行号',
  `express_no` varchar(32) DEFAULT NULL COMMENT '快递单号',
  `sender_name` varchar(64) DEFAULT NULL COMMENT '寄件人姓名',
  `sender_mobile` varchar(16) DEFAULT NULL COMMENT '寄件人电话',
  `has_order` int(255) DEFAULT NULL COMMENT '是否有单',
  `instore_no` varchar(64) DEFAULT NULL COMMENT '入库单号',
  `shop` varchar(128) DEFAULT NULL COMMENT '店铺',
  `channel` varchar(32) DEFAULT NULL COMMENT '渠道',
  `buyer_memo` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `created_by` varchar(32) DEFAULT NULL,
  `updated_by` varchar(32) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL COMMENT '状态',
  `extra_json` text COMMENT '扩展字段',
  `shop_id` bigint(20) DEFAULT NULL,
  `out_created_at` datetime DEFAULT NULL COMMENT '外部快递单创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_express` (`express_no`),
  KEY `idx_mobile` (`sender_mobile`),
  KEY `idx_out_create` (`out_created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/**
  *2019-07-16 author:bernie
  * 逆物流无头件
 */
CREATE TABLE `pousheng_reverse_headless_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `headless_no` varchar(32) DEFAULT NULL COMMENT '无头件单号',
  `channel` varchar(32) DEFAULT NULL COMMENT '渠道',
  `shop` varchar(32) DEFAULT NULL COMMENT '店铺',
  `express_no` varchar(32) DEFAULT NULL COMMENT '快递单号',
  `platform_no` varchar(32) DEFAULT NULL COMMENT '平台单号',
  `customer` varchar(64) DEFAULT NULL COMMENT '客户名称',
  `phone` varchar(16) DEFAULT NULL COMMENT '手机号码',
  `status` varchar(16) DEFAULT NULL COMMENT '状态',
  `unique_no` varchar(32) DEFAULT NULL COMMENT '唯一码',
  `reason` varchar(255) DEFAULT NULL COMMENT '原因',
  `relate_asn` varchar(32) DEFAULT NULL COMMENT '关联ASN:',
  `inventory_profit_no` varchar(32) DEFAULT NULL COMMENT '盘盈入库单',
  `ship_mode` varchar(16) DEFAULT NULL COMMENT '出货方式',
  `ship_company` varchar(32) DEFAULT NULL COMMENT '出货快递公司',
  `ship_express_no` varchar(32) DEFAULT NULL COMMENT '出货快递单号',
  `created_at` datetime DEFAULT NULL,
  `created_by` varchar(16) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(16) DEFAULT NULL,
  `extra_json` text COMMENT '扩展字段',
  `process_type` varchar(32) DEFAULT NULL COMMENT '处理结果：拒收寄回 正常入库 盘盈 未处理',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `shop_id` bigint(20) DEFAULT NULL COMMENT '店铺id',
  PRIMARY KEY (`id`),
  KEY `idx_express` (`express_no`),
  KEY `idx_platform` (`platform_no`),
  KEY `idx_phone` (`phone`),
  KEY `idx_process_type` (`process_type`),
  KEY `idx_closeat` (`closed_at`),
  KEY `idx_createdat` (`closed_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

/**
  *2019-07-16 author:bernie
  * 逆物入库单闽西
 */
CREATE TABLE `pousheng_reverse_instore_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `instore_no` varchar(16) DEFAULT NULL COMMENT '退货入库单单号',
  `instore_detail_no` varchar(32) DEFAULT NULL COMMENT '退货入库明细行',
  `erp_no` varchar(32) DEFAULT NULL COMMENT 'ERP单号',
  `platform_no` varchar(32) DEFAULT NULL COMMENT '平台销售单单号',
  `carrier_express_no` varchar(32) DEFAULT NULL COMMENT '承运商运单号',
  `real_express_no` varchar(32) DEFAULT NULL COMMENT '实际快递单号',
  `in_flow_at` datetime DEFAULT NULL COMMENT '单据流入时间',
  `accomplish_at` datetime DEFAULT NULL COMMENT '完成时间',
  `status` varchar(16) DEFAULT NULL COMMENT '单据状态',
  `channel` varchar(16) DEFAULT NULL COMMENT '渠道',
  `shop` varchar(64) DEFAULT NULL COMMENT '店铺',
  `created_at` datetime DEFAULT NULL,
  `created_by` varchar(32) DEFAULT NULL,
  `updated_at` varchar(255) DEFAULT NULL,
  `updated_by` datetime DEFAULT NULL,
  `extra_json` text,
  `arrive_wms_at` datetime DEFAULT NULL,
  `customer_note` varchar(255) DEFAULT NULL,
  `shop_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`instore_no`,`instore_detail_no`) USING BTREE,
  KEY `idx_expressno` (`real_express_no`) USING BTREE,
  KEY `idx_createdat` (`created_at`) USING BTREE,
  KEY `idx_erpno` (`erp_no`) USING BTREE,
  KEY `idx_plantform` (`platform_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- 渠道商品推送
CREATE TABLE `parana_channel_item_push` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `brand_id` bigint(20) DEFAULT NULL,
  `brand_name` varchar(100) DEFAULT NULL COMMENT '品牌名称',
  `spu_code` varchar(64) NOT NULL DEFAULT '' COMMENT '货号',
  `spu_name` varchar(64) NOT NULL DEFAULT '' COMMENT 'parana商品名称',
  `sku_id` bigint(20) DEFAULT NULL COMMENT 'skuId',
  `sku_code` varchar(64) DEFAULT NULL COMMENT '条码',
  `channel` varchar(20) NOT NULL COMMENT '外部平台标识, 如jd, taobao等',
  `open_shop_id` bigint(20) DEFAULT NULL COMMENT '外部店铺id',
  `open_shop_name` varchar(64) DEFAULT '' COMMENT '外部店铺名称',
  `channel_item_id` varchar(64) DEFAULT NULL COMMENT '外部平台对应的商品id',
  `channel_sku_id` varchar(64) DEFAULT NULL COMMENT '外部平台sku编码',
  `channel_brand_id` varchar(64) DEFAULT NULL COMMENT '外部平台品牌id',
  `status` tinyint(4) NOT NULL COMMENT '推送状态,1成功,-1失败',
  `color` varchar(5) DEFAULT NULL COMMENT '颜色',
  `size` varchar(5) DEFAULT NULL COMMENT '尺码',
  `price` int(11) DEFAULT NULL COMMENT '吊牌价',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='渠道商品推送状态';

CREATE TABLE `parana_channel_item_push_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `channel` varchar(20) DEFAULT NULL,
  `spu_code` varchar(64) DEFAULT NULL,
  `sku_code` varchar(64) DEFAULT NULL,
  `brand_id` bigint(20) DEFAULT NULL,
  `channel_item_id` varchar(64) DEFAULT NULL COMMENT '外部平台对应的商品id',
  `channel_sku_id` varchar(64) DEFAULT NULL COMMENT '外部平台sku编码',
  `extra_json` mediumtext NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '推送状态,1成功,-1失败',
  `cause` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='渠道商品推送日志';


CREATE TABLE `parana_shop_counter_mappings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `open_shop_id` bigint(20) NOT NULL COMMENT 'open_shops.id',
  `channel` varchar(20) NOT NULL DEFAULT '' COMMENT '外部平台标识',
  `biz_key` varchar(64) NOT NULL DEFAULT '' COMMENT '业务标识',
  `counter_id` varchar(20) NOT NULL DEFAULT '' COMMENT '柜台标识',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态:1启用,-1停用',
  `extra_json` varchar(2000) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_biz_counter_id` (`open_shop_id`,`biz_key`,`counter_id`),
  KEY `idx_open_shop_id` (`open_shop_id`),
  KEY `idx_channel` (`channel`),
  KEY `idx_key` (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='店铺柜台映射';


-- 添加mpos快递码--
alter table `pousheng_trade_express_code` add `xhs_code` VARCHAR(64) NULL COMMENT '小红书快递代码' after `yintai_code`;
# 配置表
DROP TABLE IF EXISTS `pousheng_config`;
CREATE TABLE `pousheng_config` (
  `id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` int(11) DEFAULT NULL COMMENT '配置类型',
  `content` varchar(512) DEFAULT NULL COMMENT '配置内容',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='配置表';

# esp快递日志表
DROP TABLE IF EXISTS `pousheng_esp_log`;
CREATE TABLE `pousheng_esp_log` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `type` varchar(32) DEFAULT NULL COMMENT '接口类型',
  `type_name` varchar(32) DEFAULT NULL COMMENT '类型名称',
  `request_time` datetime DEFAULT NULL COMMENT '请求时间',
  `request_content` varchar(1024) DEFAULT NULL COMMENT '请求体',
  `response_content` varchar(1024) DEFAULT NULL COMMENT '返回体',
  `stroe_code` varchar(32) DEFAULT NULL COMMENT '门店编码',
  `stroe_name` varchar(64) DEFAULT NULL COMMENT '门店名称',
  `express_company_code` varchar(11) DEFAULT NULL COMMENT '快递代码',
  `express_no` varchar(128) DEFAULT NULL COMMENT '快递单号',
  `original_order_no` varchar(128) DEFAULT NULL COMMENT '原始平台单号',
  `mpos_order_no` varchar(128) DEFAULT NULL COMMENT 'mpos单号',
  `middle_shipment_no` varchar(32) DEFAULT NULL COMMENT '中台发货单号',
  `retry_num` int(11) DEFAULT NULL COMMENT '重试次数,最多3次',
  `syn_status` int(11) DEFAULT NULL COMMENT '同步状态，1同步成功,-1同步失败',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `shipment_created_at` datetime DEFAULT NULL COMMENT '发货单创建时间',
  `shipment_updated_at` datetime DEFAULT NULL COMMENT '发货单更新时间',
  PRIMARY KEY (`id`),
  KEY `index_shipmentCode` (`middle_shipment_no`),
  KEY `index_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=132 DEFAULT CHARSET=utf8 COMMENT='Esp快递日志';

# 售后指定退货仓规则表
DROP TABLE IF EXISTS `parana_refunds_warehouse_rules`;
CREATE TABLE `parana_refunds_warehouse_rules` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '销售下单店铺id', 
  `order_shop_code` varchar(30) NOT NULL COMMENT '销售下单店铺代码', 
  `order_shop_name` varchar(64) DEFAULT NULL COMMENT '销售下单店铺名称',  
  `shipment_company_id` varchar(10) NOT NULL COMMENT '发货仓公司账套',
  `refund_warehouse_code` varchar(30) NOT NULL COMMENT '退货仓代码',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_shop_id_company_id` (`shop_id`,`shipment_company_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='售后指定退货仓规则';

