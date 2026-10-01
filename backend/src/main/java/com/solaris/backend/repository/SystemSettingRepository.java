package com.solaris.backend.repository;

import com.solaris.backend.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SystemSetting> findLockedById(Long id);
}
