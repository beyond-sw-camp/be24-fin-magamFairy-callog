package org.example.backend.reference.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.reference.model.ReferenceDto;
import org.example.backend.reference.service.ReferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/references")
public class ReferenceController {
    private final ReferenceService referenceService;

    @PostMapping("/create")
    public ResponseEntity<?> createReference(
            @RequestBody ReferenceDto.UpsertReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                referenceService.createReference(currentUser(authentication), dto)
        ));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getReferences(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(
                referenceService.getReferences(currentUser(authentication))
        ));
    }

    @GetMapping("/detail/{referenceId}")
    public ResponseEntity<?> getReference(
            @PathVariable Long referenceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                referenceService.getReference(currentUser(authentication), referenceId)
        ));
    }

    @PutMapping("/update/{referenceId}")
    public ResponseEntity<?> updateReference(
            @PathVariable Long referenceId,
            @RequestBody ReferenceDto.UpsertReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                referenceService.updateReference(currentUser(authentication), referenceId, dto)
        ));
    }

    @DeleteMapping("/delete/{referenceId}")
    public ResponseEntity<?> deleteReference(
            @PathVariable Long referenceId,
            Authentication authentication
    ) {
        referenceService.deleteReference(currentUser(authentication), referenceId);
        return ResponseEntity.ok(BaseResponse.success("deleted"));
    }

    private String currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required.");
        }

        return authentication.getName();
    }
}
