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
ALTER TABLE `parana_shipments` CHANGE `extra_json` `extra_json` VARCHAR(2048);
ALTER TABLE `parana_refunds` CHANGE `extra_json` `extra_json` VARCHAR(2048);


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