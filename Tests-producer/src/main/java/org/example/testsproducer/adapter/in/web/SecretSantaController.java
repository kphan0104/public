package org.example.testsproducer.adapter.in.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.example.testsproducer.application.service.SecretSantaService;
import org.example.testsproducer.config.SecretSantaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Hidden
@RestController
@RequestMapping(value = "/secret-santa", produces = MediaType.TEXT_HTML_VALUE)
@ConditionalOnProperty(name = "tests-producer.secret-santa.enabled", havingValue = "true")
public class SecretSantaController {
    private final SecretSantaService service;
    private final SecretSantaProperties properties;

    public SecretSantaController(SecretSantaService service, SecretSantaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/admin/{key}")
    public ResponseEntity<String> admin(@PathVariable String key) {
        if (!MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                properties.adminKey().getBytes(StandardCharsets.UTF_8))) {
            return missing();
        }
        String base = properties.publicBaseUrl().toString().replaceAll("/+$", "");
        StringBuilder rows = new StringBuilder();
        int index = 0;
        for (var link : service.links()) {
            String id = "link-" + index++;
            rows.append("<li><label for=\"").append(id).append("\">")
                    .append(escape(link.participant())).append("</label><div class=\"link\">")
                    .append("<input readonly id=\"").append(id).append("\" value=\"")
                    .append(escape(base + "/secret-santa/" + link.token()))
                    .append("\"><button type=\"button\" data-copy=\"").append(id)
                    .append("\" aria-label=\"Copier le lien de ").append(escape(link.participant()))
                    .append("\">Copier le lien</button></div></li>");
        }
        return page(200, "Les invitations", """
                <p class="eyebrow">SECRET SANTA · ORGANISATION</p>
                <h1>À chacun son secret.</h1>
                <p>Envoie à chaque personne son lien personnel. Les résultats restent cachés ici.</p>
                <ul>%s</ul>
                <p id="feedback" role="status" aria-live="polite"></p>
                <p class="note">Garde cette page pour toi. Ouvrir un lien personnel révèle le destinataire.</p>
                """.formatted(rows));
    }

    @GetMapping("/{token}")
    public ResponseEntity<String> reveal(@PathVariable String token) {
        return service.reveal(token).map(result -> page(200, "Ton Secret Santa", """
                <p class="eyebrow">SECRET SANTA</p>
                <p>Bonjour %s,</p>
                <h1>Tu offres un cadeau à</h1>
                <p class="recipient">%s</p>
                <p class="note">Chut… garde la surprise jusqu’au jour des cadeaux.</p>
                """.formatted(escape(result.participant()), escape(result.recipient())))).orElseGet(this::missing);
    }

    private ResponseEntity<String> missing() {
        return page(404, "Lien introuvable", "<h1>Lien introuvable.</h1><p>Vérifie le lien reçu auprès de l’organisateur.</p>");
    }

    private ResponseEntity<String> page(int status, String title, String content) {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        String nonce = Base64.getEncoder().encodeToString(bytes);
        String html = """
                <!doctype html>
                <html lang="fr"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="robots" content="noindex,nofollow,noarchive">
                <title>%s</title>
                <style nonce="%s">
                :root{color-scheme:light}*{box-sizing:border-box}
                body{margin:0;padding:clamp(24px,7vw,80px) 20px;background:#f5f0e6;color:#183c32;font:18px/1.6 Georgia,serif}
                main{max-width:800px;margin:auto;border-top:6px solid #a33128;padding-top:30px}
                h1{font-weight:normal;font-size:clamp(32px,6vw,52px);line-height:1.15;margin:20px 0 30px}
                .eyebrow{font:13px/1.5 Verdana,sans-serif;letter-spacing:2px;color:#a33128}
                .recipient{font-size:clamp(40px,9vw,76px);line-height:1.2;overflow-wrap:anywhere;margin:25px 0 50px}
                .note{font-size:15px;color:#4b5c53;margin-top:35px}ul{list-style:none;padding:0;margin-top:35px}
                li{border-top:1px solid #c7cabc;padding:18px 0}label{display:block;font-weight:bold;margin-bottom:8px;overflow-wrap:anywhere}
                .link{display:flex;gap:10px}input{min-width:0;flex:1;padding:12px;border:1px solid #8a9a90;background:#fffcf5;color:#183c32;font:14px monospace}
                button{border:0;background:#183c32;color:#fffcf5;padding:12px 18px;font:14px Verdana,sans-serif;cursor:pointer}
                button:hover{background:#285848}button:focus-visible,input:focus-visible{outline:3px solid #a33128;outline-offset:3px}
                #feedback{font-size:15px;min-height:24px}@media(max-width:480px){.link{flex-direction:column}}
                </style></head><body><main>%s</main>
                <script nonce="%s">
                document.querySelectorAll('[data-copy]').forEach(button=>button.addEventListener('click',async()=>{
                  const input=document.getElementById(button.dataset.copy);
                  const feedback=document.getElementById('feedback');
                  try{await navigator.clipboard.writeText(input.value);feedback.textContent='Lien copié !';}
                  catch(e){input.focus();input.select();feedback.textContent='Lien sélectionné : utilise la commande Copier de ton appareil.';}
                }));
                </script></body></html>
                """.formatted(escape(title), nonce, content, nonce);
        return ResponseEntity.status(status)
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header("Cache-Control", "no-store, private")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'nonce-" + nonce
                        + "'; script-src 'nonce-" + nonce + "'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
                .body(html);
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text, StandardCharsets.UTF_8.name());
    }
}
