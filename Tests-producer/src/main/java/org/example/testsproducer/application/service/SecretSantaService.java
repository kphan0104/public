package org.example.testsproducer.application.service;

import org.example.testsproducer.application.port.out.SecretSantaStorePort;
import org.example.testsproducer.domain.model.SecretSantaDraw;
import org.example.testsproducer.domain.model.SecretSantaDraw.Entry;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class SecretSantaService {
    private final SecretSantaDraw draw;

    public SecretSantaService(List<String> participants, SecretSantaStorePort store) {
        if (participants == null || participants.size() < 2 || participants.size() > 1000
                || participants.stream().anyMatch(name -> name == null || name.isBlank()
                || !name.equals(name.strip()) || name.length() > 100)
                || new HashSet<>(participants).size() != participants.size()) {
            throw new IllegalArgumentException("Secret Santa : prévoir de 2 à 1000 noms distincts, non vides, de 100 caractères maximum, sans espaces aux extrémités.");
        }
        draw = store.loadOrCreate(() -> create(participants));
        validate(participants);
    }

    private static SecretSantaDraw create(List<String> participants) {
        SecureRandom random = new SecureRandom();
        List<String> recipients = new ArrayList<>(participants);
        // Rejection sampling gives every valid permutation the same probability.
        do {
            Collections.shuffle(recipients, random);
        } while (java.util.stream.IntStream.range(0, participants.size())
                .anyMatch(i -> participants.get(i).equals(recipients.get(i))));
        List<Entry> entries = new ArrayList<>();
        var tokens = new HashSet<String>();
        for (int i = 0; i < participants.size(); i++) {
            String token;
            do {
                byte[] bytes = new byte[32];
                random.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            } while (!tokens.add(token));
            entries.add(new Entry(participants.get(i), token, recipients.get(i)));
        }
        return new SecretSantaDraw(entries);
    }

    private void validate(List<String> participants) {
        var names = new HashSet<String>();
        var recipients = new HashSet<String>();
        var tokens = new HashSet<String>();
        boolean valid = draw != null && draw.entries().size() == participants.size();
        if (valid) {
            for (Entry entry : draw.entries()) {
                if (entry == null || entry.participant() == null || entry.recipient() == null
                        || entry.participant().equals(entry.recipient()) || entry.token() == null
                        || !entry.token().matches("[A-Za-z0-9_-]{43}")
                        || !names.add(entry.participant()) || !recipients.add(entry.recipient())
                        || !tokens.add(entry.token())) {
                    valid = false;
                    break;
                }
            }
        }
        if (!valid || !names.equals(new HashSet<>(participants)) || !recipients.equals(names)) {
            throw new IllegalStateException("Secret Santa : tirage invalide ou participants modifiés. Le tirage existant est conservé.");
        }
    }

    public List<ParticipantLink> links() {
        return draw.entries().stream()
                .map(entry -> new ParticipantLink(entry.participant(), entry.token())).toList();
    }

    public Optional<Reveal> reveal(String token) {
        return draw.entries().stream().filter(entry -> entry.token().equals(token))
                .map(entry -> new Reveal(entry.participant(), entry.recipient())).findFirst();
    }

    public record ParticipantLink(String participant, String token) {}
    public record Reveal(String participant, String recipient) {}
}
