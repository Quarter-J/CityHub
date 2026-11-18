-- Existing database migration for concurrency hardening.
-- Run once against the `dingping` database before deploying the new application.

CREATE TABLE IF NOT EXISTS `tb_blog_like` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `blog_id` bigint(20) UNSIGNED NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_like_blog_user` (`blog_id`, `user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `tb_feed_outbox` (
  `blog_id` bigint(20) UNSIGNED NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`blog_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

DELETE f1
FROM `tb_follow` f1
JOIN `tb_follow` f2
  ON f1.`user_id` = f2.`user_id`
 AND f1.`follow_user_id` = f2.`follow_user_id`
 AND f1.`id` > f2.`id`;

ALTER TABLE `tb_follow`
  ADD UNIQUE INDEX `uk_follow_user` (`user_id`, `follow_user_id`) USING BTREE;

DELETE o1
FROM `tb_voucher_order` o1
JOIN `tb_voucher_order` o2
  ON o1.`user_id` = o2.`user_id`
 AND o1.`voucher_id` = o2.`voucher_id`
 AND o1.`id` > o2.`id`;

ALTER TABLE `tb_voucher_order`
  ADD UNIQUE INDEX `uk_voucher_order_user` (`user_id`, `voucher_id`) USING BTREE;

-- Reservation keys are versioned as seckill:order:v2:* and seckill:pending:v2:*,
-- so old Set values under seckill:order:* will not cause a Redis WRONGTYPE error.
