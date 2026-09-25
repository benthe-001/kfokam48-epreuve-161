-- V3 : compteur d'echecs de code par etudiant (RG3 revisee).
-- NUMERO DE VERSION : V2 est deja pris par V2__demo_data.sql ; deux migrations
-- de meme version feraient echouer Flyway au demarrage.

CREATE TABLE tentative_blocage (
    etudiant_id     BIGINT PRIMARY KEY,
    echecs          INTEGER NOT NULL DEFAULT 0,
    bloque_jusqua   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_tentative_etudiant FOREIGN KEY (etudiant_id) REFERENCES etudiant (id)
);
