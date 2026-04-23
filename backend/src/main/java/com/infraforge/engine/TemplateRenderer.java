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
 * Renders a provider's FreeMarker template against user-supplied field values.
 *
 * FreeMarker is loaded from classpath:/providers/{providerId}/template.ftl.
 * The template receives the flat map of field-id → value as its data model,
 * so templates reference values directly: e.g. ${region}, ${instance_type}.
 *
 * Hook point for future AI enhancement: a pre-render step can enrich the
 * values map with AI-suggested defaults before FreeMarker processes it.
 */
@Component
public class TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

    private final Configuration freemarkerConfig;

    public TemplateRenderer(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }

    public String render(String providerId, Map<String, Object> values) throws IOException, TemplateException {
        String templatePath = "providers/" + providerId + "/template.ftl";
        Template template = freemarkerConfig.getTemplate(templatePath);

        StringWriter writer = new StringWriter();
        template.process(values, writer);

        log.debug("[TemplateRenderer] Rendered template for provider '{}' with {} values", providerId, values.size());
        return writer.toString();
    }
}
