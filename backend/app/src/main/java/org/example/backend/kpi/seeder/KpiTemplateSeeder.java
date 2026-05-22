package org.example.backend.kpi.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.KpiTemplate;
import org.example.backend.kpi.model.TemplateScope;
import org.example.backend.kpi.repository.KpiTemplateRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 첫 구동 시 GLOBAL KPI 템플릿 시드.
 * 디지털 마케팅 5축 (노출 / 참여 / 전환 / 매출 / 브랜드) + 운영·ESG 기준.
 * 이미 row가 있으면 skip.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KpiTemplateSeeder implements ApplicationRunner {

    private final KpiTemplateRepository templateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (templateRepository.count() > 0) {
            log.info("[KpiTemplateSeeder] kpi_template already populated ({} rows). skip.",
                    templateRepository.count());
            return;
        }

        List<KpiTemplate> seeds = List.of(
                // ── 노출 (IMPRESSION) ─────────────────────────────────
                tpl("순도달 (Reach) 500만 UU",       "UU",       KpiCategory.GROWTH, GoalKind.STRATEGIC),
                tpl("SOV (Share of Voice) 25%",      "%",        KpiCategory.GROWTH, GoalKind.STRATEGIC),
                tpl("광고 노출 1,000만 회",           "회",       KpiCategory.GROWTH, GoalKind.TACTICAL),

                // ── 참여 (ENGAGEMENT) ──────────────────────────────────
                tpl("CTR 1.5% 유지",                  "%",        KpiCategory.GROWTH, GoalKind.TACTICAL),
                tpl("영상 완주율 (VTR) 50%",          "%",        KpiCategory.GROWTH, GoalKind.TACTICAL),
                tpl("SNS 팔로워 순증 1만 명",          "명",       KpiCategory.GROWTH, GoalKind.TACTICAL),
                tpl("UGC 생성 1,000건",               "건",       KpiCategory.GROWTH, GoalKind.TACTICAL),

                // ── 전환 (CONVERSION) ─────────────────────────────────
                tpl("CVR (전환율) 3%",                "%",        KpiCategory.GROWTH, GoalKind.TACTICAL),
                tpl("랜딩페이지 방문 10만 세션",        "Sessions", KpiCategory.GROWTH, GoalKind.TACTICAL),
                tpl("신규 회원가입 5,000명",           "명",       KpiCategory.GROWTH, GoalKind.TACTICAL),

                // ── 매출 (REVENUE) ────────────────────────────────────
                tpl("ROAS 400%",                      "%",        KpiCategory.FINANCIAL,  GoalKind.STRATEGIC),
                tpl("CPA 25,000원 이하",              "원",       KpiCategory.FINANCIAL,  GoalKind.TACTICAL),
                tpl("LTV:CAC 3배",                    "배수",     KpiCategory.FINANCIAL,  GoalKind.STRATEGIC),

                // ── 브랜드 (BRAND) ─────────────────────────────────────
                tpl("브랜드 인지도 +5%p",             "%p",       KpiCategory.BRAND,      GoalKind.STRATEGIC),
                tpl("Branded Search +30%",            "%",        KpiCategory.BRAND,      GoalKind.STRATEGIC),
                tpl("긍정 Sentiment 비율 70%",        "%",        KpiCategory.BRAND,      GoalKind.TACTICAL),
                tpl("NPS 50점 이상",                  "점",       KpiCategory.BRAND,      GoalKind.STRATEGIC),

                // ── 운영·기타 (OTHER) ─────────────────────────────────
                tpl("신규 협력사 25곳 확보",          "곳",       KpiCategory.OPERATIONAL, GoalKind.STRATEGIC),
                tpl("캠페인 12건 런칭",               "건",       KpiCategory.OPERATIONAL, GoalKind.STRATEGIC),
                tpl("자산 LIVE 100건 유지",           "건",       KpiCategory.OPERATIONAL, GoalKind.TACTICAL),
                tpl("검수 패스율 90%",                "%",        KpiCategory.OPERATIONAL, GoalKind.TACTICAL),

                // ── ESG ─────────────────────────────────────────────────
                tplEsg("탄소 배출 10% 절감",          "%",        EsgCategory.ENVIRONMENTAL),
                tplEsg("협력사 ESG 평가 80점 이상",   "점",       EsgCategory.GOVERNANCE),
                tplEsg("사회 공헌 캠페인 4건",        "건",       EsgCategory.SOCIAL)
        );

        templateRepository.saveAll(seeds);
        log.info("[KpiTemplateSeeder] inserted {} GLOBAL KPI templates.", seeds.size());
    }

    private static KpiTemplate tpl(String name, String unit, KpiCategory cat, GoalKind kind) {
        return KpiTemplate.builder()
                .name(name)
                .defaultUnit(unit)
                .defaultCategory(cat)
                .defaultKind(kind)
                .scope(TemplateScope.GLOBAL)
                .usageCount(0)
                .build();
    }

    private static KpiTemplate tplEsg(String name, String unit, EsgCategory esg) {
        return KpiTemplate.builder()
                .name(name)
                .defaultUnit(unit)
                .defaultCategory(KpiCategory.SUSTAINABILITY)
                .defaultEsgCategory(esg)
                .defaultKind(GoalKind.STRATEGIC)
                .scope(TemplateScope.GLOBAL)
                .usageCount(0)
                .build();
    }
}
