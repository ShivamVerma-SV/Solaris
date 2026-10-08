package com.solaris.backend.service;

import com.solaris.backend.dto.homeowner.ChangePasswordRequest;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class HomeownerServiceTest {
    @Test
    void siteLookupIsScopedToAuthenticatedOwner() {
        UserRepository userRepository = mock(UserRepository.class);
        SolarSiteRepository siteRepository = mock(SolarSiteRepository.class);
        HomeownerService service = new HomeownerService(userRepository, siteRepository, mock(PasswordEncoder.class));
        SolarSite otherOwnersSite = SolarSite.builder().id(10L).owner(User.builder().id(2L).build()).build();
        when(siteRepository.findById(10L)).thenReturn(Optional.of(otherOwnersSite));
        when(siteRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.site(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Solar site not found");
    }

    @Test
    void changesPasswordWhenCurrentPasswordMatches() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        HomeownerService service = new HomeownerService(
                userRepository, mock(SolarSiteRepository.class), passwordEncoder);
        User user = User.builder().id(1L).password(passwordEncoder.encode("old-password")).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest request = passwordRequest("old-password", "new-password");

        service.changePassword(1L, request);

        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        HomeownerService service = new HomeownerService(
                userRepository, mock(SolarSiteRepository.class), passwordEncoder);
        User user = User.builder().id(1L).password(passwordEncoder.encode("old-password")).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest request = passwordRequest("wrong-password", "new-password");

        assertThatThrownBy(() -> service.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");
        verify(userRepository, never()).save(user);
    }

    private ChangePasswordRequest passwordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }
}
