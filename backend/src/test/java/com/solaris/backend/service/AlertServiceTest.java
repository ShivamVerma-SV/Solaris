package com.solaris.backend.service;

import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.AlertRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertServiceTest {
    @Test
    void homeownerCannotMarkAnotherUsersAlertRead() {
        AlertRepository repository = mock(AlertRepository.class);
        when(repository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AlertService(repository).markRead(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Alert not found");
    }
}
