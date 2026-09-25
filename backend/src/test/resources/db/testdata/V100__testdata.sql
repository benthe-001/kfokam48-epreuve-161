-- Testdata (profil test uniquement) : la promotion id=1 est fournie par V2 ;
-- on force un libellé explicite de test (seul l'identifiant importe pour les tests).
UPDATE promotion SET nom = 'Test' WHERE id = 1;