-- CREA DATABASE (se lo lanci da psql come superuser puoi usare anche questa parte)
-- CREATE DATABASE bookrecommender
--   WITH ENCODING 'UTF8'
--   LC_COLLATE='it_IT.utf8'
--   LC_CTYPE='it_IT.utf8'
--   TEMPLATE=template0;

-- Usa il database
-- \c bookrecommender;

-- ========== TABELLA LIBRI ==========
CREATE TABLE libri (
    id            INTEGER PRIMARY KEY,
    titolo        TEXT NOT NULL,
    autori        TEXT,          -- lista autori separati da ';' o simile
    anno          INTEGER,
    editore       TEXT,
    categoria     TEXT
    -- se vuoi puoi aggiungere altre colonne in base al CSV
);

-- ========== TABELLA UTENTI ==========
CREATE TABLE utenti_registrati (
    userid        VARCHAR(50) PRIMARY KEY,
    password_hash CHAR(64) NOT NULL,   -- SHA-256
    nome          VARCHAR(100),
    cognome       VARCHAR(100),
    codice_fiscale VARCHAR(16),
    email         VARCHAR(255)
);

-- ========== TABELLE LIBRERIE PERSONALI ==========
-- Ogni libreria ha un id interno, un nome, e appartiene a un utente
CREATE TABLE librerie (
    id       SERIAL PRIMARY KEY,
    userid   VARCHAR(50) NOT NULL REFERENCES utenti_registrati(userid) ON DELETE CASCADE,
    nome     VARCHAR(100) NOT NULL
);

-- Tabella di collegamento libreria ↔ libri (N:M)
CREATE TABLE librerie_libri (
    id_libreria INTEGER NOT NULL REFERENCES librerie(id) ON DELETE CASCADE,
    id_libro    INTEGER NOT NULL REFERENCES libri(id) ON DELETE CASCADE,
    PRIMARY KEY (id_libreria, id_libro)
);

-- ========== TABELLA VALUTAZIONI ==========
-- Una sola valutazione per coppia (utente, libro)
CREATE TABLE valutazioni_libri (
    userid        VARCHAR(50) NOT NULL REFERENCES utenti_registrati(userid) ON DELETE CASCADE,
    id_libro      INTEGER NOT NULL REFERENCES libri(id) ON DELETE CASCADE,

    stile         INTEGER NOT NULL CHECK (stile BETWEEN 1 AND 5),
    contenuto     INTEGER NOT NULL CHECK (contenuto BETWEEN 1 AND 5),
    gradevolezza  INTEGER NOT NULL CHECK (gradevolezza BETWEEN 1 AND 5),
    originalita   INTEGER NOT NULL CHECK (originalita BETWEEN 1 AND 5),
    edizione      INTEGER NOT NULL CHECK (edizione BETWEEN 1 AND 5),
    voto_finale   INTEGER NOT NULL CHECK (voto_finale BETWEEN 1 AND 5),

    commento      VARCHAR(256),

    PRIMARY KEY (userid, id_libro)
);

-- ========== TABELLE SUGGERIMENTI ==========
-- libro_sorgente = libro visualizzato
-- libro_consigliato = libro suggerito come collegato
CREATE TABLE consigli_libri (
    userid           VARCHAR(50) NOT NULL REFERENCES utenti_registrati(userid) ON DELETE CASCADE,
    id_libro_sorgente   INTEGER NOT NULL REFERENCES libri(id) ON DELETE CASCADE,
    id_libro_consigliato INTEGER NOT NULL REFERENCES libri(id) ON DELETE CASCADE,

    PRIMARY KEY (userid, id_libro_sorgente, id_libro_consigliato)
);
