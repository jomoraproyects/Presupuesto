-- Dependencias iniciales; conservar las áreas existentes y su historial.
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'FIN', 'Financiera', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'FIN');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'COM', 'Comercial', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'COM');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'PLA', 'Planeación', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'PLA');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'TEC', 'Técnica', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'TEC');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'SG', 'Secretaría General', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'SG');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'CIG', 'Control Interno Gestión', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'CIG');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'CID', 'Control Interno Disciplinario', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'CID');
INSERT INTO area (codigo, nombre, activa, created_at, updated_at)
SELECT 'GER', 'Gerencia', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM area WHERE codigo = 'GER');
