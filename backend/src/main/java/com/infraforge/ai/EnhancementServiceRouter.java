package com.infraforge.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes AI suggest requests to the correct EnhancementService implementation
 * based on the user's stored ai_provider value.
 *
 * All registered EnhancementService beans are auto-discovered via Spring injection.
 * Adding a new provider requires only a new @Component — no changes here.
 *
 * Falls back to NoOpEnhancementService when:
 *   - providerName is null (user has not configured AI)
 *   - providerName does not match any registered provider (unknown/removed provider)
 */
@Component
public class EnhancementServiceRouter {

    private static final Logger log = LoggerFactory.getLogger(EnhancementServiceRouter.class);

    private final Map<String, EnhancementService> serviceMap;
    private final NoOpEnhancementService noOp;

    public EnhancementServiceRouter(List<EnhancementService> services,
                                     NoOpEnhancementService noOp) {
        this.noOp = noOp;
        this.serviceMap = services.stream()
            .filter(s -> !"noop".equals(s.getProviderName()))
            .collect(Collectors.toMap(EnhancementService::getProviderName, Function.identity()));

        log.info("[EnhancementServiceRouter] Registered AI providers: {}", serviceMap.keySet());
    }

    /**
     * Return the EnhancementService for the given provider name.
     * Never returns null — falls back to NoOpEnhancementService.
     */
    public EnhancementService route(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return noOp;
        }
        EnhancementService service = serviceMap.get(providerName.toLowerCase());
        if (service == null) {
            log.warn("[EnhancementServiceRouter] Unknown AI provider '{}' — using NoOp", providerName);
            return noOp;
        }
        return service;
    }

    /** Returns the list of registered provider names for the frontend dropdown. */
    public List<String> registeredProviders() {
        return List.copyOf(serviceMap.keySet());
    }
}