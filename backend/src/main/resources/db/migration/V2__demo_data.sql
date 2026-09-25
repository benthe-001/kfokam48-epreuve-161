-- Données de démonstration (cahier des charges : promotions et étudiants pré-remplis au démarrage).
-- Identifiants explicites pour pouvoir viser des promotions connues (tests, démos, Swagger).
-- NOTE : la séquence d'identité n'est pas avancée ici (syntaxe PostgreSQL non supportée par H2 en test) ;
-- aucune insertion programmatique de promotion/étudiant n'existe à ce jour.
INSERT INTO promotion (id, nom) VALUES
    (1, '2025-2026 B3 Développement'),
    (2, '2025-2026 B3 Cybersécurité');

INSERT INTO etudiant (id, nom, promotion_id) VALUES
    (1, 'Ndiaye Awa',       1),
    (2, 'Diallo Moussa',    1),
    (3, 'Kone Fatou',       2),
    (4, 'Traore Ibrahima',  2);