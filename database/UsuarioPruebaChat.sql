-- ============================================================
--  Insertar cliente de prueba para el Chatbot
--  ID: 123456  (el que usa PruebaChatbot.java)
-- ============================================================

-- 1. Insertar en USUARIOS (super-tipo)
INSERT INTO USUARIOS (
    numero_identificacion,
    tipo_identificacion,
    nombre,
    apellidos,
    telefono,
    correo,
    contrasena_hash,
    estado,
    fecha_registro,
    tipo_usuario
) VALUES (
    '123456',
    'CC',
    'Cliente',
    'Prueba',
    '3001234567',
    'cliente.prueba@email.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- "password"
    'ACTIVO',
    SYSDATE,
    'CLIENTE'
);

-- 2. Insertar en CLIENTES (sub-tipo)
INSERT INTO CLIENTES (
    numero_identificacion,
    direccion,
    fecha_nacimiento
) VALUES (
    '123456',
    'Calle 1 # 2-3, Valledupar',
    TO_DATE('1995-06-15', 'YYYY-MM-DD')
);

COMMIT;

-- Verificar
SELECT u.numero_identificacion, u.nombre, u.apellidos, u.tipo_usuario, u.estado
FROM USUARIOS u
LEFT JOIN CLIENTES c ON u.numero_identificacion = c.numero_identificacion
WHERE u.numero_identificacion = '123456';
