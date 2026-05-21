-- dumass.sql
-- MariaDB dummy seed for the current Callog backend schema.
-- Generated from backend/app JPA entities.
--
-- Logical volume:
--   users: 100
--   campaigns: 120
--   tasks: 1200
--   notifications: 1000
--   plus KPI, campaign, matching, reference, profile, ad-review, and ad-analysis rows.
--
-- Usage:
--   1. Start the backend once first so Hibernate ddl-auto=update creates the schema.
--   2. Run: mysql -u DB_USER -p DB_NAME < dumass.sql
--
-- The script only deletes/recreates rows in the high 900000+ dummy ID ranges below.

START TRANSACTION;

SET @base_org := 900000;
SET @base_user := 910000;
SET @base_profile := 920000;
SET @base_profile_log := 921000;
SET @base_profile_history := 922000;
SET @base_setting := 923000;
SET @base_refresh := 924000;
SET @base_template := 925000;
SET @base_org_kpi := 926000;
SET @base_daily := 927000;
SET @base_monthly := 928000;
SET @base_campaign := 930000;
SET @base_participant := 940000;
SET @base_member := 950000;
SET @base_intro := 955000;
SET @base_campaign_kpi := 956000;
SET @base_contribution := 957000;
SET @base_invitation := 958000;
SET @base_milestone := 960000;
SET @base_task_part := 970000;
SET @base_task := 980000;
SET @base_goal := 990000;
SET @base_benefit := 1000000;
SET @base_asset := 1010000;
SET @base_customer_eval := 1020000;
SET @base_revenue_eval := 1030000;
SET @base_cost_eval := 1040000;
SET @base_operation_eval := 1050000;
SET @base_brand_eval := 1060000;
SET @base_evaluation := 1070000;
SET @base_reference := 1080000;
SET @base_notification := 1090000;
SET @base_policy := 1100000;
SET @base_frame := 1110000;
SET @base_ad_request := 1120000;
SET @base_ad_document := 1130000;
SET @base_ad_page := 1140000;
SET @base_ad_region := 1150000;
SET @base_ad_issue := 1160000;
SET @base_ad_keyword := 1170000;

CREATE TEMPORARY TABLE IF NOT EXISTS `_dumass_digits` (`d` INT PRIMARY KEY);
CREATE TEMPORARY TABLE IF NOT EXISTS `_dumass_seq` (`n` INT PRIMARY KEY);
DELETE FROM `_dumass_digits`;
DELETE FROM `_dumass_seq`;

INSERT INTO `_dumass_digits` (`d`) VALUES
(0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

INSERT INTO `_dumass_seq` (`n`)
SELECT
  ones.`d` + tens.`d` * 10 + hundreds.`d` * 100 + thousands.`d` * 1000 + 1
FROM `_dumass_digits` ones
CROSS JOIN `_dumass_digits` tens
CROSS JOIN `_dumass_digits` hundreds
CROSS JOIN `_dumass_digits` thousands
WHERE ones.`d` + tens.`d` * 10 + hundreds.`d` * 100 + thousands.`d` * 1000 + 1 <= 1200;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `campaign_tag` WHERE `campaign_idx` BETWEEN @base_campaign + 1 AND @base_campaign + 120;
DELETE FROM `campaign_partner` WHERE `campaign_idx` BETWEEN @base_campaign + 1 AND @base_campaign + 120;
DELETE FROM `campaign_method` WHERE `campaign_idx` BETWEEN @base_campaign + 1 AND @base_campaign + 120;
DELETE FROM `reference_tag` WHERE `reference_idx` BETWEEN @base_reference + 1 AND @base_reference + 300;
DELETE FROM `customer_improvement_directions` WHERE `eval_id` BETWEEN @base_customer_eval + 1 AND @base_customer_eval + 300;
DELETE FROM `revenue_improvement_directions` WHERE `eval_id` BETWEEN @base_revenue_eval + 1 AND @base_revenue_eval + 300;
DELETE FROM `cost_improvement_directions` WHERE `eval_id` BETWEEN @base_cost_eval + 1 AND @base_cost_eval + 300;
DELETE FROM `operation_improvement_directions` WHERE `eval_id` BETWEEN @base_operation_eval + 1 AND @base_operation_eval + 300;
DELETE FROM `brand_improvement_directions` WHERE `eval_id` BETWEEN @base_brand_eval + 1 AND @base_brand_eval + 300;
DELETE FROM `campaign_frame_required_field` WHERE `frame_idx` BETWEEN @base_frame + 1 AND @base_frame + 100;
DELETE FROM `campaign_frame_banned_expression` WHERE `frame_idx` BETWEEN @base_frame + 1 AND @base_frame + 100;
DELETE FROM `campaign_frame_recommended_expression` WHERE `frame_idx` BETWEEN @base_frame + 1 AND @base_frame + 100;
DELETE FROM `campaign_frame_approval_process` WHERE `frame_idx` BETWEEN @base_frame + 1 AND @base_frame + 100;

DELETE FROM `ad_analysis_issue` WHERE `idx` BETWEEN @base_ad_issue + 1 AND @base_ad_issue + 240;
DELETE FROM `ad_analysis_keyword` WHERE `idx` BETWEEN @base_ad_keyword + 1 AND @base_ad_keyword + 240;
DELETE FROM `ad_analysis_region` WHERE `idx` BETWEEN @base_ad_region + 1 AND @base_ad_region + 320;
DELETE FROM `ad_analysis_page` WHERE `idx` BETWEEN @base_ad_page + 1 AND @base_ad_page + 160;
DELETE FROM `ad_analysis_document` WHERE `idx` BETWEEN @base_ad_document + 1 AND @base_ad_document + 80;
DELETE FROM `ad_review_request` WHERE `idx` BETWEEN @base_ad_request + 1 AND @base_ad_request + 80;
DELETE FROM `campaign_frame` WHERE `idx` BETWEEN @base_frame + 1 AND @base_frame + 100;
DELETE FROM `notification_admin_policies` WHERE `idx` BETWEEN @base_policy + 1 AND @base_policy + 280;
DELETE FROM `notifications` WHERE `idx` BETWEEN @base_notification + 1 AND @base_notification + 1000;
DELETE FROM `reference_items` WHERE `idx` BETWEEN @base_reference + 1 AND @base_reference + 300;
DELETE FROM `evaluation` WHERE `idx` BETWEEN @base_evaluation + 1 AND @base_evaluation + 300;
DELETE FROM `brand_eval` WHERE `idx` BETWEEN @base_brand_eval + 1 AND @base_brand_eval + 300;
DELETE FROM `operation_eval` WHERE `idx` BETWEEN @base_operation_eval + 1 AND @base_operation_eval + 300;
DELETE FROM `cost_eval` WHERE `idx` BETWEEN @base_cost_eval + 1 AND @base_cost_eval + 300;
DELETE FROM `revenue_eval` WHERE `idx` BETWEEN @base_revenue_eval + 1 AND @base_revenue_eval + 300;
DELETE FROM `customer_eval` WHERE `idx` BETWEEN @base_customer_eval + 1 AND @base_customer_eval + 300;
DELETE FROM `marketing_asset` WHERE `idx` BETWEEN @base_asset + 1 AND @base_asset + 240;
DELETE FROM `partner_benefits` WHERE `idx` BETWEEN @base_benefit + 1 AND @base_benefit + 300;
DELETE FROM `campaign_goal` WHERE `idx` BETWEEN @base_goal + 1 AND @base_goal + 120;
DELETE FROM `task` WHERE `idx` BETWEEN @base_task + 1 AND @base_task + 1200;
DELETE FROM `task_parts` WHERE `idx` BETWEEN @base_task_part + 1 AND @base_task_part + 480;
DELETE FROM `mile_stones` WHERE `idx` BETWEEN @base_milestone + 1 AND @base_milestone + 240;
DELETE FROM `campaign_invitations` WHERE `idx` BETWEEN @base_invitation + 1 AND @base_invitation + 240;
DELETE FROM `campaign_kpi_contribution` WHERE `idx` BETWEEN @base_contribution + 1 AND @base_contribution + 240;
DELETE FROM `campaign_kpis` WHERE `idx` BETWEEN @base_campaign_kpi + 1 AND @base_campaign_kpi + 360;
DELETE FROM `campaign_intro` WHERE `idx` BETWEEN @base_intro + 1 AND @base_intro + 120;
DELETE FROM `campaign_members` WHERE `idx` BETWEEN @base_member + 1 AND @base_member + 600;
DELETE FROM `campaign_participant` WHERE `idx` BETWEEN @base_participant + 1 AND @base_participant + 360;
DELETE FROM `campaigns` WHERE `idx` BETWEEN @base_campaign + 1 AND @base_campaign + 120;
DELETE FROM `kpi_monthly_snapshot` WHERE `idx` BETWEEN @base_monthly + 1 AND @base_monthly + 300;
DELETE FROM `kpi_daily_snapshot` WHERE `idx` BETWEEN @base_daily + 1 AND @base_daily + 140;
DELETE FROM `organization_kpi` WHERE `idx` BETWEEN @base_org_kpi + 1 AND @base_org_kpi + 100;
DELETE FROM `kpi_template` WHERE `idx` BETWEEN @base_template + 1 AND @base_template + 20;
DELETE FROM `refresh_token` WHERE `id` BETWEEN @base_refresh + 1 AND @base_refresh + 100;
DELETE FROM `notification_settings` WHERE `idx` BETWEEN @base_setting + 1 AND @base_setting + 100;
DELETE FROM `profile_image_history` WHERE `idx` BETWEEN @base_profile_history + 1 AND @base_profile_history + 100;
DELETE FROM `profile_image_generation_log` WHERE `idx` BETWEEN @base_profile_log + 1 AND @base_profile_log + 100;
DELETE FROM `user_profile` WHERE `idx` BETWEEN @base_profile + 1 AND @base_profile + 100;
DELETE FROM `user` WHERE `idx` BETWEEN @base_user + 1 AND @base_user + 100;
DELETE FROM `organizations` WHERE `idx` BETWEEN @base_org + 1 AND @base_org + 20;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `organizations`
(`idx`, `code`, `name`, `type`, `general_manager_idx`, `can_create_campaign`, `created_at`)
SELECT
  @base_org + `n`,
  CONCAT('DUMASS_ORG_', LPAD(`n`, 3, '0')),
  CONCAT('Dumass Organization ', LPAD(`n`, 2, '0')),
  CASE WHEN `n` = 1 THEN 'HQ' WHEN MOD(`n`, 5) = 0 THEN 'EXTERNAL_PARTNER' ELSE 'AFFILIATE' END,
  NULL,
  CASE WHEN MOD(`n`, 5) = 0 THEN FALSE ELSE TRUE END,
  DATE_SUB(NOW(), INTERVAL `n` DAY)
FROM `_dumass_seq` WHERE `n` <= 20;

INSERT INTO `user`
(`idx`, `id`, `email`, `name`, `company_name`, `department`, `password`, `enable`, `role`, `organization_id`, `account_status`)
SELECT
  @base_user + `n`,
  CONCAT('dumass_user_', LPAD(`n`, 3, '0')),
  CONCAT('dumass_user_', LPAD(`n`, 3, '0'), '@example.com'),
  CONCAT('Dumass User ', LPAD(`n`, 3, '0')),
  CONCAT('Dumass Company ', LPAD(((`n` - 1) MOD 20) + 1, 2, '0')),
  ELT(((`n` - 1) MOD 8) + 1, 'Marketing', 'Sales', 'Partnership', 'Design', 'Legal', 'Finance', 'Data', 'Operations'),
  '{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOHIhi0/PdoAQQUS7nZHpZSjJx1mCWJ1u',
  TRUE,
  CASE WHEN `n` = 1 THEN 'ROLE_ADMIN' WHEN MOD(`n`, 10) = 0 THEN 'ROLE_GENERAL_MANAGER' WHEN MOD(`n`, 4) = 0 THEN 'ROLE_MANAGER' ELSE 'ROLE_USER' END,
  @base_org + (((`n` - 1) MOD 20) + 1),
  CASE WHEN MOD(`n`, 50) = 0 THEN 'INACTIVE' WHEN MOD(`n`, 37) = 0 THEN 'BLOCKED' ELSE 'ACTIVE' END
FROM `_dumass_seq` WHERE `n` <= 100;

UPDATE `organizations` o
SET o.`general_manager_idx` =
  @base_user + CASE WHEN o.`idx` = @base_org + 1 THEN 1 ELSE ((o.`idx` - @base_org - 1) * 5) + 1 END
WHERE o.`idx` BETWEEN @base_org + 1 AND @base_org + 20;

INSERT INTO `user_profile`
(`idx`, `user_idx`, `email`, `phone`, `profile_image_key`, `profile_image_url`, `create_date`, `update_date`)
SELECT
  @base_profile + `n`,
  @base_user + `n`,
  CONCAT('dumass_user_', LPAD(`n`, 3, '0'), '@example.com'),
  CONCAT('010-', LPAD(1000 + `n`, 4, '0'), '-', LPAD(2000 + `n`, 4, '0')),
  CONCAT('profiles/dumass/user-', LPAD(`n`, 3, '0'), '.png'),
  CONCAT('https://cdn.example.com/profiles/dumass/user-', LPAD(`n`, 3, '0'), '.png'),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `profile_image_generation_log`
(`idx`, `user_idx`, `prompt`, `model`, `requested_size`, `status`, `generated_object_key`, `error_message`, `create_date`, `update_date`)
SELECT
  @base_profile_log + `n`,
  @base_user + `n`,
  CONCAT('professional profile image for user ', `n`),
  'gpt-image-dummy',
  1024,
  CASE WHEN MOD(`n`, 17) = 0 THEN 'FAILED' ELSE 'SUCCEEDED' END,
  CASE WHEN MOD(`n`, 17) = 0 THEN NULL ELSE CONCAT('profiles/generated/dumass-', LPAD(`n`, 3, '0'), '.png') END,
  CASE WHEN MOD(`n`, 17) = 0 THEN 'dummy generation failure' ELSE NULL END,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `profile_image_history`
(`idx`, `user_idx`, `object_key`, `history_type`, `source`, `generation_log_idx`, `prompt`, `create_date`, `update_date`)
SELECT
  @base_profile_history + `n`,
  @base_user + `n`,
  CONCAT('profiles/history/dumass-', LPAD(`n`, 3, '0'), '.png'),
  CASE WHEN MOD(`n`, 2) = 0 THEN 'APPLIED' ELSE 'GENERATED' END,
  CASE WHEN MOD(`n`, 3) = 0 THEN 'MANUAL' ELSE 'AI' END,
  @base_profile_log + `n`,
  CONCAT('history prompt ', `n`),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `notification_settings`
(`idx`, `user_idx`, `enabled`, `level`, `in_app_enabled`, `email_enabled`, `browser_enabled`,
 `task_assigned_enabled`, `task_status_changed_enabled`, `qa_review_enabled`, `deadline_enabled`,
 `campaign_enabled`, `schedule_enabled`, `create_date`, `update_date`)
SELECT
  @base_setting + `n`,
  @base_user + `n`,
  TRUE,
  ELT(((`n` - 1) MOD 3) + 1, 'ESSENTIAL', 'NORMAL', 'ALL'),
  TRUE,
  MOD(`n`, 2) = 0,
  MOD(`n`, 3) = 0,
  TRUE,
  TRUE,
  TRUE,
  TRUE,
  TRUE,
  MOD(`n`, 4) <> 0,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `refresh_token` (`id`, `user_id`, `token`, `expiry_date`)
SELECT
  @base_refresh + `n`,
  CONCAT('dumass_user_', LPAD(`n`, 3, '0')),
  CONCAT('dumass-refresh-token-', LPAD(`n`, 3, '0'), '-', UUID()),
  DATE_ADD(NOW(), INTERVAL 30 DAY)
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `kpi_template`
(`idx`, `name`, `default_unit`, `default_category`, `default_esg_category`, `default_kind`, `scope`,
 `owner_org_id`, `created_by`, `created_at`, `usage_count`)
SELECT
  @base_template + `n`,
  CONCAT('Dumass KPI Template ', LPAD(`n`, 2, '0')),
  ELT(((`n` - 1) MOD 5) + 1, 'KRW', 'count', 'percent', 'score', 'cases'),
  ELT(((`n` - 1) MOD 6) + 1, 'IMPRESSION', 'ENGAGEMENT', 'CONVERSION', 'REVENUE', 'BRAND', 'OTHER'),
  CASE WHEN MOD(`n`, 4) = 0 THEN ELT((`n` MOD 3) + 1, 'ENVIRONMENTAL', 'SOCIAL', 'GOVERNANCE') ELSE NULL END,
  ELT(((`n` - 1) MOD 3) + 1, 'STRATEGIC', 'TACTICAL', 'OPERATIONAL'),
  CASE WHEN MOD(`n`, 2) = 0 THEN 'GLOBAL' ELSE 'ORG_ONLY' END,
  CASE WHEN MOD(`n`, 2) = 0 THEN NULL ELSE @base_org + (((`n` - 1) MOD 20) + 1) END,
  @base_user + (((`n` - 1) MOD 100) + 1),
  NOW(),
  `n` * 3
FROM `_dumass_seq` WHERE `n` <= 20;

INSERT INTO `organization_kpi`
(`idx`, `owner_org_id`, `parent_kpi_id`, `contribution_to_parent`, `name`, `period_type`, `period_code`,
 `period_start`, `period_end`, `target_value`, `actual_value`, `unit`, `category`, `esg_category`, `kind`,
 `status`, `achievability_note`, `template_id`, `previous_version_id`, `created_by`, `created_at`,
 `updated_by`, `updated_at`, `approved_by`, `approved_at`)
SELECT
  @base_org_kpi + `n`,
  @base_org + (((`n` - 1) MOD 20) + 1),
  CASE WHEN `n` > 20 THEN @base_org_kpi + (((`n` - 1) MOD 20) + 1) ELSE NULL END,
  CASE WHEN `n` > 20 THEN ROUND(5 + MOD(`n`, 30), 4) ELSE NULL END,
  CONCAT('Dumass Organization KPI ', LPAD(`n`, 3, '0')),
  ELT(((`n` - 1) MOD 3) + 1, 'QUARTERLY', 'ANNUAL', 'CUSTOM'),
  ELT(((`n` - 1) MOD 4) + 1, '2026-Q1', '2026-Q2', '2026-Q3', '2026-Q4'),
  DATE('2026-01-01'),
  DATE('2026-12-31'),
  10000 + (`n` * 250),
  3000 + (`n` * 117),
  ELT(((`n` - 1) MOD 4) + 1, 'KRW', 'count', 'percent', 'score'),
  ELT(((`n` - 1) MOD 6) + 1, 'IMPRESSION', 'ENGAGEMENT', 'CONVERSION', 'REVENUE', 'BRAND', 'OTHER'),
  CASE WHEN MOD(`n`, 5) = 0 THEN ELT((`n` MOD 3) + 1, 'ENVIRONMENTAL', 'SOCIAL', 'GOVERNANCE') ELSE NULL END,
  ELT(((`n` - 1) MOD 3) + 1, 'STRATEGIC', 'TACTICAL', 'OPERATIONAL'),
  ELT(((`n` - 1) MOD 3) + 1, 'DRAFT', 'ACTIVE', 'ARCHIVED'),
  CONCAT('Achievability note for KPI ', `n`),
  @base_template + (((`n` - 1) MOD 20) + 1),
  NULL,
  @base_user + (((`n` - 1) MOD 100) + 1),
  NOW(),
  @base_user + (((`n` + 9) MOD 100) + 1),
  NOW(),
  CASE WHEN MOD(`n`, 3) = 0 THEN @base_user + 1 ELSE NULL END,
  CASE WHEN MOD(`n`, 3) = 0 THEN NOW() ELSE NULL END
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `kpi_daily_snapshot`
(`idx`, `organization_id`, `snapshot_date`, `avg_kpi_percent`, `snapshot_at`)
SELECT
  @base_daily + `n`,
  @base_org + (((`n` - 1) MOD 20) + 1),
  DATE_SUB(CURDATE(), INTERVAL FLOOR((`n` - 1) / 20) DAY),
  45 + MOD(`n` * 7, 55),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 140;

INSERT INTO `kpi_monthly_snapshot`
(`idx`, `org_kpi_id`, `snapshot_year`, `snapshot_month`, `actual_value`, `target_value`, `snapshot_at`)
SELECT
  @base_monthly + `n`,
  @base_org_kpi + (((`n` - 1) MOD 100) + 1),
  2026,
  FLOOR((`n` - 1) / 100) + 1,
  1000 + (`n` * 13),
  3000 + (`n` * 21),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `campaigns`
(`idx`, `public_id`, `owner_login_id`, `name`, `purpose`, `start_date`, `end_date`, `goals`,
 `asset_name`, `asset_description`, `primary_goal`, `max_cost`, `min_revenue`, `status`, `kpi_analysis`,
 `initials`, `icon`, `color`, `visibility`, `create_date`, `update_date`)
SELECT
  @base_campaign + `n`,
  CONCAT('00000000-0000-4000-8000-', LPAD(`n`, 12, '0')),
  CONCAT('dumass_user_', LPAD(((`n` - 1) MOD 100) + 1, 3, '0')),
  CONCAT('Dumass Campaign ', LPAD(`n`, 3, '0')),
  CONCAT('Campaign purpose ', `n`, ' with segmented partner testing.'),
  DATE_ADD(CURDATE(), INTERVAL -MOD(`n`, 45) DAY),
  DATE_ADD(CURDATE(), INTERVAL 30 + MOD(`n`, 90) DAY),
  CONCAT('Revenue growth, awareness, and retention target ', `n`),
  ELT(((`n` - 1) MOD 5) + 1, 'Hotel Package', 'Loyalty Coupon', 'Brand Booth', 'Mobile Voucher', 'Content Asset'),
  CONCAT('Asset description for campaign ', `n`),
  ELT(((`n` - 1) MOD 5) + 1, 'Revenue', 'Awareness', 'Conversion', 'Retention', 'ESG'),
  CONCAT(FORMAT(1000000 + (`n` * 50000), 0), ' KRW'),
  CONCAT(FORMAT(3000000 + (`n` * 75000), 0), ' KRW'),
  ELT(((`n` - 1) MOD 5) + 1, 'draft', 'active', 'pending', 'closed', 'review'),
  CONCAT('KPI analysis summary for campaign ', `n`),
  CONCAT('C', LPAD(`n`, 2, '0')),
  ELT(((`n` - 1) MOD 5) + 1, 'megaphone', 'gift', 'chart', 'star', 'calendar'),
  ELT(((`n` - 1) MOD 6) + 1, '#2563eb', '#059669', '#dc2626', '#7c3aed', '#ea580c', '#0891b2'),
  ELT(((`n` - 1) MOD 6) + 1, 'PRIVATE', 'HQ_ONLY', 'HQ_AND_AFFILIATE', 'AFFILIATE_ONLY', 'EXTERNAL_ONLY', 'ALL'),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 120;

INSERT INTO `campaign_tag` (`campaign_idx`, `tag`)
SELECT
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  ELT(((`n` - 1) MOD 8) + 1, 'loyalty', 'summer', 'premium', 'family', 'mobile', 'offline', 'retention', 'brand')
FROM `_dumass_seq` WHERE `n` <= 360;

INSERT INTO `campaign_partner` (`campaign_idx`, `partner`)
SELECT
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  CONCAT('Partner Org ', LPAD(((`n` - 1) MOD 40) + 1, 2, '0'))
FROM `_dumass_seq` WHERE `n` <= 360;

INSERT INTO `campaign_method` (`campaign_idx`, `method`)
SELECT
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  ELT(((`n` - 1) MOD 6) + 1, 'POPUP', 'SNS', 'EMAIL', 'APP_PUSH', 'OFFLINE_EVENT', 'COUPON')
FROM `_dumass_seq` WHERE `n` <= 360;

INSERT INTO `campaign_participant` (`idx`, `campaign_idx`, `organization_idx`, `campaign_role`)
SELECT
  @base_participant + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  @base_org + (((`n` - 1) MOD 20) + 1),
  CASE WHEN MOD(`n`, 3) = 1 THEN 'PM' ELSE 'PARTNER' END
FROM `_dumass_seq` WHERE `n` <= 360;

INSERT INTO `campaign_members`
(`idx`, `campaign_idx`, `user_idx`, `campaign_role`, `joined_at`, `create_date`, `update_date`)
SELECT
  @base_member + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  @base_user + (((`n` - 1) MOD 100) + 1),
  ELT(((`n` - 1) MOD 3) + 1, 'USER', 'MANAGER', 'GENERAL_MANAGER'),
  DATE_SUB(NOW(), INTERVAL MOD(`n`, 25) DAY),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 600;

INSERT INTO `campaign_intro`
(`idx`, `campaign_idx`, `rfp_code`, `recruit_deadline`, `hanwha_assets`, `partner_roles`, `customer_tags`,
 `partner_values`, `timeline_events`, `submission_docs`, `attached_files`, `contact_info`, `weight_customer`,
 `weight_revenue`, `weight_cost`, `weight_operation`, `weight_brand`, `overview_items`, `hero_kpis`,
 `target_segment`, `target_scale`, `submission_info`, `customer_items`, `view_count`, `create_date`, `update_date`)
SELECT
  @base_intro + `n`,
  @base_campaign + `n`,
  CONCAT('RFP-DUMASS-', LPAD(`n`, 4, '0')),
  DATE_ADD(NOW(), INTERVAL 14 + MOD(`n`, 30) DAY),
  JSON_OBJECT('items', JSON_ARRAY(CONCAT('Asset ', `n`), 'CRM audience', 'Media inventory')),
  JSON_OBJECT('roles', JSON_ARRAY('Sponsor', 'Channel', 'Operations')),
  JSON_OBJECT('tags', JSON_ARRAY('family', 'premium', 'loyalty')),
  JSON_OBJECT('values', JSON_ARRAY('reach', 'conversion', 'experience')),
  JSON_OBJECT('events', JSON_ARRAY('kickoff', 'launch', 'wrapup')),
  JSON_OBJECT('docs', JSON_ARRAY('proposal', 'media plan')),
  JSON_OBJECT('files', JSON_ARRAY(CONCAT('brief-', `n`, '.pdf'))),
  JSON_OBJECT('email', CONCAT('campaign', `n`, '@example.com')),
  20 + MOD(`n`, 10),
  25 + MOD(`n`, 15),
  10 + MOD(`n`, 10),
  15 + MOD(`n`, 10),
  20 + MOD(`n`, 10),
  JSON_OBJECT('items', JSON_ARRAY('Overview', 'Value', 'Timeline')),
  JSON_OBJECT('kpis', JSON_ARRAY('Reach', 'Revenue', 'Conversion')),
  ELT(((`n` - 1) MOD 5) + 1, 'Young professionals', 'Family travelers', 'VIP members', 'Local shoppers', 'Business guests'),
  ELT(((`n` - 1) MOD 4) + 1, '10k users', '30k users', '50k users', '100k users'),
  JSON_OBJECT('deadline', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 20 DAY), '%Y-%m-%d')),
  JSON_OBJECT('items', JSON_ARRAY('age', 'spend', 'channel')),
  `n` * 11,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 120;

INSERT INTO `campaign_kpis`
(`idx`, `campaign_idx`, `name`, `category`, `target_value`, `actual_value`, `unit`, `owner_label`,
 `owner_user_idx`, `memo`, `next_action`, `measured_at`, `parent_org_kpi_id`, `esg_category`, `create_date`, `update_date`)
SELECT
  @base_campaign_kpi + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  CONCAT('Campaign KPI ', LPAD(`n`, 3, '0')),
  ELT(((`n` - 1) MOD 6) + 1, 'IMPRESSION', 'ENGAGEMENT', 'CONVERSION', 'REVENUE', 'BRAND', 'OTHER'),
  1000 + (`n` * 25),
  100 + (`n` * 7),
  ELT(((`n` - 1) MOD 4) + 1, 'count', 'percent', 'KRW', 'score'),
  CONCAT('Owner Team ', (((`n` - 1) MOD 20) + 1)),
  @base_user + (((`n` - 1) MOD 100) + 1),
  CONCAT('memo ', `n`),
  CONCAT('next action ', `n`),
  DATE_SUB(NOW(), INTERVAL MOD(`n`, 15) DAY),
  @base_org_kpi + (((`n` - 1) MOD 100) + 1),
  CASE WHEN MOD(`n`, 8) = 0 THEN ELT((`n` MOD 3) + 1, 'ENVIRONMENTAL', 'SOCIAL', 'GOVERNANCE') ELSE NULL END,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 360;

INSERT INTO `campaign_kpi_contribution`
(`idx`, `campaign_id`, `target_org_kpi_id`, `committed_value`, `actual_value`, `created_at`, `updated_at`)
SELECT
  @base_contribution + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  @base_org_kpi + (((`n` - 1) MOD 100) + 1),
  500 + (`n` * 10),
  100 + (`n` * 4),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 240;

INSERT INTO `campaign_invitations`
(`idx`, `campaign_idx`, `inviter_idx`, `invitee_idx`, `invitee_organization_idx`, `status`, `type`,
 `responded_at`, `create_date`, `update_date`)
SELECT
  @base_invitation + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  @base_user + (((`n` - 1) MOD 100) + 1),
  @base_user + (((`n` + 7) MOD 100) + 1),
  @base_org + (((`n` + 7) MOD 20) + 1),
  ELT(((`n` - 1) MOD 3) + 1, 'PENDING', 'ACCEPTED', 'REJECTED'),
  CASE WHEN MOD(`n`, 5) = 0 THEN 'GROUP' ELSE 'INDIVIDUAL' END,
  CASE WHEN MOD(`n`, 3) = 1 THEN NULL ELSE DATE_SUB(NOW(), INTERVAL MOD(`n`, 10) DAY) END,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 240;

INSERT INTO `mile_stones`
(`idx`, `name`, `campaign_id`, `start_date`, `end_date`, `description`, `sort_order`, `created_at`, `updated_at`)
SELECT
  @base_milestone + `n`,
  CONCAT('Milestone ', LPAD(`n`, 3, '0')),
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  DATE_ADD(NOW(), INTERVAL MOD(`n`, 20) DAY),
  DATE_ADD(NOW(), INTERVAL 10 + MOD(`n`, 40) DAY),
  CONCAT('Milestone description ', `n`),
  ((`n` - 1) MOD 5),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 240;

INSERT INTO `task_parts`
(`idx`, `campaign_id`, `milestone_id`, `owner_team_id`, `name`, `review_flow`, `task_priority`, `dependency`,
 `deliverable`, `description`, `created_at`, `updated_at`, `sort_order`)
SELECT
  @base_task_part + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  @base_milestone + (((`n` - 1) MOD 240) + 1),
  @base_participant + (((`n` - 1) MOD 360) + 1),
  CONCAT('Task Part ', LPAD(`n`, 3, '0')),
  ELT(((`n` - 1) MOD 4) + 1, 'PM review', 'Legal review', 'Brand review', 'Partner review'),
  ELT(((`n` - 1) MOD 4) + 1, 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'),
  CONCAT('Dependency ', MOD(`n`, 12)),
  CONCAT('Deliverable ', `n`),
  CONCAT('Task part description ', `n`),
  NOW(),
  NOW(),
  ((`n` - 1) MOD 10)
FROM `_dumass_seq` WHERE `n` <= 480;

INSERT INTO `task`
(`idx`, `name`, `participant_id`, `due_date`, `task_type`, `status`, `task_part_id`, `milestone_id`,
 `assignee_id`, `priority`, `memo`, `created_at`, `updated_at`)
SELECT
  @base_task + `n`,
  CONCAT('Dumass Task ', LPAD(`n`, 4, '0')),
  @base_participant + (((`n` - 1) MOD 360) + 1),
  DATE_ADD(NOW(), INTERVAL MOD(`n`, 60) DAY),
  ELT(((`n` - 1) MOD 6) + 1, 'DOCUMENT', 'DESIGN', 'VIDEO', 'MEETING', 'REVIEW', 'OTHER'),
  ELT(((`n` - 1) MOD 6) + 1, 'BACKLOG', 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'BLOCKED'),
  @base_task_part + (((`n` - 1) MOD 480) + 1),
  @base_milestone + (((`n` - 1) MOD 240) + 1),
  @base_user + (((`n` - 1) MOD 100) + 1),
  ELT(((`n` - 1) MOD 4) + 1, 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'),
  CONCAT('Task memo ', `n`),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 1200;

INSERT INTO `campaign_goal`
(`idx`, `name`, `primary_type`, `secondary_type`, `kpi_primary`, `kpi_secondary`, `budget_limit`, `effort_limit`,
 `period_start`, `period_end`, `weight_revenue`, `weight_effort`, `weight_brand`, `owner_idx`,
 `owner_organization_idx`, `owner_label`, `status`, `created_at`)
SELECT
  @base_goal + `n`,
  CONCAT('Matching Goal ', LPAD(`n`, 3, '0')),
  ELT(((`n` - 1) MOD 10) + 1, 'NEW_CUSTOMER', 'CUSTOMER_REVISIT', 'MEMBER_SIGNUP', 'PURCHASE_BOOKING', 'BRAND_AWARENESS', 'REVENUE', 'UPSELL', 'DIRECT_BOOKING', 'REVIEW_REPUTATION', 'OTHER'),
  ELT((`n` MOD 10) + 1, 'NEW_CUSTOMER', 'CUSTOMER_REVISIT', 'MEMBER_SIGNUP', 'PURCHASE_BOOKING', 'BRAND_AWARENESS', 'REVENUE', 'UPSELL', 'DIRECT_BOOKING', 'REVIEW_REPUTATION', 'OTHER'),
  ELT(((`n` - 1) MOD 4) + 1, 'Revenue', 'Signups', 'Reach', 'Reviews'),
  ELT(((`n` - 1) MOD 4) + 1, 'CPA', 'ROAS', 'NPS', 'Conversion'),
  CONCAT(100 + `n`, 'M'),
  CONCAT(5 + MOD(`n`, 20), 'MD'),
  DATE_SUB(CURDATE(), INTERVAL MOD(`n`, 30) DAY),
  DATE_ADD(CURDATE(), INTERVAL 60 + MOD(`n`, 90) DAY),
  30 + MOD(`n`, 40),
  20 + MOD(`n`, 30),
  20 + MOD(`n`, 50),
  @base_user + (((`n` - 1) MOD 100) + 1),
  @base_org + (((`n` - 1) MOD 20) + 1),
  CONCAT('Goal Owner ', `n`),
  ELT(((`n` - 1) MOD 4) + 1, 'draft', 'active', 'paused', 'done'),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 120;

INSERT INTO `partner_benefits`
(`idx`, `affiliation_id`, `campaign_idx`, `name`, `type`, `description`, `quantity`, `quantity_unit`,
 `value_per_person`, `total_value`, `period_start`, `period_end`, `always_negotiable`, `prep_days`,
 `target_audience`, `expected_reach`, `cost_bearer`, `cost_partner_percent`, `cost_ours_percent`,
 `cost_details`, `exposure_channels`, `required_collaborations`, `conditions`, `desired_assets`,
 `auto_recommend`, `manager_name`, `manager_email`, `manager_phone`, `status`, `created_at`)
SELECT
  @base_benefit + `n`,
  @base_org + (((`n` - 1) MOD 20) + 1),
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  CONCAT('Partner Benefit ', LPAD(`n`, 3, '0')),
  ELT(((`n` - 1) MOD 5) + 1, 'DISCOUNT', 'COUPON', 'SPACE', 'CONTENT', 'SAMPLE'),
  CONCAT('Partner benefit description ', `n`),
  100 + (`n` * 2),
  ELT(((`n` - 1) MOD 3) + 1, 'ea', 'people', 'slots'),
  10000 + (`n` * 100),
  (100 + (`n` * 2)) * (10000 + (`n` * 100)),
  DATE_SUB(CURDATE(), INTERVAL MOD(`n`, 20) DAY),
  DATE_ADD(CURDATE(), INTERVAL 30 + MOD(`n`, 70) DAY),
  MOD(`n`, 2) = 0,
  3 + MOD(`n`, 14),
  ELT(((`n` - 1) MOD 5) + 1, 'VIP', 'Family', 'Youth', 'Business', 'Local'),
  1000 + (`n` * 50),
  ELT(((`n` - 1) MOD 3) + 1, 'PARTNER', 'HANWHA', 'SHARED'),
  30 + MOD(`n`, 50),
  70 - MOD(`n`, 50),
  CONCAT('Cost detail ', `n`),
  'SNS, APP, ONSITE',
  'Creative review and weekly reporting',
  'Available during campaign period',
  'CRM segment and media placement',
  MOD(`n`, 2) = 1,
  CONCAT('Manager ', `n`),
  CONCAT('benefit.manager', `n`, '@example.com'),
  CONCAT('010-55', LPAD(MOD(`n`, 100), 2, '0'), '-', LPAD(3000 + `n`, 4, '0')),
  ELT(((`n` - 1) MOD 4) + 1, 'READY', 'MATCHED', 'NEGOTIATING', 'ARCHIVED'),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `marketing_asset`
(`idx`, `owner_idx`, `campaign_idx`, `affiliate`, `category`, `conditions`, `custom_affiliate`, `exposure_value`,
 `matching_status`, `performance`, `public_status`, `scale`, `supply_limit`, `target`, `type`, `registered_at`)
SELECT
  @base_asset + `n`,
  @base_org + (((`n` - 1) MOD 20) + 1),
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  CONCAT('Affiliate ', (((`n` - 1) MOD 20) + 1)),
  ELT(((`n` - 1) MOD 5) + 1, 'MEDIA', 'CRM', 'SPACE', 'EVENT', 'CONTENT'),
  CONCAT('Asset condition ', `n`),
  CONCAT('Custom affiliate ', `n`),
  CONCAT(1000 + `n` * 3, ' impressions/day'),
  ELT(((`n` - 1) MOD 4) + 1, 'READY', 'MATCHED', 'REVIEW', 'HOLD'),
  CONCAT('CTR ', ROUND(1 + MOD(`n`, 70) / 10, 1), '%'),
  ELT(((`n` - 1) MOD 3) + 1, 'PUBLIC', 'PRIVATE', 'LIMITED'),
  ELT(((`n` - 1) MOD 4) + 1, 'SMALL', 'MEDIUM', 'LARGE', 'ENTERPRISE'),
  CONCAT(100 + MOD(`n`, 900), ' units'),
  ELT(((`n` - 1) MOD 5) + 1, 'VIP', 'Family', 'Youth', 'Business', 'Local'),
  ELT(((`n` - 1) MOD 5) + 1, 'BANNER', 'PUSH', 'BOOTH', 'COUPON', 'ARTICLE'),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 240;

INSERT INTO `customer_eval`
(`idx`, `overall_score`, `customer_age_group`, `customer_spending_patterns`, `membership_tier`, `usage_channel`, `benefit_category`)
SELECT @base_customer_eval + `n`, 50 + MOD(`n`, 50), '20-40', 'moderate to high spend', 'GOLD', 'APP', 'DISCOUNT'
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `revenue_eval`
(`idx`, `overall_score`, `purchase_conversion_probability`, `room_reservation_increase_probability`,
 `app_registration_increase_probability`, `membership_registration_revisit_probability`)
SELECT @base_revenue_eval + `n`, 45 + MOD(`n`, 55), 'medium', 'high', 'medium', 'high'
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `cost_eval`
(`idx`, `overall_score`, `partner_sample_scale`, `partner_discount_cost_burden`, `co_production_cost_sharing`,
 `hanwha_direct_cost_burden`, `existing_hanwha_channel_utilization`)
SELECT @base_cost_eval + `n`, 40 + MOD(`n`, 60), 'manageable', 'shared', 'balanced', 'low', 'available'
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `operation_eval`
(`idx`, `overall_score`, `approval_steps_count`, `legal_review_required`, `brand_review_required`,
 `deliverables_count`, `participating_depts_and_partners`, `schedule_urgency`, `offline_or_onsite_staff_required`)
SELECT
  @base_operation_eval + `n`,
  42 + MOD(`n`, 58),
  '3',
  CASE WHEN MOD(`n`, 2) = 0 THEN 'true' ELSE 'false' END,
  'true',
  '5',
  'marketing, legal, partner',
  'normal',
  CASE WHEN MOD(`n`, 3) = 0 THEN 'true' ELSE 'false' END
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `brand_eval`
(`idx`, `overall_score`, `brand_tone`, `price_range`, `customer_experience`, `brand_trust`, `reputation_risk`, `hanwha_image_consistency`)
SELECT @base_brand_eval + `n`, 48 + MOD(`n`, 52), 'premium but friendly', 'mid-high', 'smooth', 'high', 'low', 'consistent'
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `customer_improvement_directions` (`eval_id`, `direction_text`)
SELECT @base_customer_eval + (((`n` - 1) MOD 300) + 1), CONCAT('Customer improvement direction ', `n`)
FROM `_dumass_seq` WHERE `n` <= 600;

INSERT INTO `revenue_improvement_directions` (`eval_id`, `direction_text`)
SELECT @base_revenue_eval + (((`n` - 1) MOD 300) + 1), CONCAT('Revenue improvement direction ', `n`)
FROM `_dumass_seq` WHERE `n` <= 600;

INSERT INTO `cost_improvement_directions` (`eval_id`, `direction_text`)
SELECT @base_cost_eval + (((`n` - 1) MOD 300) + 1), CONCAT('Cost improvement direction ', `n`)
FROM `_dumass_seq` WHERE `n` <= 600;

INSERT INTO `operation_improvement_directions` (`eval_id`, `direction_text`)
SELECT @base_operation_eval + (((`n` - 1) MOD 300) + 1), CONCAT('Operation improvement direction ', `n`)
FROM `_dumass_seq` WHERE `n` <= 600;

INSERT INTO `brand_improvement_directions` (`eval_id`, `direction_text`)
SELECT @base_brand_eval + (((`n` - 1) MOD 300) + 1), CONCAT('Brand improvement direction ', `n`)
FROM `_dumass_seq` WHERE `n` <= 600;

INSERT INTO `evaluation`
(`idx`, `session_id`, `benefit_idx`, `campaign_idx`, `customer_idx`, `revenue_idx`, `cost_idx`,
 `operation_idx`, `brand_idx`, `started_at`, `ended_at`)
SELECT
  @base_evaluation + `n`,
  CONCAT('dumass-session-', LPAD(`n`, 4, '0')),
  @base_benefit + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  @base_customer_eval + `n`,
  @base_revenue_eval + `n`,
  @base_cost_eval + `n`,
  @base_operation_eval + `n`,
  @base_brand_eval + `n`,
  DATE_SUB(NOW(), INTERVAL MOD(`n`, 20) DAY),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `reference_items`
(`idx`, `owner_login_id`, `type`, `title`, `url`, `thumbnail`, `description`, `channel`,
 `objective`, `status`, `reference_date`, `create_date`, `update_date`)
SELECT
  @base_reference + `n`,
  CONCAT('dumass_user_', LPAD(((`n` - 1) MOD 100) + 1, 3, '0')),
  ELT(((`n` - 1) MOD 5) + 1, 'ARTICLE', 'VIDEO', 'SNS', 'REPORT', 'CASE_STUDY'),
  CONCAT('Reference Item ', LPAD(`n`, 3, '0')),
  CONCAT('https://example.com/references/', `n`),
  CONCAT('https://example.com/thumbs/', `n`, '.jpg'),
  CONCAT('Reference description ', `n`),
  ELT(((`n` - 1) MOD 5) + 1, 'Instagram', 'YouTube', 'Blog', 'Newsletter', 'Offline'),
  ELT(((`n` - 1) MOD 5) + 1, 'Awareness', 'Conversion', 'Retention', 'Lead', 'Brand'),
  ELT(((`n` - 1) MOD 4) + 1, 'draft', 'active', 'archived', 'review'),
  DATE_SUB(CURDATE(), INTERVAL MOD(`n`, 365) DAY),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `reference_tag` (`reference_idx`, `tag`)
SELECT
  @base_reference + (((`n` - 1) MOD 300) + 1),
  ELT(((`n` - 1) MOD 10) + 1, 'hotel', 'coupon', 'loyalty', 'premium', 'food', 'family', 'app', 'event', 'esg', 'brand')
FROM `_dumass_seq` WHERE `n` <= 900;

INSERT INTO `notifications`
(`idx`, `recipient_idx`, `sender_idx`, `type`, `severity`, `title`, `message`, `detail`, `target_label`,
 `target_url`, `dedupe_key`, `reference_type`, `reference_id`, `reference_status`, `is_read`, `read_at`,
 `create_date`, `update_date`)
SELECT
  @base_notification + `n`,
  @base_user + (((`n` - 1) MOD 100) + 1),
  CASE WHEN MOD(`n`, 7) = 0 THEN NULL ELSE @base_user + (((`n` + 11) MOD 100) + 1) END,
  ELT(((`n` - 1) MOD 14) + 1, 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED', 'TASK_UPDATED', 'REVIEW_REQUESTED',
      'REVIEW_APPROVED', 'REVIEW_REJECTED', 'DEADLINE_24H', 'DEADLINE_1H', 'DEADLINE_OVERDUE',
      'CAMPAIGN_INVITED', 'CAMPAIGN_INVITATION_ACCEPTED', 'CAMPAIGN_INVITATION_REJECTED',
      'CAMPAIGN_MEMBER_ADDED', 'SYSTEM'),
  ELT(((`n` - 1) MOD 4) + 1, 'LOW', 'NORMAL', 'HIGH', 'CRITICAL'),
  CONCAT('Dumass Notification ', LPAD(`n`, 4, '0')),
  CONCAT('Notification message ', `n`),
  CONCAT('Notification detail ', `n`),
  CONCAT('Target ', MOD(`n`, 120) + 1),
  CONCAT('/campaigns/', @base_campaign + (((`n` - 1) MOD 120) + 1)),
  CONCAT('dumass-notification-', LPAD(`n`, 4, '0')),
  ELT(((`n` - 1) MOD 4) + 1, 'TASK', 'CAMPAIGN', 'REVIEW', 'SYSTEM'),
  CASE WHEN MOD(`n`, 4) = 1 THEN @base_task + (((`n` - 1) MOD 1200) + 1)
       ELSE @base_campaign + (((`n` - 1) MOD 120) + 1) END,
  ELT(((`n` - 1) MOD 4) + 1, 'OPEN', 'DONE', 'PENDING', 'INFO'),
  MOD(`n`, 3) = 0,
  CASE WHEN MOD(`n`, 3) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(`n`, 10) DAY) ELSE NULL END,
  DATE_SUB(NOW(), INTERVAL MOD(`n`, 30) DAY),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 1000;

INSERT INTO `notification_admin_policies`
(`idx`, `organization_idx`, `role_name`, `notification_type`, `enabled`, `create_date`, `update_date`)
SELECT
  @base_policy + `n`,
  @base_org + (((`n` - 1) MOD 20) + 1),
  ELT((FLOOR((`n` - 1) / 20) MOD 4) + 1, 'ALL', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_GENERAL_MANAGER'),
  ELT(((`n` - 1) MOD 14) + 1, 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED', 'TASK_UPDATED', 'REVIEW_REQUESTED',
      'REVIEW_APPROVED', 'REVIEW_REJECTED', 'DEADLINE_24H', 'DEADLINE_1H', 'DEADLINE_OVERDUE',
      'CAMPAIGN_INVITED', 'CAMPAIGN_INVITATION_ACCEPTED', 'CAMPAIGN_INVITATION_REJECTED',
      'CAMPAIGN_MEMBER_ADDED', 'SYSTEM'),
  MOD(`n`, 9) <> 0,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 280;

INSERT INTO `campaign_frame`
(`idx`, `id`, `owner_idx`, `category`, `version`, `title`, `score`, `status`, `overview`, `tone_guide`,
 `usage_count`, `pass_rate`, `avg_revisions`)
SELECT
  @base_frame + `n`,
  CONCAT('dumass-frame-', LPAD(`n`, 3, '0')),
  @base_user + (((`n` - 1) MOD 100) + 1),
  ELT(((`n` - 1) MOD 5) + 1, 'PROMOTION', 'LEGAL', 'BRAND', 'EVENT', 'CRM'),
  CONCAT('v', 1 + MOD(`n`, 3), '.0'),
  CONCAT('Dumass Campaign Frame ', LPAD(`n`, 3, '0')),
  60 + MOD(`n`, 40),
  ELT(((`n` - 1) MOD 4) + 1, 'DRAFT', 'ACTIVE', 'REVIEW', 'ARCHIVED'),
  CONCAT('Frame overview ', `n`),
  CONCAT('Tone guide ', `n`, ': clear, factual, compliant.'),
  `n` * 2,
  70 + MOD(`n`, 30),
  ROUND(0.5 + MOD(`n`, 12) / 10, 2)
FROM `_dumass_seq` WHERE `n` <= 100;

INSERT INTO `campaign_frame_required_field` (`frame_idx`, `sort_order`, `required_field`)
SELECT
  @base_frame + (((`n` - 1) MOD 100) + 1),
  FLOOR((`n` - 1) / 100),
  ELT((FLOOR((`n` - 1) / 100) MOD 3) + 1, 'campaign objective', 'target audience', 'legal disclaimer')
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `campaign_frame_banned_expression` (`frame_idx`, `sort_order`, `banned_expression`)
SELECT
  @base_frame + (((`n` - 1) MOD 100) + 1),
  FLOOR((`n` - 1) / 100),
  ELT((FLOOR((`n` - 1) / 100) MOD 3) + 1, 'guaranteed profit', 'risk-free', 'best in the world')
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `campaign_frame_recommended_expression` (`frame_idx`, `sort_order`, `recommended_expression`)
SELECT
  @base_frame + (((`n` - 1) MOD 100) + 1),
  FLOOR((`n` - 1) / 100),
  ELT((FLOOR((`n` - 1) / 100) MOD 3) + 1, 'limited period benefit', 'member exclusive', 'subject to conditions')
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `campaign_frame_approval_process` (`frame_idx`, `sort_order`, `approval_step`)
SELECT
  @base_frame + (((`n` - 1) MOD 100) + 1),
  FLOOR((`n` - 1) / 100),
  ELT((FLOOR((`n` - 1) / 100) MOD 3) + 1, 'PM review', 'Brand review', 'Legal approval')
FROM `_dumass_seq` WHERE `n` <= 300;

INSERT INTO `ad_review_request`
(`idx`, `campaign_idx`, `file_name`, `file_object_key`, `file_content_type`, `file_size`, `extracted_text`,
 `request_status`, `ai_status`, `law`, `violation_text`, `reason`, `suggestion`, `request_memo`,
 `requester_login_id`, `requester_name`, `requester_organization_idx`, `requester_organization_name`,
 `reviewer_login_id`, `reviewer_name`, `reviewed_at`, `review_memo`, `reject_reason`, `create_date`, `update_date`)
SELECT
  @base_ad_request + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  CONCAT('ad-review-', LPAD(`n`, 3, '0'), '.pdf'),
  CONCAT('adcheck/dumass/', LPAD(`n`, 3, '0'), '.pdf'),
  'application/pdf',
  204800 + (`n` * 1024),
  CONCAT('Extracted ad text for request ', `n`),
  ELT(((`n` - 1) MOD 4) + 1, 'REQUESTED', 'APPROVED', 'REJECTED', 'IN_REVIEW'),
  ELT(((`n` - 1) MOD 3) + 1, 'PENDING', 'PASS', 'FAIL'),
  CASE WHEN MOD(`n`, 4) = 0 THEN 'Display Advertising Act' ELSE NULL END,
  CASE WHEN MOD(`n`, 4) = 0 THEN 'Potential absolute claim' ELSE NULL END,
  CASE WHEN MOD(`n`, 4) = 0 THEN 'Claim requires evidence' ELSE NULL END,
  CASE WHEN MOD(`n`, 4) = 0 THEN 'Add qualifying condition' ELSE NULL END,
  CONCAT('Review memo request ', `n`),
  CONCAT('dumass_user_', LPAD(((`n` - 1) MOD 100) + 1, 3, '0')),
  CONCAT('Dumass User ', LPAD(((`n` - 1) MOD 100) + 1, 3, '0')),
  @base_org + (((`n` - 1) MOD 20) + 1),
  CONCAT('Dumass Organization ', LPAD(((`n` - 1) MOD 20) + 1, 2, '0')),
  CASE WHEN MOD(`n`, 4) IN (2,3) THEN 'dumass_user_001' ELSE NULL END,
  CASE WHEN MOD(`n`, 4) IN (2,3) THEN 'Dumass User 001' ELSE NULL END,
  CASE WHEN MOD(`n`, 4) IN (2,3) THEN NOW() ELSE NULL END,
  CASE WHEN MOD(`n`, 4) = 2 THEN 'approved in dummy review' ELSE NULL END,
  CASE WHEN MOD(`n`, 4) = 3 THEN 'needs revision in dummy review' ELSE NULL END,
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 80;

INSERT INTO `ad_analysis_document`
(`idx`, `review_request_idx`, `campaign_idx`, `file_name`, `file_object_key`, `analysis_status`,
 `total_pages`, `total_regions`, `total_issues`, `layout_model`, `ocr_model`, `detector_model`, `llm_model`,
 `summary`, `raw_payload`, `create_date`, `update_date`)
SELECT
  @base_ad_document + `n`,
  @base_ad_request + `n`,
  @base_campaign + (((`n` - 1) MOD 120) + 1),
  CONCAT('ad-analysis-', LPAD(`n`, 3, '0'), '.pdf'),
  CONCAT('adcheck/analysis/dumass/', LPAD(`n`, 3, '0'), '.pdf'),
  ELT(((`n` - 1) MOD 3) + 1, 'COMPLETED', 'PROCESSING', 'FAILED'),
  2,
  4,
  3,
  'layout-dummy-v1',
  'ocr-dummy-v1',
  'detector-dummy-v1',
  'llm-dummy-v1',
  CONCAT('Analysis summary ', `n`),
  JSON_OBJECT('document', `n`, 'status', 'dummy'),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 80;

INSERT INTO `ad_analysis_page`
(`idx`, `document_idx`, `page_no`, `width`, `height`, `thumbnail_object_key`, `create_date`, `update_date`)
SELECT
  @base_ad_page + `n`,
  @base_ad_document + (((`n` - 1) MOD 80) + 1),
  FLOOR((`n` - 1) / 80) + 1,
  1240,
  1754,
  CONCAT('adcheck/thumbs/dumass-page-', LPAD(`n`, 3, '0'), '.jpg'),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 160;

INSERT INTO `ad_analysis_region`
(`idx`, `page_idx`, `region_key`, `region_type`, `order_index`, `x`, `y`, `width`, `height`,
 `confidence`, `extracted_text`, `labels_json`, `create_date`, `update_date`)
SELECT
  @base_ad_region + `n`,
  @base_ad_page + (((`n` - 1) MOD 160) + 1),
  CONCAT('region-', LPAD(`n`, 4, '0')),
  ELT(((`n` - 1) MOD 4) + 1, 'HEADLINE', 'BODY', 'CTA', 'LEGAL'),
  ((`n` - 1) MOD 4),
  ROUND(MOD(`n` * 7, 900) / 10, 2),
  ROUND(MOD(`n` * 11, 1200) / 10, 2),
  ROUND(100 + MOD(`n`, 300), 2),
  ROUND(40 + MOD(`n`, 160), 2),
  ROUND(0.65 + MOD(`n`, 35) / 100, 2),
  CONCAT('Region extracted text ', `n`),
  JSON_OBJECT('labels', JSON_ARRAY('dummy', 'ad', 'region')),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 320;

INSERT INTO `ad_analysis_issue`
(`idx`, `document_idx`, `page_no`, `region_key`, `issue_type`, `severity`, `issue_status`, `x`, `y`,
 `width`, `height`, `target_text`, `law`, `reason`, `suggestion`, `create_date`, `update_date`)
SELECT
  @base_ad_issue + `n`,
  @base_ad_document + (((`n` - 1) MOD 80) + 1),
  (((`n` - 1) MOD 2) + 1),
  CONCAT('region-', LPAD(((`n` - 1) MOD 320) + 1, 4, '0')),
  ELT(((`n` - 1) MOD 4) + 1, 'LEGAL', 'CLAIM', 'TYPO', 'BRAND'),
  ELT(((`n` - 1) MOD 4) + 1, 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'),
  ELT(((`n` - 1) MOD 3) + 1, 'OPEN', 'RESOLVED', 'IGNORED'),
  ROUND(MOD(`n` * 5, 900) / 10, 2),
  ROUND(MOD(`n` * 13, 1200) / 10, 2),
  120,
  60,
  CONCAT('Issue target text ', `n`),
  CASE WHEN MOD(`n`, 3) = 0 THEN 'Advertising Compliance Rule' ELSE NULL END,
  CONCAT('Issue reason ', `n`),
  CONCAT('Issue suggestion ', `n`),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 240;

INSERT INTO `ad_analysis_keyword`
(`idx`, `document_idx`, `keyword`, `source`, `weight`, `create_date`, `update_date`)
SELECT
  @base_ad_keyword + `n`,
  @base_ad_document + (((`n` - 1) MOD 80) + 1),
  ELT(((`n` - 1) MOD 10) + 1, 'coupon', 'hotel', 'premium', 'membership', 'limited', 'family', 'event', 'brand', 'benefit', 'review'),
  ELT(((`n` - 1) MOD 3) + 1, 'OCR', 'LLM', 'RULE'),
  ROUND(0.1 + MOD(`n`, 90) / 100, 2),
  NOW(),
  NOW()
FROM `_dumass_seq` WHERE `n` <= 240;

DROP TEMPORARY TABLE IF EXISTS `_dumass_seq`;
DROP TEMPORARY TABLE IF EXISTS `_dumass_digits`;

COMMIT;

SELECT 'dumass users' AS `metric`, COUNT(*) AS `rows` FROM `user` WHERE `idx` BETWEEN @base_user + 1 AND @base_user + 100
UNION ALL SELECT 'dumass campaigns', COUNT(*) FROM `campaigns` WHERE `idx` BETWEEN @base_campaign + 1 AND @base_campaign + 120
UNION ALL SELECT 'dumass tasks', COUNT(*) FROM `task` WHERE `idx` BETWEEN @base_task + 1 AND @base_task + 1200
UNION ALL SELECT 'dumass notifications', COUNT(*) FROM `notifications` WHERE `idx` BETWEEN @base_notification + 1 AND @base_notification + 1000;
