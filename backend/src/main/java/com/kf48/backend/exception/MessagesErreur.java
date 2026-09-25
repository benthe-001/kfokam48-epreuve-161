package com.kf48.backend.exception;

import java.util.Map;
import static java.util.Map.entry;

public final class MessagesErreur {

    private static final Map<String, String> LIBELLES = Map.ofEntries(
            // Génériques (B4 : jamais de page Spring par défaut)
            entry("REQUETE_INVALIDE", "La requête est mal formée."),
            entry("CHAMP_MANQUANT", "Un champ obligatoire est manquant."),
            entry("RESSOURCE_INTROUVABLE", "Cette adresse n'existe pas."),
            entry("METHODE_NON_AUTORISEE", "Cette opération n'est pas autorisée sur cette adresse."),
            entry("ERREUR_INTERNE", "Une erreur interne est survenue. Réessayez plus tard."),

            // POST /api/sessions
            // (400 CHAMP_MANQUANT ci-dessus)

            // POST /api/presences
            entry("CODE_INCONNU", "Ce code de présence n'existe pas."),
            entry("CODE_EXPIRE", "Le code de présence a expiré."),
            entry("DEJA_PRESENT", "La présence est déjà enregistrée pour cette session."),
            entry("ETUDIANT_BLOQUE", "Trop de tentatives échouées. Réessayez dans quelques instants."),

            // POST /api/exercices
            entry("LIEN_INVALIDE", "Le lien doit être une adresse http ou https complète."),
            entry("EXERCICE_DEJA_DEPOSE", "Un exercice a déjà été déposé pour cette session."),

            // POST/PUT /api/relectures/{id}
            entry("NOTE_INVALIDE", "La note doit être un nombre entier entre 0 et 20."),
            entry("AUTO_RELECTURE", "Un étudiant ne peut pas relire son propre exercice."),
            entry("RELECTURE_DEJA_RENDUE", "Cette relecture a déjà été rendue et ne peut plus être modifiée."),
            entry("RELECTURE_INCONNUE", "Cette relecture n'existe pas."),
            entry("SESSION_CLOTUREE", "Cette session est clôturée."),

            // GET /api/tableau
            entry("PROMOTION_INCONNUE", "Cette promotion n'existe pas."),

            // Ajouts : /api/sessions/{id}/cloture, /api/sessions/{id}/presences, /api/exercices/{id}
            entry("SESSION_INCONNUE", "Cette session n'existe pas."),
            entry("SESSION_DEJA_CLOTUREE", "Cette session est déjà clôturée."),
            entry("ETUDIANT_INCONNU", "Cet étudiant n'existe pas."),
            entry("EXERCICE_INCONNU", "Cet exercice n'existe pas."),
            entry("RELECTEUR_DEJA_ASSIGNE", "Un relecteur est déjà assigné, le lien n'est plus modifiable.")
    );

    private MessagesErreur() {}

    public static String libelle(String code) {
        return LIBELLES.getOrDefault(code, "La requête est invalide.");
    }
}