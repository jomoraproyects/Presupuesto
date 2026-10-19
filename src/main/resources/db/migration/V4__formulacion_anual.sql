ALTER TABLE necesidad_presupuestal ADD COLUMN aplica_distribucion BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE rubro_presupuestal ADD COLUMN categoria_formulacion BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE area_vigencia (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  vigencia_id BIGINT NOT NULL,
  area_id BIGINT NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_participante_vigencia FOREIGN KEY (vigencia_id) REFERENCES vigencia(id),
  CONSTRAINT fk_participante_area FOREIGN KEY (area_id) REFERENCES area(id),
  CONSTRAINT uq_participante UNIQUE (vigencia_id, area_id)
);

-- Las solicitudes existentes conservan habilitada su área en su vigencia.
INSERT INTO area_vigencia (vigencia_id, area_id, activa, created_at, updated_at)
SELECT DISTINCT vigencia_id, area_id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM necesidad_presupuestal;

CREATE TABLE mapeo_rubro_servicio (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  vigencia_id BIGINT NOT NULL,
  rubro_origen_id BIGINT NOT NULL,
  servicio_id BIGINT NOT NULL,
  rubro_destino_id BIGINT NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_mapeo_vigencia FOREIGN KEY (vigencia_id) REFERENCES vigencia(id),
  CONSTRAINT fk_mapeo_origen FOREIGN KEY (rubro_origen_id) REFERENCES rubro_presupuestal(id),
  CONSTRAINT fk_mapeo_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id),
  CONSTRAINT fk_mapeo_destino FOREIGN KEY (rubro_destino_id) REFERENCES rubro_presupuestal(id),
  CONSTRAINT uq_mapeo UNIQUE (vigencia_id, rubro_origen_id, servicio_id)
);
