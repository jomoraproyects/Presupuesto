CREATE TABLE rol (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE
);
CREATE TABLE area (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(150) NOT NULL,
  descripcion VARCHAR(500),
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  area_padre_id BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_area_padre FOREIGN KEY (area_padre_id) REFERENCES area(id)
);
CREATE TABLE usuario (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  rol_id BIGINT NOT NULL,
  area_id BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
  CONSTRAINT fk_usuario_area FOREIGN KEY (area_id) REFERENCES area(id)
);
CREATE TABLE servicio (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(150) NOT NULL,
  descripcion VARCHAR(500),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);
CREATE TABLE vigencia (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  anio INT NOT NULL UNIQUE,
  nombre VARCHAR(150) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  presupuesto_disponible DECIMAL(19,2) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);
CREATE TABLE rubro_presupuestal (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(60) NOT NULL,
  nombre VARCHAR(200) NOT NULL,
  descripcion VARCHAR(500),
  rubro_padre_id BIGINT,
  nivel INT NOT NULL,
  permite_movimiento BOOLEAN NOT NULL DEFAULT FALSE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  vigencia_id BIGINT,
  orden INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_rubro_padre FOREIGN KEY (rubro_padre_id) REFERENCES rubro_presupuestal(id),
  CONSTRAINT fk_rubro_vigencia FOREIGN KEY (vigencia_id) REFERENCES vigencia(id),
  INDEX ix_rubro_padre (rubro_padre_id),
  INDEX ix_rubro_vigencia (vigencia_id)
);
