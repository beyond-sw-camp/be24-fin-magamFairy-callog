package org.example.backend.common.security;

import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class CampaignMemberGuard {

    private CampaignMemberGuard() {}

    public static CampaignMember requireMember(CampaignMember member) {
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "캠페인 참여자가 아닙니다.");
        }
        return member;
    }

    public static CampaignMember requireCampaignManager(CampaignMember member) {
        requireMember(member);
        CampaignMemberRole role = member.getCampaignRole();
        if (role != CampaignMemberRole.MANAGER && role != CampaignMemberRole.GENERAL_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "MANAGER 또는 GENERAL_MANAGER 권한이 필요합니다.");
        }
        return member;
    }

    public static CampaignMember requireCampaignGM(CampaignMember member) {
        requireMember(member);
        if (member.getCampaignRole() != CampaignMemberRole.GENERAL_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "GENERAL_MANAGER 권한이 필요합니다.");
        }
        return member;
    }

    public static void requireSameCompany(User actor, User target) {
        String aCompany = normalize(actor.getCompanyName());
        String tCompany = normalize(target.getCompanyName());
        if (aCompany.isEmpty() || !aCompany.equals(tCompany)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "같은 회사의 사용자만 처리할 수 있습니다.");
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
