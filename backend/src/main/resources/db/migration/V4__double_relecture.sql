-- V4 : deux relecteurs par exercice (changement de besoin du client, issue #27).
-- Conséquence du remplacement de Q6 : un exercice est désormais relu par deux
-- relecteurs distincts, la note retenue est la moyenne des deux, et une note
-- unique reste provisoire tant qu'un seul relecteur a rendu (RG17, RG18).
--
-- V1, V2 et V3 ne sont PAS modifiees : cette migration est ajoutee.
-- La contrainte uk_relecture_exercice UNIQUE (exercice_id) devient fausse et
-- doit disparaitre au profit d'une unicite sur le COUPLE (exercice, relecteur),
-- qui autorise deux relectures distinctes tout en interdisant au meme relecteur
-- de relire deux fois le meme exercice.
--
-- Les donnees deja presentes survivent : une base ayant une seule relecture par
-- exercice respecte trivially la nouvelle contrainte (une seule paire par
-- exercice). Aucune reparation de donnees n'est necessaire.
--
-- Contrainte de portabilite : la suite tourne sur PostgreSQL ET sur H2 (profil de
-- test). `ALTER TABLE ... DROP CONSTRAINT` et `ALTER TABLE ... ADD CONSTRAINT`
-- avec la meme syntaxe existent dans les deux ; `DROP CONSTRAINT IF EXISTS` est
-- evite car H2 ne le supporte pas dans cette forme.

ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;

-- RG5 revisee : deux relecteurs distincts au plus par exercice, un seul passage
-- par relecteur. Le couple (exercice_id, relecteur_id) est ce qui rend le tirage
-- au sort sans repetition sans interdire la double relecture.
ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- Index d'appui : le tableau et la consultation d'exercice comptent les relectures
-- d'un exercice ; l'index unique cree par la contrainte couvre deja exercice_id
-- en tete, cet index sert au comptage par relecteur (charge de travail restante).
CREATE INDEX idx_relecture_exercice_statut ON relecture (exercice_id, rendue_at);
