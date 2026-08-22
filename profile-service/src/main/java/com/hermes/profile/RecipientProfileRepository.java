package com.hermes.profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RecipientProfileRepository extends JpaRepository<RecipientProfile, Long> {
    Optional<RecipientProfile> findByUserId(Long userId);
}
