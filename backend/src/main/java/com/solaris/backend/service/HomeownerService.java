package com.solaris.backend.service;

import com.solaris.backend.dto.homeowner.ProfileResponse;
import com.solaris.backend.dto.homeowner.UpdateProfileRequest;
import com.solaris.backend.dto.site.SiteResponse;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HomeownerService {
    private final UserRepository userRepository;
    private final SolarSiteRepository siteRepository;

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        return toProfile(findUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ConflictException("Email is already registered");
        }
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(normalizeNullable(request.getPhone()));
        return toProfile(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> sites(Long userId) {
        return siteRepository.findByOwnerIdOrderByName(userId).stream()
                .map(this::toSite)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteResponse site(Long userId, Long siteId) {
        SolarSite site = siteRepository.findByIdAndOwnerId(siteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Solar site not found"));
        return toSite(site);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProfileResponse toProfile(User user) {
        return new ProfileResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    private SiteResponse toSite(SolarSite site) {
        return new SiteResponse(
                site.getId(), site.getOwner().getId(), site.getOwner().getName(), site.getCode(), site.getName(),
                site.getAddress(), site.getCapacityKw(), site.isActive(), site.getCreatedAt(), site.getUpdatedAt());
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
