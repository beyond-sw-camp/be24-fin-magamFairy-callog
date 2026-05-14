package org.example.backend.campaignframe.repository;

import org.example.backend.campaignframe.model.CampaignFrame;
import org.example.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignFrameRepository extends JpaRepository<CampaignFrame, Long> {
    List<CampaignFrame> findAllByOwnerOrderByIdxDesc(User owner);

    Optional<CampaignFrame> findByOwnerAndId(User owner, String id);

    boolean existsByOwnerAndId(User owner, String id);
}
