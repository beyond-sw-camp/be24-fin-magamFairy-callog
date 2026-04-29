package org.example.backend.matching.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.organization.model.Organization;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class PartnerBenefits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String title;
    private String benefitType;
    private Integer quantity;
    private Integer budgetAmount;
    private String targetCustomer;
    private String costShareCondition;
    private String operationCondition;
    private String brandInfo;
    private String createdAt;


}
