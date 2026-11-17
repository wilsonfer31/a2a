-- Create the fournisseur table
CREATE TABLE IF NOT EXISTS fournisseur (
    id     SERIAL PRIMARY KEY,
    nom    VARCHAR(255) NOT NULL,
    siret  VARCHAR(14)  NOT NULL,
    ville  VARCHAR(100) NOT NULL
);

-- Insert 10 fictional suppliers
INSERT INTO fournisseur (nom, siret, ville) VALUES
('Dupont SAS',              '12345678901234', 'Paris'),
('Martin & Fils SARL',      '23456789012345', 'Lyon'),
('Lefebvre Industries',     '34567890123456', 'Marseille'),
('Bernard Technologie',     '45678901234567', 'Toulouse'),
('Moreau Fournitures',      '56789012345678', 'Bordeaux'),
('Petit Commerce SARL',     '67890123456789', 'Nantes'),
('Roux & Associés SAS',     '78901234567890', 'Strasbourg'),
('Simon Équipements',       '89012345678901', 'Lille'),
('Laurent Distribution',    '90123456789012', 'Nice'),
('Thomas Services SARL',    '01234567890123', 'Rennes');

-- Create read-only user for MCP server
CREATE USER mcp_reader WITH PASSWORD 'mcp_pass';
GRANT CONNECT ON DATABASE fournisseurs TO mcp_reader;
GRANT USAGE ON SCHEMA public TO mcp_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO mcp_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO mcp_reader;
