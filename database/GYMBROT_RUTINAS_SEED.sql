-- ============================================================
--  GYMBROT — Rutinas de ejemplo para instructores
-- ============================================================

DECLARE
    v_id_rutina NUMBER;
BEGIN

    -- ============================================================
    --  1. Diego Morales (2001000001) → Cliente Prueba (123456)
    --     Musculación Integral
    -- ============================================================
    INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo)
    VALUES ('2001000001', '123456', 'Rutina Musculacion Integral',
            'Rutina completa de hipertrofia con enfoque en pecho, espalda y piernas',
            SYSDATE, SYSDATE + 90, 'LUNES, MIERCOLES, VIERNES', 'Masa Muscular')
    RETURNING id_rutina INTO v_id_rutina;

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Banca'), 1, 'LUNES', '4x10, ultima serie al fallo');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Inclinado'), 2, 'LUNES', '4x10');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Aperturas'), 3, 'LUNES', '3x12, controlar negativa');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Fondos'), 4, 'LUNES', '4x10, peso corporal');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Curl Biceps'), 5, 'LUNES', '3x12, super serie con triceps');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Extension Triceps'), 6, 'LUNES', '3x12');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Remo con Barra'), 1, 'MIERCOLES', '4x10, espalda recta');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Jalon al Pecho'), 2, 'MIERCOLES', '3x12, agarre amplio');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Dominadas'), 3, 'MIERCOLES', '4x8, asistencia si es necesario');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Peso Muerto'), 1, 'VIERNES', '5x5, tecnica ante todo');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Sentadilla'), 2, 'VIERNES', '4x8, profundidad completa');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Prensa de Piernas'), 3, 'VIERNES', '3x12');

    -- ============================================================
    --  2. Andres Lopez (2001000003) → Rosa Mora CLI011
    --     Cardio y Resistencia Adulto Mayor
    -- ============================================================
    INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo)
    VALUES ('2001000003', 'CLI011', 'Rutina Cardio Adulto Mayor',
            'Rutina suave de cardio y resistencia para adulto mayor, bajo impacto',
            SYSDATE, SYSDATE + 60, 'MARTES, JUEVES, SABADO', 'Resistencia')
    RETURNING id_rutina INTO v_id_rutina;

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Zancadas'), 1, 'MARTES', '3x10, sin peso, controlar equilibrio');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Prensa de Piernas'), 2, 'MARTES', '3x12, peso ligero');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Crunches'), 3, 'MARTES', '3x15, abdominales suaves');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Plancha'), 4, 'MARTES', '3x20 seg, modificar si duele espalda');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Elevaciones Laterales'), 1, 'JUEVES', '3x10, mancuernas ligeras');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Curl Biceps'), 2, 'JUEVES', '3x10, peso ligero');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Extension Triceps'), 3, 'JUEVES', '3x10');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Aperturas'), 1, 'SABADO', '3x10, mancuernas ligeras');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Militar'), 2, 'SABADO', '3x8, mancuernas ligeras');

    -- ============================================================
    --  3. Sebastian Castro (2001000005) → Sofia Reyes CLI009
    --     Tren Superior Juvenil
    -- ============================================================
    INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo)
    VALUES ('2001000005', 'CLI009', 'Rutina Tren Superior Juvenil',
            'Rutina adaptada para menor de edad, enfoque en tecnica y fuerza funcional',
            SYSDATE, SYSDATE + 30, 'LUNES, MIERCOLES', 'Fuerza')
    RETURNING id_rutina INTO v_id_rutina;

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Banca'), 1, 'LUNES', '3x10, barra sola para tecnica');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Remo con Barra'), 2, 'LUNES', '3x10, peso moderado');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Militar'), 3, 'LUNES', '3x8, mancuernas');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Curl Biceps'), 4, 'LUNES', '3x10');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Dominadas'), 1, 'MIERCOLES', '3x5, asistencia con banda');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Fondos'), 2, 'MIERCOLES', '3x8');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Elevaciones Laterales'), 3, 'MIERCOLES', '3x10');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Inclinado'), 4, 'MIERCOLES', '3x10');

    -- ============================================================
    --  4. Felipe Ortiz (2001000007) → Mateo Castro CLI010
    --     Tren Inferior Juvenil
    -- ============================================================
    INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo)
    VALUES ('2001000007', 'CLI010', 'Rutina Piernas Juvenil',
            'Desarrollo de tren inferior con enfasis en tecnica de sentadilla y peso muerto',
            SYSDATE, SYSDATE + 30, 'MARTES, JUEVES', 'Fuerza')
    RETURNING id_rutina INTO v_id_rutina;

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Sentadilla'), 1, 'MARTES', '3x10, barra sola perfeccionar tecnica');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Prensa de Piernas'), 2, 'MARTES', '3x12');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Zancadas'), 3, 'MARTES', '3x10, sin peso');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Peso Muerto'), 1, 'JUEVES', '3x8, peso ligero, cuidado con lumbar');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Peso Muerto Rumano'), 2, 'JUEVES', '3x8');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Plancha'), 3, 'JUEVES', '3x30 seg');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Crunches'), 4, 'JUEVES', '3x15');

    -- ============================================================
    --  5. Julian Prada (2001000009) → Carlos Lopez CLI012
    --     Funcional Adulto Mayor
    -- ============================================================
    INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo)
    VALUES ('2001000009', 'CLI012', 'Rutina Funcional Adulto Mayor',
            'Ejercicios funcionales para mejorar movilidad, equilibrio y fuerza general',
            SYSDATE, SYSDATE + 60, 'LUNES, MIERCOLES, VIERNES', 'Resistencia')
    RETURNING id_rutina INTO v_id_rutina;

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Sentadilla'), 1, 'LUNES', '3x10, solo peso corporal');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Elevaciones Laterales'), 2, 'LUNES', '3x10, mancuernas 2kg');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Plancha'), 3, 'LUNES', '3x15 seg');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Curl Biceps'), 1, 'MIERCOLES', '3x10, mancuernas 3kg');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Extension Triceps'), 2, 'MIERCOLES', '3x10');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Press Militar'), 3, 'MIERCOLES', '3x8, mancuernas ligeras');

    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Zancadas'), 1, 'VIERNES', '3x8, cortas, apoyado en pared');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Crunches'), 2, 'VIERNES', '3x12');
    INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
    VALUES (v_id_rutina, (SELECT id_ejercicio FROM EJERCICIOS WHERE nombre='Aperturas'), 3, 'VIERNES', '3x10, mancuernas 2kg');

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
