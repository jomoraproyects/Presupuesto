-- El perfil central único es SUPER_ADMIN. Conservar IDs, contraseñas e históricos.
-- Retirar únicamente la cuenta separada de desarrollo, si aún tenía el rol anterior.
UPDATE usuario
SET activo = FALSE, updated_at = CURRENT_TIMESTAMP
WHERE username = 'presupuesto'
  AND rol_id = (SELECT id FROM rol WHERE nombre = 'PRESUPUESTO');

UPDATE usuario
SET rol_id = (SELECT id FROM rol WHERE nombre = 'SUPER_ADMIN'), updated_at = CURRENT_TIMESTAMP
WHERE rol_id = (SELECT id FROM rol WHERE nombre = 'PRESUPUESTO');

DELETE FROM rol WHERE nombre = 'PRESUPUESTO';
