-- V4 : deux relecteurs par exercice (changement de besoin du client, issue #27).
-- Conséquence du remplacement de Q6 : un exercice est désormais relu par deux
-- relecteurs distincts, la note retenue est la moyenne des deux, et une note
-- unique reste provisoire tant qu'un seul relecteur a rendu (RG17, RG18).
--
-- V1, V2 et V3 ne sont PAS modifiees : cette migration est ajoutee.
-- La contrainte uk_relecture_exercice UNIQUE (exercice_id) devient fausse et
-- doit disparaitre au profit d'une unicite sur le COUPLE (exercice, relecteur),
-- qui autorise deux relecteurs distincts tout en interdisant au meme relecteur
-- de relire deux fois le meme exercice.
--
-- Les donnees deja presentes survivent : une base ayant une seule relecture par
-- exercice respecte trivially la nouvelle contrainte (une seule paire par
-- exercice). Aucune instruction DELETE ni UPDATE n'apparait ci-dessous.
--
-- Contrainte de portabilite : la suite tourne sur PostgreSQL ET sur H2 (profil de
-- test), et les deux ne se comportent pas pareil sur la suppression d'une
-- contrainte UNIQUE. C'est le point le plus delicat de cette migration, d'ou les
-- commentaires detaillees.

-- 1. Retirer la contrainte devenue fausse : un seul relecteur par exercice.
--    Sur PostgreSQL, la contrainte et son index partent ensemble.
ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;

-- 2-4. Divergence de moteur, traitee explicitement.
--
-- Sur H2, ALTER TABLE ... DROP CONSTRAINT retire la contrainte mais CONSERVE
-- l'index unique qu'elle avait genere (uk_relecture_exercice_INDEX_5). Pire,
-- cet index orphelin se retrouve REATTRIBUE a la cle etrangere
-- fk_relecture_exercice, qui refuse alors qu'on le supprime :
--   SQL State 90085 - Index "uk_relecture_exercice_INDEX_5" belongs to
--   constraint "fk_relecture_exercice"
-- C'est cet index-la, et non la contrainte, qui continuait d'interdire deux
-- relectures sur le meme exercice apres la migration.
--
-- Remede : on retire la cle etrangere, ce qui libere l'index, on supprime
-- l'index, puis on RECREE la cle etrangere a l'identique de V1. Aucune donnee
-- n'est touchee : une cle etrangere ne porte pas de donnee, elle les constraint.
--
-- Sur PostgreSQL, ces instructions sont sans effet utile (l'index a deja disparu
-- avec la contrainte, et IF EXISTS absorbe l'absence) ; seule la suppression puis
-- recreation de la cle a lieu, ce qui est inoffensif.
ALTER TABLE relecture DROP CONSTRAINT fk_relecture_exercice;

-- Les guillemets doubles sont NECESSAIRES : le nom se termine par "INDEX_5" en
-- majuscules, alors que la base de test tourne avec DATABASE_TO_LOWER=TRUE, qui
-- replie en minuscules tout identifiant NON guillemeté. Sans guillemets,
-- l'instruction chercherait "uk_relecture_exercice_index_5", ne trouverait rien,
-- et IF EXISTS en ferait silencieusement un no-op : l'index serait toujours là.
DROP INDEX IF EXISTS "uk_relecture_exercice_INDEX_5";

ALTER TABLE relecture ADD CONSTRAINT fk_relecture_exercice FOREIGN KEY (exercice_id) REFERENCES exercice (id);

-- 5. RG5 revisee : deux relecteurs distincts au plus par exercice, un seul
--    passage par relecteur. C'est le couple (exercice_id, relecteur_id) qui
--    autorise la double relecture sans autoriser la repetition d'un relecteur.
ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- Index d'appui : le tableau compte les relectures non rendues d'un exercice.
-- L'index unique cree par la contrainte couvre deja exercice_id en tete ;
-- celui-ci sert au filtre sur le couple exercice / rendue_at.
CREATE INDEX idx_relecture_exercice_statut ON relecture (exercice_id, rendue_at);
