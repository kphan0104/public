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

              function timestampParts(now, timeZone) {
                const formatter = new Intl.DateTimeFormat("en-GB", {
                  timeZone,
                  year: "numeric",
                  month: "2-digit",
                  day: "2-digit",
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                  hourCycle: "h23",
                  timeZoneName: "longOffset"
                });
                const formattedParts = Object.fromEntries(
                  formatter.formatToParts(now)
                    .map(part => [part.type, part.value])
                );
                const zoneName = formattedParts.timeZoneName;
                let offset = "+00:00";
                if (zoneName !== "GMT" && zoneName !== "UTC") {
                  offset = zoneName.replace(/^GMT/, "");
                }
                if (!/^[+-][0-9]{2}:[0-9]{2}$/.test(offset)) {
                  throw new Error(
                    `Fuseau horaire non pris en charge : ${timeZone}`
                  );
                }
                return {
                  year: formattedParts.year,
                  month: formattedParts.month,
                  day: formattedParts.day,
                  hour: formattedParts.hour,
                  minute: formattedParts.minute,
                  second: formattedParts.second,
                  millisecond: String(now.getUTCMilliseconds())
                    .padStart(3, "0"),
                  offset
                };
              }

              function formatTimestamp(now, pattern, timeZone) {
                const parts = timestampParts(now, timeZone);
                const zeroOffset = parts.offset === "+00:00";
                const tokenValues = {
                  yyyy: parts.year,
                  yy: parts.year.slice(-2),
                  MM: parts.month,
                  dd: parts.day,
                  HH: parts.hour,
                  mm: parts.minute,
                  ss: parts.second,
                  SSS: parts.millisecond,
                  XXX: zeroOffset ? "Z" : parts.offset,
                  XX: zeroOffset ? "Z" : parts.offset.replace(":", ""),
                  X: zeroOffset
                    ? "Z"
                    : parts.offset.endsWith(":00")
                      ? parts.offset.slice(0, 3)
                      : parts.offset.replace(":", ""),
                  Z: parts.offset.replace(":", "")
                };
                const tokens = [
                  "yyyy", "SSS", "XXX", "yy", "MM", "dd",
                  "HH", "mm", "ss", "XX", "X", "Z"
                ];
                let result = "";
                let quoted = false;

                for (let index = 0; index < pattern.length;) {
                  if (pattern[index] === "'") {
                    if (pattern[index + 1] === "'") {
                      result += "'";
                      index += 2;
                    } else {
                      quoted = !quoted;
                      index += 1;
                    }
                    continue;
                  }

                  const token = quoted
                    ? undefined
                    : tokens.find(candidate =>
                        pattern.startsWith(candidate, index)
                      );
                  if (token) {
                    result += tokenValues[token];
                    index += token.length;
                    continue;
                  }
                  if (!quoted && /[A-Za-z]/.test(pattern[index])) {
                    throw new Error(
                      `Format de timestamp non pris en charge : ${pattern}`
                    );
                  }
                  result += pattern[index];
                  index += 1;
                }

                if (quoted) {
                  throw new Error(
                    `Format de timestamp invalide : ${pattern}`
                  );
                }
                return result;
              }

              function resolveTimestamps(messageTemplate) {
                const now = new Date();
                const placeholder = /\\{\\{NOW(?:\\|([^|{}]+)(?:\\|([^|{}]+))?)?\\}\\}/g;
                return messageTemplate.replace(
                  placeholder,
                  (match, pattern, timeZone) => {
                    if (!pattern) {
                      return now.toISOString();
                    }
                    return formatTimestamp(
                      now,
                      pattern.trim(),
                      timeZone?.trim() || "UTC"
                    );
                  }
                );
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

                const messageTemplate = configuredMessages()[flow] ?? "";
                let message = messageTemplate;
                try {
                  message = resolveTimestamps(messageTemplate);
                  textArea.setCustomValidity("");
                } catch (error) {
                  textArea.setCustomValidity(error.message);
                  console.error(error);
                }
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
