package com.infraforge.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    List<Template> findAllByOrderByUpdatedAtDesc();

    List<Template> findByProviderIdOrderByUpdatedAtDesc(String providerId);

    // User-scoped queries
    List<Template> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<Template> findByProviderIdAndUserIdOrderByUpdatedAtDesc(String providerId, UUID userId);

    boolean existsByProviderIdAndName(String providerId, String name);

    // Share token lookup — used by the public shared view endpoint
    Optional<Template> findByShareToken(UUID shareToken);
}