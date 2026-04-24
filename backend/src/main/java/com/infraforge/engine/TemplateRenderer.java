package com.infraforge.engine;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Thin FreeMarker wrapper. The controller builds the model; this class
 * loads the provider template and renders it.
 *
 * Template path convention: classpath:/providers/{providerId}/template.ftl
 * Model key contract: whatever the controller puts in the map is accessible
 * in the template. Current contract: model has a single "sections" key whose
 * value is a Map<sectionId, { enabled, instances[] }>.
 *
 * AI hook point (Phase 4): insert a pre-render enrichment step here that
 * can add AI-suggested values to the model before FreeMarker processes it.
 */
@Component
public class TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

    private final Configuration cfg;

    public TemplateRenderer(Configuration cfg) {
        this.cfg = cfg;
    }

    public String render(String providerId, Map<String, Object> model) throws IOException, TemplateException {
        String templatePath = "providers/" + providerId + "/template.ftl";
        Template template = cfg.getTemplate(templatePath);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        log.debug("[TemplateRenderer] Rendered template for provider '{}'", providerId);
        return writer.toString();
    }
}
