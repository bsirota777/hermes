package com.hermes.profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SenderProfileRepository extends JpaRepository<SenderProfile, Long> {
    Optional<SenderProfile> findByUserId(Long userId);
}
