ALTER TABLE area_vigencia ADD COLUMN enviada BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE area_vigencia SET enviada = TRUE WHERE EXISTS (
    SELECT 1 FROM necesidad_presupuestal n
    WHERE n.area_id = area_vigencia.area_id AND n.vigencia_id = area_vigencia.vigencia_id
      AND n.activa = TRUE AND n.estado IN ('ENVIADA', 'EN_REVISION', 'APROBADA', 'APROBADA_CON_AJUSTES', 'RECHAZADA')
);
