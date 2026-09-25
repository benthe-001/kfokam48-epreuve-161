# D4 — États-transitions : cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> Deposé : POST /api/exercices (EF3)

    Deposé --> Deposé : remplacement du lien (EF4, RG12 — tant qu'aucun relecteur assigné)
    Deposé --> EnAttenteDeRelecture : deux relecteurs assignés (RG6, RG5 révisée)

    EnAttenteDeRelecture --> EnAttenteDeRelecture : 1re relecture rendue (RG18 — note provisoire, l'exercice reste en attente de la 2e)
    EnAttenteDeRelecture --> Relu : les 2 relectures sont rendues (EF6, RG8, RG17 — note = moyenne des deux)
    EnAttenteDeRelecture --> EnAttenteDeRelecture : session clôturée sans les 2 relectures rendues (RG10 — reste visible "en attente" dans le tableau)

    Relu --> [*] : session clôturée (RG14 — note figée définitivement)
```