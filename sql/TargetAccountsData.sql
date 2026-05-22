-- Target account experience seed.
-- Standalone and safe to rerun.
-- Accounts:
--   hqgm@callog.com
--   partner@callog.com

START TRANSACTION;

SET @target_hq_org_seed := 1180001;
SET @target_partner_org_seed := 1180002;
SET @target_hq_user_seed := 1180101;
SET @target_partner_user_seed := 1180102;
SET @target_profile_seed := 1180200;
SET @target_setting_seed := 1180300;
SET @target_org_kpi := 1181000;
SET @target_daily := 1182000;
SET @target_monthly := 1183000;
SET @target_campaign := 1184000;
SET @target_participant := 1185000;
SET @target_member := 1186000;
SET @target_intro := 1187000;
SET @target_campaign_kpi := 1188000;
SET @target_contribution := 1189000;
SET @target_invitation := 1190000;
SET @target_milestone := 1191000;
SET @target_task_part := 1192000;
SET @target_task := 1193000;
SET @target_benefit := 1195000;
SET @target_asset := 1196000;
SET @target_reference := 1197000;
SET @target_notification := 1198000;
SET @target_frame := 1199000;
SET @target_ad_request := 1200000;
SET @target_ad_document := 1201000;
SET @target_ad_page := 1202000;
SET @target_ad_region := 1203000;
SET @target_ad_issue := 1204000;
SET @target_ad_keyword := 1205000;

CREATE TEMPORARY TABLE IF NOT EXISTS `_target_digits` (`d` INT PRIMARY KEY);
CREATE TEMPORARY TABLE IF NOT EXISTS `_target_seq` (`n` INT PRIMARY KEY);
DELETE FROM `_target_digits`;
DELETE FROM `_target_seq`;

INSERT INTO `_target_digits` (`d`) VALUES
(0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

INSERT INTO `_target_seq` (`n`)
SELECT ones.`d` + tens.`d` * 10 + hundreds.`d` * 100 + 1
FROM `_target_digits` ones
CROSS JOIN `_target_digits` tens
CROSS JOIN `_target_digits` hundreds
WHERE ones.`d` + tens.`d` * 10 + hundreds.`d` * 100 + 1 <= 400;

SET @target_password := '{bcrypt}$2a$10$JCOLDEL/O51kLupcIETqve4Xr7bOHy5uz8Nlvbs5eUscOY/Oj54va';

SET @target_hq_org := (
  SELECT u.`organization_id`
  FROM `user` u
  WHERE u.`email` = 'hqgm@callog.com' OR u.`id` IN ('hqgm@callog.com', 'hqgm')
  ORDER BY u.`idx`
  LIMIT 1
);
SET @target_hq_org := COALESCE(
  @target_hq_org,
  (SELECT o.`idx` FROM `organizations` o WHERE o.`type` = 'HQ' ORDER BY o.`idx` LIMIT 1)
);

INSERT INTO `organizations`
(`idx`, `code`, `name`, `type`, `general_manager_idx`, `can_create_campaign`, `created_at`)
SELECT @target_hq_org_seed, 'TARGET_HQ', 'Headquarters', 'HQ', NULL, TRUE, NOW()
WHERE @target_hq_org IS NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `type` = VALUES(`type`),
  `can_create_campaign` = VALUES(`can_create_campaign`);

SET @target_hq_org := COALESCE(@target_hq_org, @target_hq_org_seed);

SET @target_partner_org := (
  SELECT u.`organization_id`
  FROM `user` u
  WHERE u.`email` = 'partner@callog.com' OR u.`id` IN ('partner@callog.com', 'partner')
  ORDER BY u.`idx`
  LIMIT 1
);
SET @target_partner_org := COALESCE(
  @target_partner_org,
  (SELECT o.`idx` FROM `organizations` o WHERE o.`code` IN ('CALLOG_PARTNER', 'TARGET_PARTNER') ORDER BY o.`idx` LIMIT 1),
  (SELECT o.`idx` FROM `organizations` o WHERE o.`type` = 'EXTERNAL_PARTNER' ORDER BY o.`idx` LIMIT 1)
);

INSERT INTO `organizations`
(`idx`, `code`, `name`, `type`, `general_manager_idx`, `can_create_campaign`, `created_at`)
SELECT @target_partner_org_seed, 'TARGET_PARTNER', 'Callog Partner', 'EXTERNAL_PARTNER', NULL, FALSE, NOW()
WHERE @target_partner_org IS NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `type` = VALUES(`type`),
  `can_create_campaign` = VALUES(`can_create_campaign`);

SET @target_partner_org := COALESCE(@target_partner_org, @target_partner_org_seed);

SET @target_hq_user := (
  SELECT u.`idx`
  FROM `user` u
  WHERE u.`email` = 'hqgm@callog.com' OR u.`id` IN ('hqgm@callog.com', 'hqgm')
  ORDER BY u.`idx`
  LIMIT 1
);

INSERT INTO `user`
(`idx`, `id`, `email`, `name`, `company_name`, `department`, `password`, `enable`, `role`, `organization_id`, `account_status`)
SELECT @target_hq_user_seed, 'hqgm@callog.com', 'hqgm@callog.com', 'HQ General Manager',
       'Callog Headquarters', 'Growth Strategy', @target_password, TRUE,
       'ROLE_GENERAL_MANAGER', @target_hq_org, 'ACTIVE'
WHERE @target_hq_user IS NULL
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `name` = VALUES(`name`),
  `company_name` = VALUES(`company_name`),
  `department` = VALUES(`department`),
  `password` = VALUES(`password`),
  `enable` = VALUES(`enable`),
  `role` = VALUES(`role`),
  `organization_id` = VALUES(`organization_id`),
  `account_status` = VALUES(`account_status`);

SET @target_hq_user := COALESCE(
  @target_hq_user,
  (SELECT u.`idx` FROM `user` u WHERE u.`email` = 'hqgm@callog.com' LIMIT 1),
  @target_hq_user_seed
);

UPDATE `user`
SET `id` = 'hqgm@callog.com',
    `email` = 'hqgm@callog.com',
    `name` = 'HQ General Manager',
    `company_name` = 'Callog Headquarters',
    `department` = 'Growth Strategy',
    `password` = @target_password,
    `enable` = TRUE,
    `role` = 'ROLE_GENERAL_MANAGER',
    `organization_id` = @target_hq_org,
    `account_status` = 'ACTIVE'
WHERE `idx` = @target_hq_user;

SET @target_partner_user := (
  SELECT u.`idx`
  FROM `user` u
  WHERE u.`email` = 'partner@callog.com' OR u.`id` IN ('partner@callog.com', 'partner')
  ORDER BY u.`idx`
  LIMIT 1
);

INSERT INTO `user`
(`idx`, `id`, `email`, `name`, `company_name`, `department`, `password`, `enable`, `role`, `organization_id`, `account_status`)
SELECT @target_partner_user_seed, 'partner@callog.com', 'partner@callog.com', 'Partner General Manager',
       'Callog Partner', 'Alliance Operations', @target_password, TRUE,
       'ROLE_GENERAL_MANAGER', @target_partner_org, 'ACTIVE'
WHERE @target_partner_user IS NULL
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `name` = VALUES(`name`),
  `company_name` = VALUES(`company_name`),
  `department` = VALUES(`department`),
  `password` = VALUES(`password`),
  `enable` = VALUES(`enable`),
  `role` = VALUES(`role`),
  `organization_id` = VALUES(`organization_id`),
  `account_status` = VALUES(`account_status`);

SET @target_partner_user := COALESCE(
  @target_partner_user,
  (SELECT u.`idx` FROM `user` u WHERE u.`email` = 'partner@callog.com' LIMIT 1),
  @target_partner_user_seed
);

UPDATE `user`
SET `id` = 'partner@callog.com',
    `email` = 'partner@callog.com',
    `name` = 'Partner General Manager',
    `company_name` = 'Callog Partner',
    `department` = 'Alliance Operations',
    `password` = @target_password,
    `enable` = TRUE,
    `role` = 'ROLE_GENERAL_MANAGER',
    `organization_id` = @target_partner_org,
    `account_status` = 'ACTIVE'
WHERE `idx` = @target_partner_user;

UPDATE `organizations`
SET `general_manager_idx` = @target_hq_user
WHERE `idx` = @target_hq_org;

UPDATE `organizations`
SET `general_manager_idx` = @target_partner_user
WHERE `idx` = @target_partner_org;

INSERT INTO `user_profile`
(`idx`, `user_idx`, `email`, `phone`, `profile_image_key`, `profile_image_url`, `create_date`, `update_date`)
VALUES
(@target_profile_seed + 1, @target_hq_user, 'hqgm@callog.com', '010-7201-4101', 'profiles/target/hq-gm.png', 'https://cdn.example.com/profiles/target/hq-gm.png', NOW(), NOW()),
(@target_profile_seed + 2, @target_partner_user, 'partner@callog.com', '010-7201-4202', 'profiles/target/partner-gm.png', 'https://cdn.example.com/profiles/target/partner-gm.png', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `phone` = VALUES(`phone`),
  `profile_image_key` = VALUES(`profile_image_key`),
  `profile_image_url` = VALUES(`profile_image_url`),
  `update_date` = NOW();

INSERT INTO `notification_settings`
(`idx`, `user_idx`, `enabled`, `level`, `in_app_enabled`, `email_enabled`, `browser_enabled`,
 `task_assigned_enabled`, `task_status_changed_enabled`, `qa_review_enabled`, `deadline_enabled`,
 `campaign_enabled`, `schedule_enabled`, `create_date`, `update_date`)
VALUES
(@target_setting_seed + 1, @target_hq_user, TRUE, 'ALL', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NOW(), NOW()),
(@target_setting_seed + 2, @target_partner_user, TRUE, 'ALL', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `level` = VALUES(`level`),
  `in_app_enabled` = VALUES(`in_app_enabled`),
  `email_enabled` = VALUES(`email_enabled`),
  `browser_enabled` = VALUES(`browser_enabled`),
  `task_assigned_enabled` = VALUES(`task_assigned_enabled`),
  `task_status_changed_enabled` = VALUES(`task_status_changed_enabled`),
  `qa_review_enabled` = VALUES(`qa_review_enabled`),
  `deadline_enabled` = VALUES(`deadline_enabled`),
  `campaign_enabled` = VALUES(`campaign_enabled`),
  `schedule_enabled` = VALUES(`schedule_enabled`),
  `update_date` = NOW();

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `campaign_tag` WHERE `campaign_idx` BETWEEN @target_campaign + 1 AND @target_campaign + 12;
DELETE FROM `campaign_partner` WHERE `campaign_idx` BETWEEN @target_campaign + 1 AND @target_campaign + 12;
DELETE FROM `campaign_method` WHERE `campaign_idx` BETWEEN @target_campaign + 1 AND @target_campaign + 12;
DELETE FROM `reference_tag` WHERE `reference_idx` BETWEEN @target_reference + 1 AND @target_reference + 24;
DELETE FROM `campaign_frame_required_field` WHERE `frame_idx` BETWEEN @target_frame + 1 AND @target_frame + 6;
DELETE FROM `campaign_frame_banned_expression` WHERE `frame_idx` BETWEEN @target_frame + 1 AND @target_frame + 6;
DELETE FROM `campaign_frame_recommended_expression` WHERE `frame_idx` BETWEEN @target_frame + 1 AND @target_frame + 6;
DELETE FROM `campaign_frame_approval_process` WHERE `frame_idx` BETWEEN @target_frame + 1 AND @target_frame + 6;
DELETE FROM `ad_analysis_issue` WHERE `idx` BETWEEN @target_ad_issue + 1 AND @target_ad_issue + 36;
DELETE FROM `ad_analysis_keyword` WHERE `idx` BETWEEN @target_ad_keyword + 1 AND @target_ad_keyword + 36;
DELETE FROM `ad_analysis_region` WHERE `idx` BETWEEN @target_ad_region + 1 AND @target_ad_region + 48;
DELETE FROM `ad_analysis_page` WHERE `idx` BETWEEN @target_ad_page + 1 AND @target_ad_page + 24;
DELETE FROM `ad_analysis_document` WHERE `idx` BETWEEN @target_ad_document + 1 AND @target_ad_document + 12;
DELETE FROM `ad_review_request` WHERE `idx` BETWEEN @target_ad_request + 1 AND @target_ad_request + 12;
DELETE FROM `campaign_frame` WHERE `idx` BETWEEN @target_frame + 1 AND @target_frame + 6;
DELETE FROM `notifications` WHERE `idx` BETWEEN @target_notification + 1 AND @target_notification + 160;
DELETE FROM `reference_items` WHERE `idx` BETWEEN @target_reference + 1 AND @target_reference + 24;
DELETE FROM `marketing_asset` WHERE `idx` BETWEEN @target_asset + 1 AND @target_asset + 24;
DELETE FROM `partner_benefits` WHERE `idx` BETWEEN @target_benefit + 1 AND @target_benefit + 24;
DELETE FROM `campaign_invitations` WHERE `idx` BETWEEN @target_invitation + 1 AND @target_invitation + 12;
DELETE FROM `campaign_kpi_contribution` WHERE `idx` BETWEEN @target_contribution + 1 AND @target_contribution + 24;
DELETE FROM `campaign_kpis` WHERE `idx` BETWEEN @target_campaign_kpi + 1 AND @target_campaign_kpi + 36;
DELETE FROM `campaign_intro` WHERE `idx` BETWEEN @target_intro + 1 AND @target_intro + 12;
DELETE FROM `task` WHERE `idx` BETWEEN @target_task + 1 AND @target_task + 120;
DELETE FROM `task_parts` WHERE `idx` BETWEEN @target_task_part + 1 AND @target_task_part + 48;
DELETE FROM `mile_stones` WHERE `idx` BETWEEN @target_milestone + 1 AND @target_milestone + 36;
DELETE FROM `campaign_members` WHERE `idx` BETWEEN @target_member + 1 AND @target_member + 24;
DELETE FROM `campaign_participant` WHERE `idx` BETWEEN @target_participant + 1 AND @target_participant + 24;
DELETE FROM `campaigns` WHERE `idx` BETWEEN @target_campaign + 1 AND @target_campaign + 12;
DELETE FROM `kpi_monthly_snapshot` WHERE `idx` BETWEEN @target_monthly + 1 AND @target_monthly + 48;
DELETE FROM `organization_kpi` WHERE `idx` BETWEEN @target_org_kpi + 1 AND @target_org_kpi + 16;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `organization_kpi`
(`idx`, `owner_org_id`, `parent_kpi_id`, `contribution_to_parent`, `name`, `period_type`, `period_code`,
 `period_start`, `period_end`, `target_value`, `actual_value`, `unit`, `category`, `esg_category`, `kind`,
 `status`, `achievability_note`, `template_id`, `previous_version_id`, `created_by`, `created_at`,
 `updated_by`, `updated_at`, `approved_by`, `approved_at`)
SELECT
  @target_org_kpi + `n`,
  CASE WHEN `n` <= 8 THEN @target_hq_org ELSE @target_partner_org END,
  NULL,
  NULL,
  ELT(((`n` - 1) MOD 8) + 1,
      'Q2 Direct Booking Revenue',
      'Member App Revisit Rate',
      'Premium Package Conversion',
      'Partner Benefit Redemption',
      'Brand Search Lift',
      'Review Quality Score',
      'Event Lead Pipeline',
      'Sustainable Stay Participation'),
  'QUARTERLY',
  '2026-Q2',
  DATE('2026-04-01'),
  DATE('2026-06-30'),
  ELT(((`n` - 1) MOD 8) + 1, 180000000, 42, 18, 3500, 24, 92, 780, 1200),
  ELT(((`n` - 1) MOD 8) + 1, 126000000, 31, 12, 2140, 17, 88, 510, 870),
  ELT(((`n` - 1) MOD 8) + 1, 'KRW', 'percent', 'percent', 'count', 'percent', 'score', 'leads', 'participants'),
  ELT(((`n` - 1) MOD 6) + 1, 'REVENUE', 'ENGAGEMENT', 'CONVERSION', 'REVENUE', 'BRAND', 'OTHER'),
  CASE WHEN MOD(`n`, 8) = 0 THEN 'ENVIRONMENTAL' ELSE NULL END,
  ELT(((`n` - 1) MOD 3) + 1, 'STRATEGIC', 'TACTICAL', 'OPERATIONAL'),
  'ACTIVE',
  ELT(((`n` - 1) MOD 4) + 1,
      'Pace is healthy if partner inventory remains stable.',
      'Needs weekly CRM targeting review.',
      'Creative assets are ready, legal review is the main risk.',
      'Strong partner alignment, monitor redemption cost.'),
  NULL,
  NULL,
  CASE WHEN `n` <= 8 THEN @target_hq_user ELSE @target_partner_user END,
  NOW(),
  CASE WHEN `n` <= 8 THEN @target_hq_user ELSE @target_partner_user END,
  NOW(),
  @target_hq_user,
  NOW()
FROM `_target_seq` WHERE `n` <= 16;

INSERT INTO `kpi_daily_snapshot`
(`idx`, `organization_id`, `snapshot_date`, `avg_kpi_percent`, `snapshot_at`)
SELECT
  @target_daily + `n`,
  CASE WHEN `n` <= 7 THEN @target_hq_org ELSE @target_partner_org END,
  DATE_SUB(CURDATE(), INTERVAL CASE WHEN `n` <= 7 THEN 7 - `n` ELSE 14 - `n` END DAY),
  CASE WHEN `n` <= 7 THEN 61 + `n` * 3 ELSE 52 + (`n` - 7) * 4 END,
  NOW()
FROM `_target_seq` WHERE `n` <= 14
ON DUPLICATE KEY UPDATE
  `avg_kpi_percent` = VALUES(`avg_kpi_percent`),
  `snapshot_at` = NOW();

INSERT INTO `kpi_monthly_snapshot`
(`idx`, `org_kpi_id`, `snapshot_year`, `snapshot_month`, `actual_value`, `target_value`, `snapshot_at`)
SELECT
  @target_monthly + `n`,
  @target_org_kpi + (((`n` - 1) MOD 16) + 1),
  2026,
  FLOOR((`n` - 1) / 16) + 4,
  5000 + (`n` * 850),
  9000 + (`n` * 1100),
  NOW()
FROM `_target_seq` WHERE `n` <= 48;

INSERT INTO `campaigns`
(`idx`, `public_id`, `owner_login_id`, `name`, `purpose`, `start_date`, `end_date`, `goals`,
 `asset_name`, `asset_description`, `primary_goal`, `max_cost`, `min_revenue`, `status`, `kpi_analysis`,
 `initials`, `icon`, `color`, `visibility`, `create_date`, `update_date`)
SELECT
  @target_campaign + `n`,
  CONCAT('11840000-0000-4000-8000-', LPAD(`n`, 12, '0')),
  'hqgm@callog.com',
  ELT(`n`,
      'Spring Staycation Rewards',
      'Seoul Gourmet Week Alliance',
      'Airport Lounge Cross Benefit',
      'Family Weekend Pass',
      'Business Traveler Fast Lane',
      'Wellness Retreat Member Upgrade',
      'MICE Lead Generation Sprint',
      'Premium Dining Voucher Exchange',
      'Pet Friendly Stay Launch',
      'Local Culture Night Package',
      'Sustainable Stay ESG Challenge',
      'App Revisit Coupon Journey'),
  ELT(`n`,
      'Bundle weekday room inventory with seasonal dining credits for members.',
      'Drive restaurant discovery through hotel guest and local resident segments.',
      'Connect airport lounge traffic to premium hotel membership benefits.',
      'Increase weekend family bookings with partner activity coupons.',
      'Improve weekday business guest retention with fast-lane benefits.',
      'Lift premium membership upgrades through wellness itinerary packaging.',
      'Build qualified MICE pipeline from corporate travel and venue inquiries.',
      'Exchange dining vouchers with a curated partner network for high-value guests.',
      'Launch a pet-friendly stay package with content and local partner exposure.',
      'Create night-time local culture packages for inbound guests.',
      'Promote low-impact stays and ESG participation with partner rewards.',
      'Bring dormant app users back with sequenced coupons and editorial content.'),
  DATE_ADD(CURDATE(), INTERVAL -(`n` * 3) DAY),
  DATE_ADD(CURDATE(), INTERVAL 35 + (`n` * 4) DAY),
  ELT(((`n` - 1) MOD 4) + 1, 'Direct booking growth and repeat visit lift', 'Partner conversion and brand affinity', 'Member activation and app revisit', 'Lead generation and review quality'),
  ELT(((`n` - 1) MOD 6) + 1, 'Room Package Inventory', 'Dining Voucher Pool', 'Airport Lounge Access', 'Family Activity Coupon', 'Corporate Fast Lane', 'Wellness Program Credit'),
  ELT(((`n` - 1) MOD 4) + 1,
      'Segmented rooms, dining credits, CRM message slots, and partner coupon pool.',
      'Co-branded voucher inventory with landing page and weekly report package.',
      'Premium journey asset bundle including concierge scripts and app placement.',
      'Offline redemption kit, partner benefit guide, and campaign content templates.'),
  ELT(((`n` - 1) MOD 5) + 1, 'Revenue', 'Retention', 'Conversion', 'Brand', 'ESG'),
  CONCAT(FORMAT(18000000 + (`n` * 2500000), 0), ' KRW'),
  CONCAT(FORMAT(52000000 + (`n` * 4200000), 0), ' KRW'),
  ELT(((`n` - 1) MOD 5) + 1, 'live', 'review', 'paused', 'planned', 'completed'),
  ELT(((`n` - 1) MOD 4) + 1,
      'Revenue pace is above plan; redemption mix should be checked twice a week.',
      'Partner traffic is strong but conversion depends on mobile creative refresh.',
      'Awareness metrics are healthy; revenue KPI needs package upsell support.',
      'Lead quality is improving after sales script and audience refinement.'),
  ELT(`n`, 'SR', 'SG', 'AL', 'FW', 'BT', 'WR', 'ML', 'DV', 'PF', 'LC', 'ES', 'AR'),
  ELT(((`n` - 1) MOD 6) + 1, 'gift', 'utensils', 'plane', 'users', 'briefcase', 'sparkles'),
  ELT(((`n` - 1) MOD 8) + 1, '#2563EB', '#059669', '#D97706', '#7C3AED', '#0891B2', '#BE185D', '#4F46E5', '#16A34A'),
  ELT(((`n` - 1) MOD 4) + 1, 'HQ_AND_AFFILIATE', 'ALL', 'EXTERNAL_ONLY', 'HQ_ONLY'),
  NOW(),
  NOW()
FROM `_target_seq` WHERE `n` <= 12;

INSERT INTO `campaign_tag` (`campaign_idx`, `tag`)
SELECT @target_campaign + (((`n` - 1) MOD 12) + 1),
       ELT(((`n` - 1) MOD 12) + 1, 'staycation', 'dining', 'airport', 'family', 'business', 'wellness', 'mice', 'voucher', 'pet', 'culture', 'esg', 'app-revisit')
FROM `_target_seq` WHERE `n` <= 36;

INSERT INTO `campaign_partner` (`campaign_idx`, `partner`)
SELECT @target_campaign + (((`n` - 1) MOD 12) + 1),
       ELT(((`n` - 1) MOD 8) + 1, 'Callog Partner', 'Blue Ribbon Dining', 'Metro Mobility', 'Urban Culture Pass', 'Green Stay Lab', 'PetJoy Studio', 'Summit Events', 'Local Table Network')
FROM `_target_seq` WHERE `n` <= 36;

INSERT INTO `campaign_method` (`campaign_idx`, `method`)
SELECT @target_campaign + (((`n` - 1) MOD 12) + 1),
       ELT(((`n` - 1) MOD 8) + 1, 'APP_PUSH', 'EMAIL', 'ONSITE_QR', 'SNS_CONTENT', 'LANDING_PAGE', 'CONCIERGE_SCRIPT', 'PARTNER_NEWSLETTER', 'EVENT_BOOTH')
FROM `_target_seq` WHERE `n` <= 36;

INSERT INTO `campaign_participant` (`idx`, `campaign_idx`, `organization_idx`, `campaign_role`)
SELECT @target_participant + `n`,
       @target_campaign + FLOOR((`n` - 1) / 2) + 1,
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_org ELSE @target_partner_org END,
       CASE WHEN MOD(`n`, 2) = 1 THEN 'PM' ELSE 'PARTNER' END
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `campaign_members`
(`idx`, `campaign_idx`, `user_idx`, `campaign_role`, `joined_at`, `create_date`, `update_date`)
SELECT @target_member + `n`,
       @target_campaign + FLOOR((`n` - 1) / 2) + 1,
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_user ELSE @target_partner_user END,
       CASE WHEN MOD(`n`, 2) = 1 THEN 'GENERAL_MANAGER' ELSE 'MANAGER' END,
       DATE_SUB(NOW(), INTERVAL 18 - FLOOR((`n` - 1) / 2) DAY),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `campaign_intro`
(`idx`, `campaign_idx`, `rfp_code`, `recruit_deadline`, `hanwha_assets`, `partner_roles`, `customer_tags`,
 `partner_values`, `timeline_events`, `submission_docs`, `attached_files`, `contact_info`, `weight_customer`,
 `weight_revenue`, `weight_cost`, `weight_operation`, `weight_brand`, `overview_items`, `hero_kpis`,
 `target_segment`, `target_scale`, `submission_info`, `customer_items`, `view_count`, `create_date`, `update_date`)
SELECT @target_intro + `n`,
       @target_campaign + `n`,
       CONCAT('RFP-CALLOG-', DATE_FORMAT(CURDATE(), '%y%m'), '-', LPAD(`n`, 3, '0')),
       DATE_ADD(NOW(), INTERVAL 10 + `n` DAY),
       JSON_OBJECT('rooms', 80 + `n` * 8, 'crmSegments', JSON_ARRAY('VIP', 'Dormant', 'Local'), 'mediaSlots', JSON_ARRAY('app home', 'lobby display')),
       JSON_OBJECT('primary', 'benefit provider', 'secondary', 'traffic amplifier', 'weeklyReport', true),
       JSON_OBJECT('tags', JSON_ARRAY('member', 'premium', 'repeat', ELT(((`n` - 1) MOD 4) + 1, 'family', 'business', 'wellness', 'culture'))),
       JSON_OBJECT('values', JSON_ARRAY('incremental revenue', 'brand fit', 'operational reliability')),
       JSON_OBJECT('events', JSON_ARRAY('partner kickoff', 'creative lock', 'soft launch', 'performance review')),
       JSON_OBJECT('docs', JSON_ARRAY('proposal deck', 'benefit sheet', 'legal wording')),
       JSON_OBJECT('files', JSON_ARRAY(CONCAT('campaign-brief-', LPAD(`n`, 2, '0'), '.pdf'), CONCAT('partner-terms-', LPAD(`n`, 2, '0'), '.xlsx'))),
       JSON_OBJECT('owner', 'Growth Strategy', 'email', 'hqgm@callog.com', 'partner', 'partner@callog.com'),
       22 + MOD(`n`, 6), 28 + MOD(`n`, 8), 12 + MOD(`n`, 5), 16 + MOD(`n`, 5), 18 + MOD(`n`, 6),
       JSON_OBJECT('items', JSON_ARRAY('Audience fit', 'Benefit inventory', 'Operational owner', 'Expected lift')),
       JSON_OBJECT('kpis', JSON_ARRAY('booking revenue', 'redemption', 'review score')),
       ELT(((`n` - 1) MOD 5) + 1, 'Premium members', 'Dormant app users', 'Family travelers', 'Business guests', 'Local dining audience'),
       ELT(((`n` - 1) MOD 4) + 1, '8k reachable users', '18k reachable users', '35k reachable users', '60k reachable users'),
       JSON_OBJECT('channel', 'partner portal', 'deadline', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 14 DAY), '%Y-%m-%d')),
       JSON_OBJECT('items', JSON_ARRAY('age band', 'booking history', 'membership tier', 'preferred channel')),
       140 + (`n` * 19),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 12;

INSERT INTO `campaign_kpis`
(`idx`, `campaign_idx`, `name`, `category`, `target_value`, `actual_value`, `unit`, `owner_label`,
 `owner_user_idx`, `memo`, `next_action`, `measured_at`, `parent_org_kpi_id`, `esg_category`, `create_date`, `update_date`)
SELECT @target_campaign_kpi + `n`,
       @target_campaign + FLOOR((`n` - 1) / 3) + 1,
       ELT(((`n` - 1) MOD 6) + 1, 'Booking Revenue', 'Benefit Redemption', 'App Revisit Rate', 'Partner Lead Count', 'Brand Search Lift', 'Review Score'),
       ELT(((`n` - 1) MOD 6) + 1, 'REVENUE', 'CONVERSION', 'ENGAGEMENT', 'CONVERSION', 'BRAND', 'OTHER'),
       ELT(((`n` - 1) MOD 6) + 1, 45000000, 1200, 38, 260, 18, 92),
       ELT(((`n` - 1) MOD 6) + 1, 31750000, 790, 27, 154, 12, 86),
       ELT(((`n` - 1) MOD 6) + 1, 'KRW', 'count', 'percent', 'leads', 'percent', 'score'),
       CASE WHEN MOD(`n`, 3) = 1 THEN 'HQ Growth' WHEN MOD(`n`, 3) = 2 THEN 'Partner Operations' ELSE 'Joint Review' END,
       CASE WHEN MOD(`n`, 3) = 2 THEN @target_partner_user ELSE @target_hq_user END,
       ELT(((`n` - 1) MOD 4) + 1, 'Tracking weekly by channel.', 'Partner inventory is stable.', 'Creative refresh scheduled.', 'Legal wording has been reviewed.'),
       ELT(((`n` - 1) MOD 4) + 1, 'Adjust CRM segment.', 'Refresh landing hero copy.', 'Confirm partner stock.', 'Review underperforming channel.'),
       DATE_SUB(NOW(), INTERVAL MOD(`n`, 7) DAY),
       @target_org_kpi + (((`n` - 1) MOD 16) + 1),
       CASE WHEN MOD(`n`, 12) = 0 THEN 'ENVIRONMENTAL' ELSE NULL END,
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 36;

INSERT INTO `campaign_kpi_contribution`
(`idx`, `campaign_id`, `target_org_kpi_id`, `committed_value`, `actual_value`, `created_at`, `updated_at`)
SELECT @target_contribution + `n`,
       @target_campaign + FLOOR((`n` - 1) / 2) + 1,
       CASE WHEN MOD(`n`, 2) = 1
            THEN @target_org_kpi + (((FLOOR((`n` - 1) / 2)) MOD 8) + 1)
            ELSE @target_org_kpi + 8 + (((FLOOR((`n` - 1) / 2)) MOD 8) + 1)
       END,
       8000 + (`n` * 350),
       4200 + (`n` * 210),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `campaign_invitations`
(`idx`, `campaign_idx`, `inviter_idx`, `invitee_idx`, `invitee_organization_idx`, `status`, `type`,
 `responded_at`, `create_date`, `update_date`)
SELECT @target_invitation + `n`,
       @target_campaign + `n`,
       @target_hq_user,
       @target_partner_user,
       @target_partner_org,
       ELT(((`n` - 1) MOD 3) + 1, 'ACCEPTED', 'PENDING', 'ACCEPTED'),
       CASE WHEN MOD(`n`, 4) = 0 THEN 'GROUP' ELSE 'INDIVIDUAL' END,
       CASE WHEN MOD(`n`, 3) = 2 THEN NULL ELSE DATE_SUB(NOW(), INTERVAL MOD(`n`, 5) DAY) END,
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 12;

INSERT INTO `mile_stones`
(`idx`, `name`, `campaign_id`, `start_date`, `end_date`, `description`, `sort_order`, `created_at`, `updated_at`)
SELECT @target_milestone + `n`,
       ELT(((`n` - 1) MOD 3) + 1, 'Partner Kickoff', 'Creative and Legal Review', 'Launch and Optimization'),
       @target_campaign + FLOOR((`n` - 1) / 3) + 1,
       DATE_ADD(NOW(), INTERVAL -6 + MOD(`n`, 9) DAY),
       DATE_ADD(NOW(), INTERVAL 8 + MOD(`n`, 16) DAY),
       ELT(((`n` - 1) MOD 3) + 1,
           'Align target audience, benefit inventory, and reporting owner.',
           'Finalize offer wording, visual assets, and compliance checklist.',
           'Monitor performance by channel and update weekly actions.'),
       ((`n` - 1) MOD 3),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 36;

INSERT INTO `task_parts`
(`idx`, `campaign_id`, `milestone_id`, `owner_team_id`, `name`, `review_flow`, `task_priority`, `dependency`,
 `deliverable`, `description`, `created_at`, `updated_at`, `sort_order`)
SELECT @target_task_part + `n`,
       @target_campaign + FLOOR((`n` - 1) / 4) + 1,
       @target_milestone + (((`n` - 1) MOD 36) + 1),
       @target_participant + (((FLOOR((`n` - 1) / 4)) * 2) + CASE WHEN MOD(`n`, 4) IN (1,2) THEN 1 ELSE 2 END),
       ELT(((`n` - 1) MOD 4) + 1, 'Audience and Offer Setup', 'Creative Production', 'Partner Operations', 'Performance Review'),
       ELT(((`n` - 1) MOD 4) + 1, 'PM > Partner', 'Brand > Legal > PM', 'Partner > PM', 'Data > PM > Partner'),
       ELT(((`n` - 1) MOD 4) + 1, 'HIGH', 'MEDIUM', 'HIGH', 'CRITICAL'),
       ELT(((`n` - 1) MOD 4) + 1, 'Confirmed audience segment', 'Approved benefit terms', 'Partner inventory sheet', 'Weekly performance extract'),
       ELT(((`n` - 1) MOD 4) + 1, 'Targeting brief', 'Creative set', 'Operation checklist', 'Performance memo'),
       ELT(((`n` - 1) MOD 4) + 1,
           'Define eligible users and exclusion rules before launch.',
           'Produce message, banner, and landing copy with compliant expressions.',
           'Prepare redemption flow, support FAQ, and escalation owner.',
           'Review channel results and decide next-week action.'),
       NOW(),
       NOW(),
       ((`n` - 1) MOD 4)
FROM `_target_seq` WHERE `n` <= 48;

INSERT INTO `task`
(`idx`, `name`, `participant_id`, `due_date`, `task_type`, `status`, `task_part_id`, `milestone_id`,
 `assignee_id`, `priority`, `memo`, `created_at`, `updated_at`)
SELECT @target_task + `n`,
       ELT(((`n` - 1) MOD 10) + 1,
           'Confirm audience exclusion list',
           'Lock benefit terms with partner',
           'Review landing page compliance',
           'Prepare CRM push copy variants',
           'Upload partner inventory sheet',
           'QA coupon redemption flow',
           'Check dashboard KPI mapping',
           'Prepare weekly partner memo',
           'Resolve blocked creative feedback',
           'Approve launch readiness checklist'),
       @target_participant + (((FLOOR((`n` - 1) / 10)) * 2) + CASE WHEN MOD(`n`, 2) = 1 THEN 1 ELSE 2 END),
       DATE_ADD(NOW(), INTERVAL -3 + MOD(`n`, 21) DAY),
       ELT(((`n` - 1) MOD 6) + 1, 'DOCUMENT', 'DESIGN', 'REVIEW', 'MEETING', 'OTHER', 'VIDEO'),
       ELT(((`n` - 1) MOD 6) + 1, 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'BLOCKED', 'BACKLOG'),
       @target_task_part + (((`n` - 1) MOD 48) + 1),
       @target_milestone + (((`n` - 1) MOD 36) + 1),
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_user ELSE @target_partner_user END,
       ELT(((`n` - 1) MOD 4) + 1, 'HIGH', 'MEDIUM', 'CRITICAL', 'LOW'),
       ELT(((`n` - 1) MOD 6) + 1,
           'Use actual audience volume from CRM export.',
           'Partner requested a softer redemption deadline.',
           'Legal note should stay visible on mobile.',
           'Waiting on final benefit stock confirmation.',
           'Blocked until creative size is re-exported.',
           'Keep the result memo short and decision oriented.'),
       DATE_SUB(NOW(), INTERVAL MOD(`n`, 11) DAY),
       NOW()
FROM `_target_seq` WHERE `n` <= 120;

INSERT INTO `partner_benefits`
(`idx`, `affiliation_id`, `campaign_idx`, `name`, `type`, `description`, `quantity`, `quantity_unit`,
 `value_per_person`, `total_value`, `period_start`, `period_end`, `always_negotiable`, `prep_days`,
 `target_audience`, `expected_reach`, `cost_bearer`, `cost_partner_percent`, `cost_ours_percent`,
 `cost_details`, `exposure_channels`, `required_collaborations`, `conditions`, `desired_assets`,
 `auto_recommend`, `manager_name`, `manager_email`, `manager_phone`, `status`, `created_at`)
SELECT @target_benefit + `n`,
       @target_partner_org,
       @target_campaign + (((`n` - 1) MOD 12) + 1),
       ELT(((`n` - 1) MOD 6) + 1, 'Dining Credit Pack', 'Airport Lounge Upgrade', 'Family Activity Pass', 'Corporate Express Coupon', 'Wellness Add-on Credit', 'Local Culture Ticket'),
       ELT(((`n` - 1) MOD 5) + 1, 'COUPON', 'UPGRADE', 'TICKET', 'SERVICE', 'CONTENT'),
       ELT(((`n` - 1) MOD 4) + 1,
           'Benefit is limited by weekly stock and prioritized for high-value members.',
           'Partner can extend quantity if redemption rate passes the first-week threshold.',
           'Requires front-desk script and clear cancellation wording.',
           'Best used with segmented CRM and onsite QR placement.'),
       150 + (`n` * 25),
       ELT(((`n` - 1) MOD 3) + 1, 'coupons', 'tickets', 'slots'),
       18000 + (`n` * 1200),
       (150 + (`n` * 25)) * (18000 + (`n` * 1200)),
       DATE_SUB(CURDATE(), INTERVAL MOD(`n`, 10) DAY),
       DATE_ADD(CURDATE(), INTERVAL 45 + MOD(`n`, 30) DAY),
       MOD(`n`, 3) = 0,
       4 + MOD(`n`, 12),
       ELT(((`n` - 1) MOD 5) + 1, 'VIP members', 'Family guests', 'Business travelers', 'Dormant app users', 'Local diners'),
       2400 + (`n` * 180),
       ELT(((`n` - 1) MOD 3) + 1, 'SHARED', 'PARTNER', 'HANWHA'),
       35 + MOD(`n`, 30),
       65 - MOD(`n`, 30),
       'Cost shared by redeemed benefit volume with weekly reconciliation.',
       'App push, email, landing page, onsite QR',
       'Benefit stock sync, redemption report, customer support owner',
       'Valid for targeted members only; cannot be combined with selected promotions.',
       'CRM audience, landing placement, onsite display',
       TRUE,
       'Partner Ops Lead',
       'partner@callog.com',
       '010-7201-4202',
       ELT(((`n` - 1) MOD 4) + 1, 'READY', 'MATCHED', 'NEGOTIATING', 'LIVE'),
       NOW()
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `marketing_asset`
(`idx`, `owner_idx`, `campaign_idx`, `affiliate`, `category`, `conditions`, `custom_affiliate`, `exposure_value`,
 `matching_status`, `performance`, `public_status`, `scale`, `supply_limit`, `target`, `type`, `registered_at`)
SELECT @target_asset + `n`,
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_org ELSE @target_partner_org END,
       @target_campaign + (((`n` - 1) MOD 12) + 1),
       CASE WHEN MOD(`n`, 2) = 1 THEN 'Callog Headquarters' ELSE 'Callog Partner' END,
       ELT(((`n` - 1) MOD 5) + 1, 'CRM', 'MEDIA', 'ONSITE', 'EVENT', 'CONTENT'),
       ELT(((`n` - 1) MOD 4) + 1, 'Segmented members only', 'Requires legal wording', 'Limited by weekly stock', 'Use before weekend demand spike'),
       ELT(((`n` - 1) MOD 4) + 1, 'VIP room audience', 'Restaurant visitor base', 'Airport lounge users', 'Local event subscribers'),
       CONCAT(FORMAT(8000 + (`n` * 650), 0), ' expected impressions'),
       ELT(((`n` - 1) MOD 4) + 1, 'READY', 'MATCHED', 'REVIEW', 'LIVE'),
       CONCAT('CTR ', ROUND(1.8 + MOD(`n`, 18) / 10, 1), '%, CVR ', ROUND(0.7 + MOD(`n`, 12) / 10, 1), '%'),
       ELT(((`n` - 1) MOD 3) + 1, 'PUBLIC', 'LIMITED', 'PRIVATE'),
       ELT(((`n` - 1) MOD 4) + 1, 'SMALL', 'MEDIUM', 'LARGE', 'ENTERPRISE'),
       CONCAT(300 + (`n` * 20), ' redemptions'),
       ELT(((`n` - 1) MOD 5) + 1, 'VIP members', 'Family guests', 'Business travelers', 'Dormant app users', 'Local diners'),
       ELT(((`n` - 1) MOD 5) + 1, 'APP_BANNER', 'PUSH', 'EMAIL', 'ONSITE_QR', 'PARTNER_CONTENT'),
       NOW()
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `reference_items`
(`idx`, `owner_login_id`, `type`, `title`, `url`, `thumbnail`, `description`, `channel`,
 `objective`, `status`, `reference_date`, `create_date`, `update_date`)
SELECT @target_reference + `n`,
       CASE WHEN MOD(`n`, 2) = 1 THEN 'hqgm@callog.com' ELSE 'partner@callog.com' END,
       ELT(((`n` - 1) MOD 5) + 1, 'CASE_STUDY', 'ARTICLE', 'REPORT', 'SNS', 'VIDEO'),
       ELT(((`n` - 1) MOD 8) + 1,
           'Luxury Stay Bundle Benchmark',
           'Dining Voucher Redemption Playbook',
           'Airport Lounge Membership Journey',
           'Family Weekend Demand Pattern',
           'Business Traveler Retention Note',
           'Wellness Package Creative Reference',
           'MICE Lead Nurture Sequence',
           'ESG Stay Participation Case'),
       CONCAT('https://example.com/callog/reference/', LPAD(`n`, 3, '0')),
       CONCAT('https://example.com/callog/reference/thumb-', LPAD(`n`, 3, '0'), '.jpg'),
       ELT(((`n` - 1) MOD 4) + 1,
           'Good reference for benefit framing and landing hierarchy.',
           'Useful for partner reporting cadence and redemption language.',
           'Shows how to balance premium tone with practical conversion copy.',
           'Can be adapted for segmented app push and onsite QR flows.'),
       ELT(((`n` - 1) MOD 5) + 1, 'App', 'Email', 'SNS', 'Landing', 'Offline'),
       ELT(((`n` - 1) MOD 5) + 1, 'Revenue', 'Conversion', 'Retention', 'Brand', 'ESG'),
       ELT(((`n` - 1) MOD 3) + 1, 'active', 'review', 'archived'),
       DATE_SUB(CURDATE(), INTERVAL MOD(`n`, 180) DAY),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `reference_tag` (`reference_idx`, `tag`)
SELECT @target_reference + (((`n` - 1) MOD 24) + 1),
       ELT(((`n` - 1) MOD 10) + 1, 'hotel', 'membership', 'partner', 'coupon', 'crm', 'premium', 'retention', 'review', 'local', 'esg')
FROM `_target_seq` WHERE `n` <= 72;

INSERT INTO `notifications`
(`idx`, `recipient_idx`, `sender_idx`, `type`, `severity`, `title`, `message`, `detail`, `target_label`,
 `target_url`, `dedupe_key`, `reference_type`, `reference_id`, `reference_status`, `is_read`, `read_at`,
 `create_date`, `update_date`)
SELECT @target_notification + `n`,
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_user ELSE @target_partner_user END,
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_partner_user ELSE @target_hq_user END,
       ELT(((`n` - 1) MOD 10) + 1, 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED', 'TASK_UPDATED', 'REVIEW_REQUESTED',
           'REVIEW_APPROVED', 'DEADLINE_24H', 'CAMPAIGN_INVITED', 'CAMPAIGN_MEMBER_ADDED', 'SYSTEM', 'DEADLINE_OVERDUE'),
       ELT(((`n` - 1) MOD 4) + 1, 'NORMAL', 'HIGH', 'LOW', 'CRITICAL'),
       ELT(((`n` - 1) MOD 8) + 1,
           'Creative review is ready',
           'Partner benefit stock updated',
           'Campaign KPI moved above plan',
           'Legal wording needs confirmation',
           'Launch checklist assigned',
           'Weekly memo is due tomorrow',
           'New partner response arrived',
           'Blocked item requires decision'),
       ELT(((`n` - 1) MOD 8) + 1,
           'Please review the latest creative set before the scheduled launch.',
           'The partner uploaded updated redemption capacity for this week.',
           'Revenue and redemption indicators improved after the CRM refresh.',
           'One claim needs a clearer qualifying condition before approval.',
           'A launch readiness task has been assigned to your workspace.',
           'Prepare the short weekly summary for the joint campaign review.',
           'The partner accepted the invitation and joined the campaign workspace.',
           'A blocked creative export is waiting for owner decision.'),
       CONCAT('Target-account seeded notification ', `n`),
       ELT(((`n` - 1) MOD 6) + 1, 'Launch checklist', 'Partner stock', 'KPI review', 'Legal review', 'Campaign board', 'Weekly memo'),
       CONCAT('/campaigns/', @target_campaign + (((`n` - 1) MOD 12) + 1)),
       CONCAT('target-account-notification-', LPAD(`n`, 4, '0')),
       ELT(((`n` - 1) MOD 4) + 1, 'TASK', 'CAMPAIGN', 'REVIEW', 'SYSTEM'),
       CASE WHEN MOD(`n`, 3) = 0 THEN @target_task + (((`n` - 1) MOD 120) + 1)
            ELSE @target_campaign + (((`n` - 1) MOD 12) + 1) END,
       ELT(((`n` - 1) MOD 4) + 1, 'OPEN', 'PENDING', 'DONE', 'INFO'),
       MOD(`n`, 4) = 0,
       CASE WHEN MOD(`n`, 4) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(`n`, 6) DAY) ELSE NULL END,
       DATE_SUB(NOW(), INTERVAL MOD(`n`, 20) DAY),
       NOW()
FROM `_target_seq` WHERE `n` <= 160;

INSERT INTO `notification_admin_policies`
(`organization_idx`, `role_name`, `notification_type`, `enabled`, `create_date`, `update_date`)
SELECT CASE WHEN `n` <= 14 THEN @target_hq_org ELSE @target_partner_org END,
       'ROLE_GENERAL_MANAGER',
       ELT(((`n` - 1) MOD 14) + 1, 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED', 'TASK_UPDATED', 'REVIEW_REQUESTED',
           'REVIEW_APPROVED', 'REVIEW_REJECTED', 'DEADLINE_24H', 'DEADLINE_1H', 'DEADLINE_OVERDUE',
           'CAMPAIGN_INVITED', 'CAMPAIGN_INVITATION_ACCEPTED', 'CAMPAIGN_INVITATION_REJECTED',
           'CAMPAIGN_MEMBER_ADDED', 'SYSTEM'),
       TRUE,
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 28
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `update_date` = NOW();

INSERT INTO `campaign_frame`
(`idx`, `id`, `owner_idx`, `category`, `version`, `title`, `score`, `status`, `overview`, `tone_guide`,
 `usage_count`, `pass_rate`, `avg_revisions`)
SELECT @target_frame + `n`,
       CONCAT('target-account-frame-', LPAD(`n`, 2, '0')),
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_user ELSE @target_partner_user END,
       ELT(((`n` - 1) MOD 3) + 1, 'PROMOTION', 'LEGAL', 'PARTNER'),
       CONCAT('v', 1 + MOD(`n`, 2), '.0'),
       ELT(((`n` - 1) MOD 6) + 1, 'Premium Benefit Campaign Frame', 'Partner Voucher Legal Frame', 'Launch Checklist Frame', 'CRM Push Review Frame', 'Weekly Memo Frame', 'ESG Participation Frame'),
       78 + MOD(`n`, 18),
       'ACTIVE',
       'Frame prepared for realistic target account campaign work.',
       'Clear, specific, benefit-led, with no absolute claims.',
       12 + (`n` * 4),
       82 + MOD(`n`, 12),
       ROUND(0.7 + MOD(`n`, 5) / 10, 2)
FROM `_target_seq` WHERE `n` <= 6;

INSERT INTO `campaign_frame_required_field` (`frame_idx`, `sort_order`, `required_field`)
SELECT @target_frame + (((`n` - 1) MOD 6) + 1), FLOOR((`n` - 1) / 6),
       ELT((FLOOR((`n` - 1) / 6) MOD 3) + 1, 'target segment', 'benefit condition', 'measurement owner')
FROM `_target_seq` WHERE `n` <= 18;

INSERT INTO `campaign_frame_banned_expression` (`frame_idx`, `sort_order`, `banned_expression`)
SELECT @target_frame + (((`n` - 1) MOD 6) + 1), FLOOR((`n` - 1) / 6),
       ELT((FLOOR((`n` - 1) / 6) MOD 3) + 1, 'guaranteed upgrade', 'unlimited benefit', 'risk-free')
FROM `_target_seq` WHERE `n` <= 18;

INSERT INTO `campaign_frame_recommended_expression` (`frame_idx`, `sort_order`, `recommended_expression`)
SELECT @target_frame + (((`n` - 1) MOD 6) + 1), FLOOR((`n` - 1) / 6),
       ELT((FLOOR((`n` - 1) / 6) MOD 3) + 1, 'subject to availability', 'member exclusive', 'limited period')
FROM `_target_seq` WHERE `n` <= 18;

INSERT INTO `campaign_frame_approval_process` (`frame_idx`, `sort_order`, `approval_step`)
SELECT @target_frame + (((`n` - 1) MOD 6) + 1), FLOOR((`n` - 1) / 6),
       ELT((FLOOR((`n` - 1) / 6) MOD 3) + 1, 'PM review', 'Partner confirmation', 'Legal approval')
FROM `_target_seq` WHERE `n` <= 18;

INSERT INTO `ad_review_request`
(`idx`, `campaign_idx`, `file_name`, `file_object_key`, `file_content_type`, `file_size`, `extracted_text`,
 `request_status`, `ai_status`, `law`, `violation_text`, `reason`, `suggestion`, `request_memo`,
 `requester_login_id`, `requester_name`, `requester_organization_idx`, `requester_organization_name`,
 `reviewer_login_id`, `reviewer_name`, `reviewed_at`, `review_memo`, `reject_reason`, `create_date`, `update_date`)
SELECT @target_ad_request + `n`,
       @target_campaign + `n`,
       CONCAT('target-campaign-ad-', LPAD(`n`, 2, '0'), '.pdf'),
       CONCAT('adcheck/target-accounts/ad-', LPAD(`n`, 2, '0'), '.pdf'),
       'application/pdf',
       380000 + (`n` * 18000),
       ELT(((`n` - 1) MOD 4) + 1,
           'Limited period member dining credit. Conditions apply.',
           'Premium stay package with partner benefit redemption guide.',
           'Airport lounge cross benefit for qualified members.',
           'Sustainable stay reward with participation notice.'),
       ELT(((`n` - 1) MOD 4) + 1, 'APPROVED', 'IN_REVIEW', 'REQUESTED', 'REJECTED'),
       ELT(((`n` - 1) MOD 3) + 1, 'PASS', 'PENDING', 'FAIL'),
       CASE WHEN MOD(`n`, 4) = 0 THEN 'Advertising Compliance Rule' ELSE NULL END,
       CASE WHEN MOD(`n`, 4) = 0 THEN 'Absolute benefit wording' ELSE NULL END,
       CASE WHEN MOD(`n`, 4) = 0 THEN 'The benefit availability needs a condition.' ELSE NULL END,
       CASE WHEN MOD(`n`, 4) = 0 THEN 'Add subject-to-availability wording near CTA.' ELSE NULL END,
       'Please check benefit wording and landing page consistency.',
       CASE WHEN MOD(`n`, 2) = 1 THEN 'hqgm@callog.com' ELSE 'partner@callog.com' END,
       CASE WHEN MOD(`n`, 2) = 1 THEN 'HQ General Manager' ELSE 'Partner General Manager' END,
       CASE WHEN MOD(`n`, 2) = 1 THEN @target_hq_org ELSE @target_partner_org END,
       CASE WHEN MOD(`n`, 2) = 1 THEN 'Callog Headquarters' ELSE 'Callog Partner' END,
       CASE WHEN MOD(`n`, 4) IN (1,4) THEN 'hqgm@callog.com' ELSE NULL END,
       CASE WHEN MOD(`n`, 4) IN (1,4) THEN 'HQ General Manager' ELSE NULL END,
       CASE WHEN MOD(`n`, 4) IN (1,4) THEN NOW() ELSE NULL END,
       CASE WHEN MOD(`n`, 4) = 1 THEN 'Approved with standard availability wording.' ELSE NULL END,
       CASE WHEN MOD(`n`, 4) = 0 THEN 'CTA wording needs condition.' ELSE NULL END,
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 12;

INSERT INTO `ad_analysis_document`
(`idx`, `review_request_idx`, `campaign_idx`, `file_name`, `file_object_key`, `analysis_status`,
 `total_pages`, `total_regions`, `total_issues`, `layout_model`, `ocr_model`, `detector_model`, `llm_model`,
 `summary`, `raw_payload`, `create_date`, `update_date`)
SELECT @target_ad_document + `n`,
       @target_ad_request + `n`,
       @target_campaign + `n`,
       CONCAT('target-analysis-', LPAD(`n`, 2, '0'), '.pdf'),
       CONCAT('adcheck/target-accounts/analysis-', LPAD(`n`, 2, '0'), '.pdf'),
       ELT(((`n` - 1) MOD 3) + 1, 'COMPLETED', 'PROCESSING', 'COMPLETED'),
       2, 4, 3,
       'layout-v1-target', 'ocr-v1-target', 'rule-detector-v1', 'compliance-llm-v1',
       'Target account ad analysis summary with issue and keyword records.',
       JSON_OBJECT('source', 'target-account-seed', 'campaign', `n`),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 12;

INSERT INTO `ad_analysis_page`
(`idx`, `document_idx`, `page_no`, `width`, `height`, `thumbnail_object_key`, `create_date`, `update_date`)
SELECT @target_ad_page + `n`,
       @target_ad_document + FLOOR((`n` - 1) / 2) + 1,
       ((`n` - 1) MOD 2) + 1,
       1240,
       1754,
       CONCAT('adcheck/target-accounts/thumb-', LPAD(`n`, 2, '0'), '.jpg'),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 24;

INSERT INTO `ad_analysis_region`
(`idx`, `page_idx`, `region_key`, `region_type`, `order_index`, `x`, `y`, `width`, `height`,
 `confidence`, `extracted_text`, `labels_json`, `create_date`, `update_date`)
SELECT @target_ad_region + `n`,
       @target_ad_page + (((`n` - 1) MOD 24) + 1),
       CONCAT('target-region-', LPAD(`n`, 3, '0')),
       ELT(((`n` - 1) MOD 4) + 1, 'HEADLINE', 'BODY', 'CTA', 'LEGAL'),
       ((`n` - 1) MOD 4),
       ROUND(12 + MOD(`n` * 5, 160), 2),
       ROUND(24 + MOD(`n` * 9, 220), 2),
       ROUND(180 + MOD(`n`, 180), 2),
       ROUND(54 + MOD(`n`, 120), 2),
       ROUND(0.72 + MOD(`n`, 24) / 100, 2),
       ELT(((`n` - 1) MOD 4) + 1, 'Member exclusive package', 'Benefit conditions and eligible dates', 'Reserve now in app', 'Subject to availability'),
       JSON_OBJECT('labels', JSON_ARRAY('target-account', 'campaign-ad', 'review')),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 48;

INSERT INTO `ad_analysis_issue`
(`idx`, `document_idx`, `page_no`, `region_key`, `issue_type`, `severity`, `issue_status`, `x`, `y`,
 `width`, `height`, `target_text`, `law`, `reason`, `suggestion`, `create_date`, `update_date`)
SELECT @target_ad_issue + `n`,
       @target_ad_document + (((`n` - 1) MOD 12) + 1),
       (((`n` - 1) MOD 2) + 1),
       CONCAT('target-region-', LPAD(((`n` - 1) MOD 48) + 1, 3, '0')),
       ELT(((`n` - 1) MOD 4) + 1, 'CLAIM', 'LEGAL', 'BRAND', 'TYPO'),
       ELT(((`n` - 1) MOD 4) + 1, 'MEDIUM', 'HIGH', 'LOW', 'NORMAL'),
       ELT(((`n` - 1) MOD 3) + 1, 'OPEN', 'RESOLVED', 'OPEN'),
       ROUND(20 + MOD(`n` * 4, 180), 2),
       ROUND(35 + MOD(`n` * 7, 240), 2),
       150,
       72,
       ELT(((`n` - 1) MOD 4) + 1, 'best benefit', 'limited seats', 'member exclusive', 'free upgrade'),
       CASE WHEN MOD(`n`, 2) = 0 THEN 'Advertising Compliance Rule' ELSE NULL END,
       'Review whether the expression needs supporting conditions.',
       'Use qualified wording and place conditions near the CTA.',
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 36;

INSERT INTO `ad_analysis_keyword`
(`idx`, `document_idx`, `keyword`, `source`, `weight`, `create_date`, `update_date`)
SELECT @target_ad_keyword + `n`,
       @target_ad_document + (((`n` - 1) MOD 12) + 1),
       ELT(((`n` - 1) MOD 10) + 1, 'staycation', 'voucher', 'member', 'premium', 'airport', 'wellness', 'family', 'dining', 'esg', 'coupon'),
       ELT(((`n` - 1) MOD 3) + 1, 'OCR', 'LLM', 'RULE'),
       ROUND(0.24 + MOD(`n`, 65) / 100, 2),
       NOW(),
       NOW()
FROM `_target_seq` WHERE `n` <= 36;

DROP TEMPORARY TABLE IF EXISTS `_target_seq`;
DROP TEMPORARY TABLE IF EXISTS `_target_digits`;

COMMIT;

SELECT 'target campaigns' AS `metric`, COUNT(*) AS `rows`
FROM `campaigns`
WHERE `idx` BETWEEN @target_campaign + 1 AND @target_campaign + 12
UNION ALL SELECT 'target tasks', COUNT(*) FROM `task` WHERE `idx` BETWEEN @target_task + 1 AND @target_task + 120
UNION ALL SELECT 'hqgm notifications', COUNT(*) FROM `notifications` WHERE `recipient_idx` = @target_hq_user AND `idx` BETWEEN @target_notification + 1 AND @target_notification + 160
UNION ALL SELECT 'partner notifications', COUNT(*) FROM `notifications` WHERE `recipient_idx` = @target_partner_user AND `idx` BETWEEN @target_notification + 1 AND @target_notification + 160
UNION ALL SELECT 'partner visible target campaigns', COUNT(DISTINCT cp.`campaign_idx`) FROM `campaign_participant` cp WHERE cp.`organization_idx` = @target_partner_org AND cp.`campaign_idx` BETWEEN @target_campaign + 1 AND @target_campaign + 12;
