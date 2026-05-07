package org.example.backend.campaign.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "campaign_intro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignIntro extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx", unique = true, nullable = false)
    private Campaign campaign;

    @Column(length = 50)
    private String rfpCode;

    private LocalDateTime recruitDeadline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> hanwhaAssets;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> partnerRoles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> customerTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> partnerValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> timelineEvents;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> submissionDocs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> attachedFiles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> contactInfo;

    // 매칭 5축 가중치 (소개 페이지 사이드바 표시용)
    private Integer weightCustomer;
    private Integer weightRevenue;
    private Integer weightCost;
    private Integer weightOperation;
    private Integer weightBrand;

    public void updateContent(CampaignIntroDto.UpdateReq dto) {
        if (dto.getRfpCode() != null) this.rfpCode = dto.getRfpCode();
        if (dto.getRecruitDeadline() != null) this.recruitDeadline = dto.getRecruitDeadline();
        if (dto.getHanwhaAssets() != null) this.hanwhaAssets = dto.getHanwhaAssets();
        if (dto.getPartnerRoles() != null) this.partnerRoles = dto.getPartnerRoles();
        if (dto.getCustomerTags() != null) this.customerTags = dto.getCustomerTags();
        if (dto.getPartnerValues() != null) this.partnerValues = dto.getPartnerValues();
        if (dto.getTimelineEvents() != null) this.timelineEvents = dto.getTimelineEvents();
        if (dto.getSubmissionDocs() != null) this.submissionDocs = dto.getSubmissionDocs();
        if (dto.getAttachedFiles() != null) this.attachedFiles = dto.getAttachedFiles();
        if (dto.getContactInfo() != null) this.contactInfo = dto.getContactInfo();
        if (dto.getWeightCustomer() != null) this.weightCustomer = dto.getWeightCustomer();
        if (dto.getWeightRevenue() != null) this.weightRevenue = dto.getWeightRevenue();
        if (dto.getWeightCost() != null) this.weightCost = dto.getWeightCost();
        if (dto.getWeightOperation() != null) this.weightOperation = dto.getWeightOperation();
        if (dto.getWeightBrand() != null) this.weightBrand = dto.getWeightBrand();
    }
}
