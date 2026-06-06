package com.cyclinglab.platform.profile;

import com.cyclinglab.platform.profile.dto.DerivedZonesResponse;
import com.cyclinglab.platform.profile.dto.RiderProfileDto;
import com.cyclinglab.platform.profile.dto.RiderProfilePatchRequest;
import com.cyclinglab.platform.profile.dto.RiderProfileUpsertRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<RiderProfileDto> get() {
        RiderProfileDto dto = profileService.getCurrent();
        // 200 + null body when nothing is saved: design says the frontend shows
        // an "unfilled" CTA in that case, no need for a 404 round-trip.
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public RiderProfileDto upsert(@Valid @RequestBody RiderProfileUpsertRequest req) {
        return profileService.upsert(req);
    }

    @PatchMapping
    public RiderProfileDto patch(@Valid @RequestBody RiderProfilePatchRequest req) {
        return profileService.patch(req);
    }

    @GetMapping("/derived-zones")
    public DerivedZonesResponse derivedZones() {
        return profileService.derivedZones();
    }

    @GetMapping("/export")
    public Map<String, Object> export() {
        return profileService.exportDump();
    }
}
