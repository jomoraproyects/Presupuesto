ALTER TABLE area_vigencia ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE';
UPDATE area_vigencia SET estado = 'ENVIADO' WHERE enviada = TRUE;
UPDATE area_vigencia SET estado = 'APROBADO' WHERE enviada = TRUE AND EXISTS (
  SELECT 1 FROM necesidad_presupuestal n WHERE n.vigencia_id = area_vigencia.vigencia_id
  AND n.area_id = area_vigencia.area_id AND n.activa = TRUE AND n.estado IN ('APROBADA', 'APROBADA_CON_AJUSTES', 'RECHAZADA')
) AND NOT EXISTS (
  SELECT 1 FROM necesidad_presupuestal n WHERE n.vigencia_id = area_vigencia.vigencia_id
  AND n.area_id = area_vigencia.area_id AND n.activa = TRUE AND n.estado NOT IN ('APROBADA', 'APROBADA_CON_AJUSTES', 'RECHAZADA')
);
UPDATE area_vigencia SET estado = 'BORRADOR' WHERE enviada = FALSE AND EXISTS (
  SELECT 1 FROM necesidad_presupuestal n WHERE n.vigencia_id = area_vigencia.vigencia_id
  AND n.area_id = area_vigencia.area_id AND n.activa = TRUE
);
ALTER TABLE distribucion_necesidad ADD COLUMN porcentaje_aprobado DECIMAL(5,2);
UPDATE distribucion_necesidad SET porcentaje_aprobado = porcentaje;
