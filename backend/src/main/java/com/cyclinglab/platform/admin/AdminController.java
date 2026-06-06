package com.cyclinglab.platform.admin;

import com.cyclinglab.platform.admin.dto.AdminUserDto;
import com.cyclinglab.platform.admin.dto.AdminUserPatchRequest;
import com.cyclinglab.platform.library.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public PageResponse<AdminUserDto> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.list(q, role, status, page, size);
    }

    @GetMapping("/{id}")
    public AdminUserDto get(@PathVariable UUID id) {
        return adminService.get(id);
    }

    @PatchMapping("/{id}")
    public AdminUserDto patch(
        @PathVariable UUID id,
        @Valid @RequestBody AdminUserPatchRequest req
    ) {
        return adminService.patch(id, req.role(), req.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}