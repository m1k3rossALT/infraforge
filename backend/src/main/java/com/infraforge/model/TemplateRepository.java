package com.infraforge.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access layer for saved templates.
 *
 * Spring Data JPA generates all standard CRUD operations automatically.
 * Custom queries can be added here as the feature set grows — for example:
 *   - findByTags for tag-based filtering
 *   - search by name using @Query with LIKE or full-text search
 *   - findByProviderIdAndNameContaining for filtered lists
 */
@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    /** List all templates for a specific provider, newest first */
    List<Template> findByProviderIdOrderByUpdatedAtDesc(String providerId);

    /** List all templates across all providers, newest first */
    List<Template> findAllByOrderByUpdatedAtDesc();

    /** Check if a template name already exists for a provider */
    boolean existsByProviderIdAndName(String providerId, String name);
}