-- ============================================================
-- GYMBROT — Seed Data v1.1
-- ============================================================

-- ── ESPECIALIDADES ────────────────────────────────────────────
-- NOTA: id_especialidad es GENERATED ALWAYS, no se inserta
INSERT INTO ESPECIALIDADES (nombre) VALUES ('Musculación');
INSERT INTO ESPECIALIDADES (nombre) VALUES ('Cardio y Resistencia');
INSERT INTO ESPECIALIDADES (nombre) VALUES ('Tren Superior');
INSERT INTO ESPECIALIDADES (nombre) VALUES ('Tren Inferior');
INSERT INTO ESPECIALIDADES (nombre) VALUES ('Funcional');

-- ── ADMINISTRADOR ─────────────────────────────────────────────
INSERT INTO USUARIOS (
    numero_identificacion, tipo_identificacion, nombre, apellidos,
    telefono, correo, contrasena_hash, foto_url, estado,
    fecha_registro, tipo_usuario)
VALUES ('1001000001', 'CC', 'Carlos', 'Ramírez Gómez',
    '3001000001', 'carlos.ramirez@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    NULL, 'ACTIVO', SYSDATE, 'ADMINISTRADOR');
INSERT INTO ADMINISTRADORES (numero_identificacion, rol) VALUES ('1001000001', 'SUPERADMIN');

-- ── INSTRUCTORES ──────────────────────────────────────────────
INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000001', 'CC', 'Diego', 'Morales Peña', '3101000001', 'diego.morales@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000001', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Musculación'), 'Lunes a Viernes 6am-2pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000002', 'CC', 'Luisa', 'Vargas López', '3101000002', 'luisa.vargas@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000002', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Musculación'), 'Lunes a Viernes 2pm-10pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000003', 'CC', 'Andrés', 'López Herrera', '3101000003', 'andres.lopez@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000003', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Cardio y Resistencia'), 'Lunes a Sábado 6am-2pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000004', 'CC', 'Camila', 'Ríos Mendoza', '3101000004', 'camila.rios@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000004', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Cardio y Resistencia'), 'Martes a Domingo 2pm-10pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000005', 'CC', 'Sebastián', 'Castro Díaz', '3101000005', 'sebastian.castro@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000005', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Tren Superior'), 'Lunes a Viernes 6am-2pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000006', 'CC', 'Natalia', 'Gómez Suárez', '3101000006', 'natalia.gomez@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000006', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Tren Superior'), 'Lunes a Sábado 2pm-10pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000007', 'CC', 'Felipe', 'Ortiz Navarro', '3101000007', 'felipe.ortiz@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000007', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Tren Inferior'), 'Lunes a Viernes 6am-2pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000008', 'CC', 'Isabella', 'Méndez Castillo', '3101000008', 'isabella.mendez@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000008', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Tren Inferior'), 'Martes a Domingo 2pm-10pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000009', 'CC', 'Julián', 'Prada Bermúdez', '3101000009', 'julian.prada@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000009', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Funcional'), 'Lunes a Viernes 6am-2pm', SYSDATE);

INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos, telefono, correo, contrasena_hash, foto_url, estado, fecha_registro, tipo_usuario)
VALUES ('2001000010', 'CC', 'Sara', 'Quintero Lozano', '3101000010', 'sara.quintero@gymbrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ACTIVO', SYSDATE, 'INSTRUCTOR');
INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
VALUES ('2001000010', (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre='Funcional'), 'Lunes a Sábado 2pm-10pm', SYSDATE);

-- ── PLANES DE MEMBRESÍA ───────────────────────────────────────
INSERT INTO PLANES_MEMBRESIAS (nombre, descripcion, precio_mensual, precio_semestral, precio_anual, beneficios)
VALUES ('Plan Silver', 'Acceso básico a sala de pesas y cardio',
    80000, 440000, 840000,
    'Acceso sala pesas - Acceso cardio - Casillero básico');

INSERT INTO PLANES_MEMBRESIAS (nombre, descripcion, precio_mensual, precio_semestral, precio_anual, beneficios)
VALUES ('Plan Gold', 'Acceso completo más clases grupales',
    130000, 710000, 1360000,
    'Acceso sala pesas - Acceso cardio - Casillero básico - Clases grupales - Toalla');

INSERT INTO PLANES_MEMBRESIAS (nombre, descripcion, precio_mensual, precio_semestral, precio_anual, beneficios)
VALUES ('Plan Black', 'Acceso premium con instructor personal y nutricionista',
    200000, 1100000, 2100000,
    'Acceso sala pesas - Acceso cardio - Casillero básico - Clases grupales - Toalla - Instructor personal - Nutricionista - Casillero VIP - Bebida');

-- ── EJERCICIOS ────────────────────────────────────────────────
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Press Banca', 'Pecho', 'INTERMEDIO', 'Acostado en banco, barra al pecho y extiende', 4, 10);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Sentadilla', 'Piernas', 'INTERMEDIO', 'Barra en trapecios, cadera abajo y arriba', 4, 8);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Peso Muerto', 'Espalda', 'AVANZADO', 'Barra en suelo, espalda recta, levanta', 5, 5);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Dominadas', 'Espalda', 'INTERMEDIO', 'Agarrar barra, subir hasta menton', 4, 8);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Fondos', 'Triceps', 'INTERMEDIO', 'En paralelas, baja y empuja', 4, 10);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Remo con Barra', 'Espalda', 'INTERMEDIO', 'Inclinado, barra al abdomen', 4, 10);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Press Militar', 'Hombros', 'INTERMEDIO', 'Barra desde hombros hasta extension', 4, 8);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Curl Biceps', 'Biceps', 'PRINCIPIANTE', 'Mancuernas, flexion de codo', 3, 12);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Extension Triceps', 'Triceps', 'PRINCIPIANTE', 'Polea alta, extension completa', 3, 12);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Elevaciones Laterales', 'Hombros', 'PRINCIPIANTE', 'Mancuernas laterales hasta altura hombro', 3, 12);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Jalon al Pecho', 'Espalda', 'PRINCIPIANTE', 'Polea alta, barra al pecho', 3, 12);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Prensa de Piernas', 'Piernas', 'PRINCIPIANTE', 'Plataforma, empuja con piernas', 3, 12);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Crunches', 'Abdomen', 'PRINCIPIANTE', 'Acostado, elevacion de tronco', 3, 15);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Plancha', 'Abdomen', 'INTERMEDIO', 'Antebrazos en suelo, cuerpo recto', 3, 30);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Zancadas', 'Piernas', 'INTERMEDIO', 'Paso al frente, rodilla trasera al suelo', 3, 12);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Press Inclinado', 'Pecho', 'INTERMEDIO', 'Banco 45°, barra a claviculas', 4, 10);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Peso Muerto Rumano', 'Gluteos', 'AVANZADO', 'Barra, cadera atras, torso firme', 4, 8);
INSERT INTO EJERCICIOS (nombre, grupo_muscular, nivel, descripcion, series, repeticiones)
VALUES ('Aperturas', 'Pecho', 'PRINCIPIANTE', 'Mancuernas, apertura pectoral', 3, 12);

COMMIT;
