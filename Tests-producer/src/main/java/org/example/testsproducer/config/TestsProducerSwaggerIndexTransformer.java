package org.example.testsproducer.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class TestsProducerSwaggerIndexTransformer
        extends SwaggerIndexPageTransformer {

    private static final String SWAGGER_CUSTOMIZATION = """

            const testsProducerOriginalOnload = window.onload;
            window.onload = function () {
              testsProducerOriginalOnload();

              const style = document.createElement("style");
              style.textContent = `
                .swagger-ui .topbar {
                  display: none;
                }
                .swagger-ui .info .link {
                  display: none;
                }
                .swagger-ui .opblock-section-request-body
                    .opblock-section-header {
                  display: none;
                }
              `;
              document.head.appendChild(style);

              const initializedEditors = new WeakMap();

              function configuredMessages() {
                const selectors = window.ui?.getSystem?.().specSelectors;
                const specification = selectors?.specJson?.();
                const messages = specification?.get?.(
                  "x-tests-producer-default-original-messages"
                );
                return messages?.toJS?.() ?? {};
              }

              function updateTextArea(textArea, message) {
                const valueSetter = Object.getOwnPropertyDescriptor(
                  window.HTMLTextAreaElement.prototype,
                  "value"
                ).set;
                valueSetter.call(textArea, message);
                textArea.dispatchEvent(new Event("input", { bubbles: true }));
                textArea.dispatchEvent(new Event("change", { bubbles: true }));
              }

              function initializeDefaultMessage(operation) {
                const operationPath = operation.querySelector(
                  '.opblock-summary-path[data-path="/events"]'
                );
                if (!operationPath) {
                  return;
                }

                const flowSelector = operation.querySelector(
                  'tr[data-param-name="flow"][data-param-in="query"] select'
                );
                const textArea = operation.querySelector(
                  ".opblock-section-request-body .body-param__text"
                );
                if (!flowSelector || !textArea) {
                  return;
                }

                const flow = flowSelector.value;
                const previous = initializedEditors.get(operation);
                if (previous?.flow === flow
                    && previous?.textArea === textArea) {
                  return;
                }

                const message = configuredMessages()[flow] ?? "";
                updateTextArea(textArea, message);
                initializedEditors.set(operation, { flow, textArea });
              }

              function initializeVisibleEditors() {
                document.querySelectorAll(".swagger-ui .opblock")
                  .forEach(initializeDefaultMessage);
              }

              document.addEventListener("change", function (event) {
                const flowSelector = event.target.closest?.(
                  'tr[data-param-name="flow"][data-param-in="query"] select'
                );
                if (!flowSelector) {
                  return;
                }
                const operation = flowSelector.closest(".opblock");
                if (operation) {
                  initializedEditors.delete(operation);
                  window.requestAnimationFrame(
                    () => initializeDefaultMessage(operation)
                  );
                }
              });

              new MutationObserver(initializeVisibleEditors).observe(
                document.getElementById("swagger-ui"),
                { childList: true, subtree: true }
              );
              initializeVisibleEditors();
            };
            """;

    TestsProducerSwaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider
    ) {
        super(
                swaggerUiConfigProperties,
                swaggerUiOAuthProperties,
                swaggerWelcomeCommon,
                objectMapperProvider
        );
    }

    @Override
    public Resource transform(
            HttpServletRequest request,
            Resource resource,
            ResourceTransformerChain transformerChain
    ) throws IOException {
        Resource transformed = super.transform(
                request,
                resource,
                transformerChain
        );
        if (!"swagger-initializer.js".equals(resource.getFilename())) {
            return transformed;
        }

        String source = transformed.getContentAsString(StandardCharsets.UTF_8);
        return new TransformedResource(
                transformed,
                (source + SWAGGER_CUSTOMIZATION)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
