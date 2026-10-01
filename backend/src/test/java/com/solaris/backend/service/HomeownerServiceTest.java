package com.solaris.backend.service;

import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeownerServiceTest {
    @Test
    void siteLookupIsScopedToAuthenticatedOwner() {
        UserRepository userRepository = mock(UserRepository.class);
        SolarSiteRepository siteRepository = mock(SolarSiteRepository.class);
        HomeownerService service = new HomeownerService(userRepository, siteRepository);
        SolarSite otherOwnersSite = SolarSite.builder().id(10L).owner(User.builder().id(2L).build()).build();
        when(siteRepository.findById(10L)).thenReturn(Optional.of(otherOwnersSite));
        when(siteRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.site(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Solar site not found");
    }
}
