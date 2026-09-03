-- ====================================================================
--   SCRIPT DE INSTALACION DE BASE DE DATOS GYMBROT
--   INSTALAR_BD_GYMBROT.sql
--   ------------------------------------------------------------------
--   Crea la base de datos Oracle de GYMBROT desde cero en un PC nuevo.
--   GENERADO A PARTIR DEL ESQUEMA REAL DE LA BD (verificado contra
--   el modelo vivo usando DBMS_METADATA). No es copia de ningun script
--   previo: refleja EXACTAMENTE la estructura en produccion.
--
--   VERSION DE ORACLE OBJETIVO: Oracle XE 18c
--   PDB objetivo: XEPDB1
--
--   CONTENIDO:
--     A.  Provision de usuario y tablespace (seccion sysdba, opcional)
--     B.  19 tablas con constraints, identities y check constraints
--     C.  16 indices nombrados (IDX_*)
--     D.  Tipo auxiliar NUMBER_ARRAY
--     E.  Paquete PKG_GYMBROT_FUNC (6 funciones)
--     F.  Paquete PKG_GYMBROT_PROC (12 procedimientos)
--     G.  10 triggers
--     H.  Datos de catalogo (admin + especialidades + planes)
--     I.  Verificacion
--
--   COMO EJECUTAR (en el PC nuevo):
--     1. Como SYS (system/sysdba) descomenta y ajusta la seccion A
--        para crear el usuario gymbrot (solo la primera vez).
--     2. Conecta como gymbrot e ejecuta el resto del script:
--           sqlplus gymbrot/<password>@localhost:1521/XEPDB1 @INSTALAR_BD_GYMBROT.sql
--
--   CONFIGURACION DE LA APP:
--     GYMBROT_DB_URL  = jdbc:oracle:thin:@localhost:1521/XEPDB1
--     GYMBROT_DB_USER = gymbrot
--     GYMBROT_DB_PASS = <la contrasena del usuario gymbrot>
-- ====================================================================


-- ====================================================================
--  A. PROVISION DE USUARIO Y TABLESPACE  (OPCIONAL - solo la 1a vez)
--  ------------------------------------------------------------------
--  EJECUTAR COMO SYS o usuario con privilegios DBA.
--  Descomenta las siguientes lineas y remplaza los valores si difieren.
-- ====================================================================

-- CREATE TABLESPACE GYMBROT_DATA
--   DATAFILE 'C:\app\<usuario>\oradata\XE\XEPDB1\gymbrot_data01.dbf'
--   SIZE 100M AUTOEXTEND ON NEXT 50M MAXSIZE 1G;
--
-- CREATE USER GYMBROT IDENTIFIED BY "tu_password"
--   DEFAULT TABLESPACE GYMBROT_DATA
--   TEMPORARY TABLESPACE TEMP
--   QUOTA UNLIMITED ON GYMBROT_DATA;
--
-- GRANT CONNECT, RESOURCE TO GYMBROT;
-- GRANT CREATE SESSION TO GYMBROT;


-- ====================================================================
--  B. CREACION DE TABLAS (19)
--  ====================================================================
--  El orden respeta las dependencias de claves foraneas.

-- --------------------------------------------------------------------
--  1. ESPECIALIDADES
-- --------------------------------------------------------------------
CREATE TABLE ESPECIALIDADES (
    id_especialidad NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    nombre VARCHAR2(30) NOT NULL ENABLE,
    CONSTRAINT PK_ESPECIALIDADES PRIMARY KEY (id_especialidad)
);

-- --------------------------------------------------------------------
--  2. PLANES_MEMBRESIAS
-- --------------------------------------------------------------------
CREATE TABLE PLANES_MEMBRESIAS (
    id_plan NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    nombre VARCHAR2(30) NOT NULL ENABLE,
    descripcion VARCHAR2(155),
    precio_mensual NUMBER(10,2),
    precio_semestral NUMBER(10,2),
    precio_anual NUMBER(10,2),
    beneficios CLOB,
    CONSTRAINT PK_PLANES_MEMBRESIAS PRIMARY KEY (id_plan)
);

-- --------------------------------------------------------------------
--  3. PLANTILLAS_MENSAJE
-- --------------------------------------------------------------------
CREATE TABLE PLANTILLAS_MENSAJE (
    id_plantilla NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    nombre VARCHAR2(50) NOT NULL ENABLE,
    tipo VARCHAR2(20) NOT NULL ENABLE,
    asunto VARCHAR2(100),
    cuerpo_html CLOB,
    cuerpo_texto CLOB,
    variables_disponibles VARCHAR2(255),
    activa NUMBER(1,0) DEFAULT 1 NOT NULL ENABLE,
    CONSTRAINT CK_PLANTILLA_TIPO CHECK (tipo IN ('SMS','EMAIL','WHATSAPP')) ENABLE,
    CONSTRAINT CK_PLANTILLA_ACTIVA CHECK (activa IN (0,1)) ENABLE,
    CONSTRAINT PK_PLANTILLAS_MENSAJE PRIMARY KEY (id_plantilla)
);

-- --------------------------------------------------------------------
--  4. USUARIOS  (supertipo)
-- --------------------------------------------------------------------
CREATE TABLE USUARIOS (
    numero_identificacion VARCHAR2(20) NOT NULL ENABLE,
    tipo_identificacion VARCHAR2(10) NOT NULL ENABLE,
    nombre VARCHAR2(25) NOT NULL ENABLE,
    apellidos VARCHAR2(25) NOT NULL ENABLE,
    telefono VARCHAR2(20),
    correo VARCHAR2(50),
    contrasena_hash VARCHAR2(255),
    foto_url VARCHAR2(105),
    estado VARCHAR2(20) DEFAULT 'ACTIVO',
    fecha_registro DATE DEFAULT SYSDATE,
    tipo_usuario VARCHAR2(20) NOT NULL ENABLE,
    CONSTRAINT CK_USU_TIPO_ID CHECK (tipo_identificacion IN ('CC','CE','PP','TI')) ENABLE,
    CONSTRAINT CK_USU_ESTADO CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO','BLOQUEADO')) ENABLE,
    CONSTRAINT CK_USU_TIPO CHECK (tipo_usuario IN ('CLIENTE','INSTRUCTOR','ADMINISTRADOR')) ENABLE,
    CONSTRAINT PK_USUARIOS PRIMARY KEY (numero_identificacion)
);

-- --------------------------------------------------------------------
--  5. CLIENTES
-- --------------------------------------------------------------------
CREATE TABLE CLIENTES (
    numero_identificacion VARCHAR2(20) NOT NULL ENABLE,
    direccion VARCHAR2(50),
    fecha_nacimiento DATE,
    huella_dactilar BLOB,
    CONSTRAINT PK_CLIENTES PRIMARY KEY (numero_identificacion),
    CONSTRAINT FK_CLI_USUARIO FOREIGN KEY (numero_identificacion)
        REFERENCES USUARIOS (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  6. INSTRUCTORES
-- --------------------------------------------------------------------
CREATE TABLE INSTRUCTORES (
    numero_identificacion VARCHAR2(20) NOT NULL ENABLE,
    id_especialidad NUMBER NOT NULL ENABLE,
    disponibilidad VARCHAR2(100),
    fecha_contratacion DATE,
    CONSTRAINT PK_INSTRUCTORES PRIMARY KEY (numero_identificacion),
    CONSTRAINT FK_INST_USUARIO FOREIGN KEY (numero_identificacion)
        REFERENCES USUARIOS (numero_identificacion) ENABLE,
    CONSTRAINT FK_INST_ESPECIALIDAD FOREIGN KEY (id_especialidad)
        REFERENCES ESPECIALIDADES (id_especialidad) ENABLE
);

-- --------------------------------------------------------------------
--  7. ADMINISTRADORES
-- --------------------------------------------------------------------
CREATE TABLE ADMINISTRADORES (
    numero_identificacion VARCHAR2(20) NOT NULL ENABLE,
    rol VARCHAR2(30) DEFAULT 'ADMIN',
    CONSTRAINT CK_ADM_ROL CHECK (rol IN ('SUPERADMIN','ADMIN','RECEPCION')) ENABLE,
    CONSTRAINT PK_ADMINISTRADORES PRIMARY KEY (numero_identificacion),
    CONSTRAINT FK_ADM_USUARIO FOREIGN KEY (numero_identificacion)
        REFERENCES USUARIOS (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  8. MEMBRESIAS
-- --------------------------------------------------------------------
CREATE TABLE MEMBRESIAS (
    id_membresia NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_plan NUMBER NOT NULL ENABLE,
    tipo_membresia VARCHAR2(50),
    modalidad_pago VARCHAR2(20),
    valor NUMBER(10,2),
    fecha_inicio DATE,
    fecha_vencimiento DATE,
    estado VARCHAR2(20) DEFAULT 'ACTIVA',
    CONSTRAINT CK_MEMB_MODALIDAD CHECK (modalidad_pago IN ('MENSUAL','SEMESTRAL','ANUAL')) ENABLE,
    CONSTRAINT CK_MEMB_ESTADO CHECK (estado IN ('ACTIVA','VENCIDA','SUSPENDIDA','CANCELADA')) ENABLE,
    CONSTRAINT PK_MEMBRESIAS PRIMARY KEY (id_membresia),
    CONSTRAINT FK_MEMB_PLAN FOREIGN KEY (id_plan)
        REFERENCES PLANES_MEMBRESIAS (id_plan) ENABLE
);

-- --------------------------------------------------------------------
--  9. HISTORIAL_MEMBRESIAS
-- --------------------------------------------------------------------
CREATE TABLE HISTORIAL_MEMBRESIAS (
    id_historial NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    id_membresia NUMBER NOT NULL ENABLE,
    fecha_asignacion DATE DEFAULT SYSDATE NOT NULL ENABLE,
    fecha_fin DATE,
    activa NUMBER(1,0) DEFAULT 1 NOT NULL ENABLE,
    CONSTRAINT CK_HIST_ACTIVA CHECK (activa IN (0,1)) ENABLE,
    CONSTRAINT PK_HISTORIAL_MEMBRESIAS PRIMARY KEY (id_historial),
    CONSTRAINT FK_HIST_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE,
    CONSTRAINT FK_HIST_MEMBRESIA FOREIGN KEY (id_membresia)
        REFERENCES MEMBRESIAS (id_membresia) ENABLE
);

-- --------------------------------------------------------------------
--  10. PAGOS
-- --------------------------------------------------------------------
CREATE TABLE PAGOS (
    id_pago NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_membresia NUMBER NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    fecha_pago DATE DEFAULT SYSDATE NOT NULL ENABLE,
    valor NUMBER(10,2) NOT NULL ENABLE,
    metodo_pago VARCHAR2(30) NOT NULL ENABLE,
    estado_pago VARCHAR2(20) DEFAULT 'EXITOSO',
    referencia_transaccion VARCHAR2(50),
    observaciones VARCHAR2(255),
    CONSTRAINT CK_PAGO_METODO CHECK (metodo_pago IN
        ('EFECTIVO','TRANSFERENCIA','TARJETA','NEQUI','DAVIPLATA')) ENABLE,
    CONSTRAINT CK_PAGO_ESTADO CHECK (estado_pago IN ('EXITOSO','RECHAZADO','PENDIENTE')) ENABLE,
    CONSTRAINT PK_PAGOS PRIMARY KEY (id_pago),
    CONSTRAINT FK_PAGO_MEMBRESIA FOREIGN KEY (id_membresia)
        REFERENCES MEMBRESIAS (id_membresia) ENABLE,
    CONSTRAINT FK_PAGO_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  11. EJERCICIOS
-- --------------------------------------------------------------------
CREATE TABLE EJERCICIOS (
    id_ejercicio NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    nombre VARCHAR2(25) NOT NULL ENABLE,
    descripcion CLOB,
    grupo_muscular VARCHAR2(25),
    series NUMBER(3,0),
    repeticiones NUMBER(3,0),
    duracion_minutos NUMBER(5,0),
    nivel VARCHAR2(20),
    recurso_url VARCHAR2(105),
    estado VARCHAR2(10) DEFAULT 'ACTIVO',
    CONSTRAINT CK_EJERCICIO_NIVEL CHECK (nivel IN ('PRINCIPIANTE','INTERMEDIO','AVANZADO')) ENABLE,
    CONSTRAINT PK_EJERCICIOS PRIMARY KEY (id_ejercicio)
);

-- --------------------------------------------------------------------
--  12. RUTINAS
-- --------------------------------------------------------------------
CREATE TABLE RUTINAS (
    id_rutina NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_instructor VARCHAR2(20) NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    nombre VARCHAR2(50) NOT NULL ENABLE,
    descripcion CLOB,
    fecha_creacion DATE DEFAULT SYSDATE,
    fecha_fin DATE,
    dias_semana VARCHAR2(50),
    objetivo VARCHAR2(100),
    estado VARCHAR2(10) DEFAULT 'ACTIVO',
    CONSTRAINT PK_RUTINAS PRIMARY KEY (id_rutina),
    CONSTRAINT FK_RUT_INSTRUCTOR FOREIGN KEY (id_instructor)
        REFERENCES INSTRUCTORES (numero_identificacion) ENABLE,
    CONSTRAINT FK_RUT_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  13. RUTINA_EJERCICIOS
-- --------------------------------------------------------------------
CREATE TABLE RUTINA_EJERCICIOS (
    id_rutina NUMBER NOT NULL ENABLE,
    id_ejercicio NUMBER NOT NULL ENABLE,
    orden NUMBER(3,0) NOT NULL ENABLE,
    dia_semana VARCHAR2(15),
    notas_instructor CLOB,
    CONSTRAINT CK_RUTEJE_DIA CHECK (dia_semana IN
        ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')) ENABLE,
    CONSTRAINT PK_RUTINA_EJERCICIOS PRIMARY KEY (id_rutina, id_ejercicio),
    CONSTRAINT FK_RUTEJE_RUTINA FOREIGN KEY (id_rutina)
        REFERENCES RUTINAS (id_rutina) ENABLE,
    CONSTRAINT FK_RUTEJE_EJERCICIO FOREIGN KEY (id_ejercicio)
        REFERENCES EJERCICIOS (id_ejercicio) ENABLE
);

-- --------------------------------------------------------------------
--  14. PROGRESOS
-- --------------------------------------------------------------------
CREATE TABLE PROGRESOS (
    id_progreso NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    fecha_registro DATE DEFAULT SYSDATE NOT NULL ENABLE,
    peso NUMBER(6,2),
    altura NUMBER(5,2),
    imc NUMBER(5,2),
    porcentaje_grasa NUMBER(5,2),
    masa_muscular NUMBER(6,2),
    objetivo VARCHAR2(50),
    observaciones CLOB,
    estado VARCHAR2(10) DEFAULT 'ACTIVO',
    CONSTRAINT PK_PROGRESOS PRIMARY KEY (id_progreso),
    CONSTRAINT FK_PROG_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  15. CITAS
-- --------------------------------------------------------------------
CREATE TABLE CITAS (
    id_cita NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_instructor VARCHAR2(20) NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    fecha DATE NOT NULL ENABLE,
    hora TIMESTAMP(6),
    tipo_cita VARCHAR2(30),
    estado VARCHAR2(20) DEFAULT 'PENDIENTE',
    notas CLOB,
    CONSTRAINT CK_CITA_TIPO CHECK (tipo_cita IN ('EVALUACION','SEGUIMIENTO','NUTRICION','CONSULTA')) ENABLE,
    CONSTRAINT CK_CITA_ESTADO CHECK (estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','COMPLETADA')) ENABLE,
    CONSTRAINT PK_CITAS PRIMARY KEY (id_cita),
    CONSTRAINT FK_CITA_INSTRUCTOR FOREIGN KEY (id_instructor)
        REFERENCES INSTRUCTORES (numero_identificacion) ENABLE,
    CONSTRAINT FK_CITA_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  16. REGISTROS_INGRESOS
-- --------------------------------------------------------------------
CREATE TABLE REGISTROS_INGRESOS (
    id_ingreso NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    fecha DATE DEFAULT SYSDATE NOT NULL ENABLE,
    hora_entrada TIMESTAMP(6),
    hora_salida TIMESTAMP(6),
    metodo_verificacion VARCHAR2(20) DEFAULT 'HUELLA',
    estado_verificacion VARCHAR2(20) DEFAULT 'APROBADO',
    CONSTRAINT CK_REGI_METODO CHECK (metodo_verificacion IN ('HUELLA','QR','MANUAL')) ENABLE,
    CONSTRAINT CK_REGI_ESTADO CHECK (estado_verificacion IN ('APROBADO','RECHAZADO','ERROR_LECTOR')) ENABLE,
    CONSTRAINT PK_REGISTROS_INGRESOS PRIMARY KEY (id_ingreso),
    CONSTRAINT FK_REGI_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  17. NOTIFICACIONES
-- --------------------------------------------------------------------
CREATE TABLE NOTIFICACIONES (
    id_notificacion NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    id_plantilla NUMBER,
    tipo VARCHAR2(20) NOT NULL ENABLE,
    asunto VARCHAR2(100),
    contenido CLOB NOT NULL ENABLE,
    estado_envio VARCHAR2(20) DEFAULT 'PENDIENTE',
    fecha_envio TIMESTAMP(6) DEFAULT SYSTIMESTAMP,
    origen VARCHAR2(20) DEFAULT 'SISTEMA',
    CONSTRAINT CK_NOTIF_TIPO CHECK (tipo IN ('SMS','EMAIL','WHATSAPP')) ENABLE,
    CONSTRAINT CK_NOTIF_ESTADO CHECK (estado_envio IN ('PENDIENTE','ENVIADO','FALLIDO','CANCELADO')) ENABLE,
    CONSTRAINT CK_NOTIF_ORIGEN CHECK (origen IN ('BOT','SISTEMA','MANUAL')) ENABLE,
    CONSTRAINT PK_NOTIFICACIONES PRIMARY KEY (id_notificacion),
    CONSTRAINT FK_NOTIF_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE,
    CONSTRAINT FK_NOTIF_PLANTILLA FOREIGN KEY (id_plantilla)
        REFERENCES PLANTILLAS_MENSAJE (id_plantilla) ENABLE
);

-- --------------------------------------------------------------------
--  18. SESIONES_GYMBROT
-- --------------------------------------------------------------------
CREATE TABLE SESIONES_GYMBROT (
    id_sesion NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_cliente VARCHAR2(20) NOT NULL ENABLE,
    fecha DATE DEFAULT SYSDATE,
    hora_inicio TIMESTAMP(6),
    hora_fin TIMESTAMP(6),
    contexto_activo CLOB,
    CONSTRAINT PK_SESIONES_GYMBROT PRIMARY KEY (id_sesion),
    CONSTRAINT FK_SES_CLIENTE FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion) ENABLE
);

-- --------------------------------------------------------------------
--  19. MENSAJES_GYMBROT
-- --------------------------------------------------------------------
CREATE TABLE MENSAJES_GYMBROT (
    id_mensaje NUMBER GENERATED ALWAYS AS IDENTITY
        MINVALUE 1 MAXVALUE 9999999999999999999999999999
        INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE NOKEEP NOSCALE
        NOT NULL ENABLE,
    id_sesion NUMBER NOT NULL ENABLE,
    id_cita NUMBER,
    remitente VARCHAR2(10) NOT NULL ENABLE,
    contenido CLOB NOT NULL ENABLE,
    timestamp_msg TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
    tipo_intencion VARCHAR2(30),
    CONSTRAINT CK_MSG_REMITENTE CHECK (remitente IN ('CLIENTE','BOT')) ENABLE,
    CONSTRAINT CK_MSG_INTENCION CHECK (tipo_intencion IN
        ('CONSULTA','AGENDA_CITA','VER_PROGRESO','SALUDO','NOTIFICACION','OTRO')) ENABLE,
    CONSTRAINT PK_MENSAJES_GYMBROT PRIMARY KEY (id_mensaje),
    CONSTRAINT FK_MSG_SESION FOREIGN KEY (id_sesion)
        REFERENCES SESIONES_GYMBROT (id_sesion) ENABLE,
    CONSTRAINT FK_MSG_CITA FOREIGN KEY (id_cita)
        REFERENCES CITAS (id_cita) ENABLE
);


-- ====================================================================
--  C. INDICES NOMBRADOS (16)
--  ====================================================================
CREATE INDEX IDX_USU_CORREO ON USUARIOS (CORREO);
CREATE INDEX IDX_USU_TIPO ON USUARIOS (TIPO_USUARIO, ESTADO);
CREATE INDEX IDX_CITAS_CLIENTE_FECHA ON CITAS (ID_CLIENTE, FECHA);
CREATE INDEX IDX_CITAS_INSTRUCTOR ON CITAS (ID_INSTRUCTOR);
CREATE INDEX IDX_HIST_CLIENTE_ACTIVA ON HISTORIAL_MEMBRESIAS (ID_CLIENTE, ACTIVA);
CREATE INDEX IDX_MEMB_VENCIMIENTO ON MEMBRESIAS (FECHA_VENCIMIENTO, ESTADO);
CREATE INDEX IDX_MENSAJES_SESION ON MENSAJES_GYMBROT (ID_SESION);
CREATE INDEX IDX_NOTIF_CLIENTE_FECHA ON NOTIFICACIONES (ID_CLIENTE, FECHA_ENVIO);
CREATE INDEX IDX_NOTIF_ESTADO ON NOTIFICACIONES (ESTADO_ENVIO);
CREATE INDEX IDX_PAGOS_CLIENTE ON PAGOS (ID_CLIENTE, FECHA_PAGO);
CREATE INDEX IDX_PAGOS_MEMBRESIA ON PAGOS (ID_MEMBRESIA);
CREATE INDEX IDX_PROGRESOS_CLIENTE ON PROGRESOS (ID_CLIENTE);
CREATE INDEX IDX_REGI_CLIENTE_FECHA ON REGISTROS_INGRESOS (ID_CLIENTE, FECHA);
CREATE INDEX IDX_RUTINAS_CLIENTE ON RUTINAS (ID_CLIENTE);
CREATE INDEX IDX_RUTINAS_INSTRUCTOR ON RUTINAS (ID_INSTRUCTOR);
CREATE INDEX IDX_SESIONES_CLIENTE ON SESIONES_GYMBROT (ID_CLIENTE);


-- ====================================================================
--  D. TIPO AUXILIAR NUMBER_ARRAY
--  ====================================================================
CREATE OR REPLACE TYPE NUMBER_ARRAY AS TABLE OF NUMBER;
/


-- ====================================================================
--  E. PAQUETE PKG_GYMBROT_FUNC  (6 funciones)
--  ====================================================================
CREATE OR REPLACE PACKAGE PKG_GYMBROT_FUNC AS

    FUNCTION FN_CONTAR_MIEMBROS_ACTIVOS    RETURN NUMBER;
    FUNCTION FN_CONTAR_ACTIVOS_HOY         RETURN NUMBER;
    FUNCTION FN_INGRESOS_MES_ACTUAL        RETURN NUMBER;
    FUNCTION FN_CALCULAR_IMC(p_peso NUMBER, p_altura_mts NUMBER)
        RETURN NUMBER;
    FUNCTION FN_CALCULAR_EDAD(p_fecha_nac DATE) RETURN NUMBER;
    FUNCTION FN_VALIDAR_CONFLICTO_CITA(
        p_id_instructor VARCHAR2, p_fecha DATE, p_hora TIMESTAMP
    ) RETURN NUMBER;

END PKG_GYMBROT_FUNC;
/

CREATE OR REPLACE PACKAGE BODY PKG_GYMBROT_FUNC AS

    FUNCTION FN_CONTAR_MIEMBROS_ACTIVOS RETURN NUMBER IS
        v_total NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_total
        FROM USUARIOS
        WHERE tipo_usuario = 'CLIENTE' AND estado = 'ACTIVO';
        RETURN v_total;
    END FN_CONTAR_MIEMBROS_ACTIVOS;

    FUNCTION FN_CONTAR_ACTIVOS_HOY RETURN NUMBER IS
        v_total NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_total
        FROM REGISTROS_INGRESOS
        WHERE fecha = TRUNC(SYSDATE) AND hora_salida IS NULL;
        RETURN v_total;
    END FN_CONTAR_ACTIVOS_HOY;

    FUNCTION FN_INGRESOS_MES_ACTUAL RETURN NUMBER IS
        v_total NUMBER;
    BEGIN
        SELECT NVL(SUM(valor), 0) INTO v_total
        FROM PAGOS
        WHERE EXTRACT(MONTH FROM fecha_pago) = EXTRACT(MONTH FROM SYSDATE)
          AND EXTRACT(YEAR  FROM fecha_pago) = EXTRACT(YEAR  FROM SYSDATE)
          AND estado_pago = 'EXITOSO';
        RETURN v_total;
    END FN_INGRESOS_MES_ACTUAL;

    FUNCTION FN_CALCULAR_IMC(
        p_peso       NUMBER,
        p_altura_mts NUMBER
    ) RETURN NUMBER IS
    BEGIN
        IF p_altura_mts IS NULL OR p_altura_mts <= 0 THEN
            RETURN NULL;
        END IF;
        RETURN ROUND(p_peso / (p_altura_mts * p_altura_mts), 2);
    END FN_CALCULAR_IMC;

    FUNCTION FN_CALCULAR_EDAD(p_fecha_nac DATE) RETURN NUMBER IS
    BEGIN
        RETURN TRUNC(MONTHS_BETWEEN(SYSDATE, p_fecha_nac) / 12);
    END FN_CALCULAR_EDAD;

    FUNCTION FN_VALIDAR_CONFLICTO_CITA(
        p_id_instructor VARCHAR2,
        p_fecha         DATE,
        p_hora          TIMESTAMP
    ) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
        FROM CITAS
        WHERE id_instructor = p_id_instructor
          AND fecha = p_fecha
          AND hora = p_hora
          AND estado NOT IN ('CANCELADA', 'COMPLETADA');
        RETURN CASE WHEN v_count > 0 THEN 1 ELSE 0 END;
    END FN_VALIDAR_CONFLICTO_CITA;

END PKG_GYMBROT_FUNC;
/


-- ====================================================================
--  F. PAQUETE PKG_GYMBROT_PROC  (12 procedimientos)
--  ====================================================================
CREATE OR REPLACE PACKAGE PKG_GYMBROT_PROC AS

    PROCEDURE SP_REGISTRAR_CLIENTE(
        p_id           IN VARCHAR2,
        p_tipo_id      IN VARCHAR2,
        p_nombre       IN VARCHAR2,
        p_apellidos    IN VARCHAR2,
        p_telefono     IN VARCHAR2,
        p_correo       IN VARCHAR2,
        p_contrasena   IN VARCHAR2,
        p_direccion    IN VARCHAR2,
        p_fecha_nac    IN DATE,
        p_huella       IN BLOB DEFAULT NULL,
        p_codigo_out   OUT NUMBER,
        p_mensaje_out  OUT VARCHAR2
    );

    PROCEDURE SP_ACTUALIZAR_CLIENTE(
        p_id           IN VARCHAR2,
        p_nombre       IN VARCHAR2,
        p_apellidos    IN VARCHAR2,
        p_telefono     IN VARCHAR2,
        p_correo       IN VARCHAR2,
        p_direccion    IN VARCHAR2,
        p_fecha_nac    IN DATE,
        p_huella       IN BLOB DEFAULT NULL,
        p_codigo_out   OUT NUMBER,
        p_mensaje_out  OUT VARCHAR2
    );

    PROCEDURE SP_REGISTRAR_INSTRUCTOR(
        p_id              IN VARCHAR2,
        p_tipo_id         IN VARCHAR2,
        p_nombre          IN VARCHAR2,
        p_apellidos       IN VARCHAR2,
        p_telefono        IN VARCHAR2,
        p_correo          IN VARCHAR2,
        p_contrasena      IN VARCHAR2,
        p_id_especialidad IN NUMBER,
        p_disponibilidad  IN VARCHAR2,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_ACTUALIZAR_INSTRUCTOR(
        p_id              IN VARCHAR2,
        p_nombre          IN VARCHAR2,
        p_apellidos       IN VARCHAR2,
        p_telefono        IN VARCHAR2,
        p_correo          IN VARCHAR2,
        p_id_especialidad IN NUMBER,
        p_disponibilidad  IN VARCHAR2,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_RENOVAR_MEMBRESIA(
        p_id_cliente      IN VARCHAR2,
        p_id_plan         IN NUMBER,
        p_modalidad       IN VARCHAR2,
        p_metodo_pago     IN VARCHAR2,
        p_valor           IN NUMBER,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_REGISTRAR_PROGRESO(
        p_id_cliente      IN VARCHAR2,
        p_peso            IN NUMBER,
        p_altura          IN NUMBER,
        p_porc_grasa      IN NUMBER DEFAULT NULL,
        p_masa_muscular   IN NUMBER DEFAULT NULL,
        p_objetivo        IN VARCHAR2 DEFAULT NULL,
        p_observaciones   IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_CREAR_EJERCICIO(
        p_nombre          IN VARCHAR2,
        p_descripcion     IN VARCHAR2 DEFAULT NULL,
        p_grupo_muscular  IN VARCHAR2 DEFAULT NULL,
        p_series          IN NUMBER DEFAULT NULL,
        p_repeticiones    IN NUMBER DEFAULT NULL,
        p_duracion        IN NUMBER DEFAULT NULL,
        p_nivel           IN VARCHAR2 DEFAULT NULL,
        p_recurso_url     IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_ACTUALIZAR_EJERCICIO(
        p_id_ejercicio    IN NUMBER,
        p_nombre          IN VARCHAR2,
        p_descripcion     IN VARCHAR2 DEFAULT NULL,
        p_grupo_muscular  IN VARCHAR2 DEFAULT NULL,
        p_series          IN NUMBER DEFAULT NULL,
        p_repeticiones    IN NUMBER DEFAULT NULL,
        p_duracion        IN NUMBER DEFAULT NULL,
        p_nivel           IN VARCHAR2 DEFAULT NULL,
        p_recurso_url     IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_AGENDAR_CITA(
        p_id_instructor   IN VARCHAR2,
        p_id_cliente      IN VARCHAR2,
        p_fecha           IN DATE,
        p_hora            IN TIMESTAMP,
        p_tipo_cita       IN VARCHAR2,
        p_notas           IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_CANCELAR_CITA(
        p_id_cita         IN NUMBER,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_CREAR_RUTINA_COMPLETA(
        p_id_instructor   IN VARCHAR2,
        p_id_cliente      IN VARCHAR2,
        p_nombre          IN VARCHAR2,
        p_descripcion     IN VARCHAR2 DEFAULT NULL,
        p_fecha_fin       IN DATE DEFAULT NULL,
        p_dias_semana     IN VARCHAR2 DEFAULT NULL,
        p_objetivo        IN VARCHAR2 DEFAULT NULL,
        p_ejercicios      IN NUMBER_ARRAY DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

    PROCEDURE SP_REGISTRAR_INGRESO(
        p_id_cliente          IN VARCHAR2,
        p_metodo_verificacion IN VARCHAR2 DEFAULT 'HUELLA',
        p_codigo_out          OUT NUMBER,
        p_mensaje_out         OUT VARCHAR2
    );

    PROCEDURE SP_REGISTRAR_SALIDA(
        p_id_cliente      IN VARCHAR2,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    );

END PKG_GYMBROT_PROC;
/

CREATE OR REPLACE PACKAGE BODY PKG_GYMBROT_PROC AS

    PROCEDURE SP_REGISTRAR_CLIENTE(
        p_id           IN VARCHAR2,
        p_tipo_id      IN VARCHAR2,
        p_nombre       IN VARCHAR2,
        p_apellidos    IN VARCHAR2,
        p_telefono     IN VARCHAR2,
        p_correo       IN VARCHAR2,
        p_contrasena   IN VARCHAR2,
        p_direccion    IN VARCHAR2,
        p_fecha_nac    IN DATE,
        p_huella       IN BLOB DEFAULT NULL,
        p_codigo_out   OUT NUMBER,
        p_mensaje_out  OUT VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        BEGIN
            SELECT 1 INTO p_codigo_out FROM USUARIOS
            WHERE numero_identificacion = p_id AND ROWNUM = 1;
            p_codigo_out  := 0;
            p_mensaje_out := 'Ya existe un usuario con esa identificacion.';
            RETURN;
        EXCEPTION WHEN NO_DATA_FOUND THEN NULL;
        END;

        INSERT INTO USUARIOS (
            numero_identificacion, tipo_identificacion, nombre, apellidos,
            telefono, correo, contrasena_hash, estado, fecha_registro, tipo_usuario
        ) VALUES (
            p_id, p_tipo_id, p_nombre, p_apellidos,
            p_telefono, p_correo, p_contrasena, 'ACTIVO', SYSDATE, 'CLIENTE'
        );

        INSERT INTO CLIENTES (
            numero_identificacion, direccion, fecha_nacimiento, huella_dactilar
        ) VALUES (
            p_id, p_direccion, p_fecha_nac, p_huella
        );

        COMMIT;
        p_codigo_out  := 1;
        p_mensaje_out := 'Cliente registrado exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_REGISTRAR_CLIENTE;

    PROCEDURE SP_ACTUALIZAR_CLIENTE(
        p_id           IN VARCHAR2,
        p_nombre       IN VARCHAR2,
        p_apellidos    IN VARCHAR2,
        p_telefono     IN VARCHAR2,
        p_correo       IN VARCHAR2,
        p_direccion    IN VARCHAR2,
        p_fecha_nac    IN DATE,
        p_huella       IN BLOB DEFAULT NULL,
        p_codigo_out   OUT NUMBER,
        p_mensaje_out  OUT VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        UPDATE USUARIOS SET
            nombre     = p_nombre,
            apellidos  = p_apellidos,
            telefono   = p_telefono,
            correo     = p_correo
        WHERE numero_identificacion = p_id;

        IF SQL%ROWCOUNT = 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Cliente no encontrado.';
            RETURN;
        END IF;

        UPDATE CLIENTES SET
            direccion        = p_direccion,
            fecha_nacimiento = p_fecha_nac,
            huella_dactilar  = NVL(p_huella, huella_dactilar)
        WHERE numero_identificacion = p_id;

        COMMIT;
        p_codigo_out  := 1;
        p_mensaje_out := 'Cliente actualizado exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_ACTUALIZAR_CLIENTE;

    PROCEDURE SP_REGISTRAR_INSTRUCTOR(
        p_id              IN VARCHAR2,
        p_tipo_id         IN VARCHAR2,
        p_nombre          IN VARCHAR2,
        p_apellidos       IN VARCHAR2,
        p_telefono        IN VARCHAR2,
        p_correo          IN VARCHAR2,
        p_contrasena      IN VARCHAR2,
        p_id_especialidad IN NUMBER,
        p_disponibilidad  IN VARCHAR2,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        BEGIN
            SELECT 1 INTO p_codigo_out FROM USUARIOS
            WHERE numero_identificacion = p_id AND ROWNUM = 1;
            p_codigo_out  := 0;
            p_mensaje_out := 'Ya existe un usuario con esa identificacion.';
            RETURN;
        EXCEPTION WHEN NO_DATA_FOUND THEN NULL;
        END;

        INSERT INTO USUARIOS (
            numero_identificacion, tipo_identificacion, nombre, apellidos,
            telefono, correo, contrasena_hash, estado, fecha_registro, tipo_usuario
        ) VALUES (
            p_id, p_tipo_id, p_nombre, p_apellidos,
            p_telefono, p_correo, p_contrasena, 'ACTIVO', SYSDATE, 'INSTRUCTOR'
        );

        INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
        VALUES (p_id, p_id_especialidad, p_disponibilidad, SYSDATE);

        COMMIT;
        p_codigo_out  := 1;
        p_mensaje_out := 'Instructor registrado exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_REGISTRAR_INSTRUCTOR;

    PROCEDURE SP_ACTUALIZAR_INSTRUCTOR(
        p_id              IN VARCHAR2,
        p_nombre          IN VARCHAR2,
        p_apellidos       IN VARCHAR2,
        p_telefono        IN VARCHAR2,
        p_correo          IN VARCHAR2,
        p_id_especialidad IN NUMBER,
        p_disponibilidad  IN VARCHAR2,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        UPDATE USUARIOS SET
            nombre    = p_nombre,
            apellidos = p_apellidos,
            telefono  = p_telefono,
            correo    = p_correo
        WHERE numero_identificacion = p_id
          AND tipo_usuario = 'INSTRUCTOR';

        IF SQL%ROWCOUNT = 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Instructor no encontrado.';
            RETURN;
        END IF;

        UPDATE INSTRUCTORES SET
            id_especialidad = p_id_especialidad,
            disponibilidad  = p_disponibilidad
        WHERE numero_identificacion = p_id;

        COMMIT;
        p_codigo_out  := 1;
        p_mensaje_out := 'Instructor actualizado exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_ACTUALIZAR_INSTRUCTOR;

    PROCEDURE SP_RENOVAR_MEMBRESIA(
        p_id_cliente      IN VARCHAR2,
        p_id_plan         IN NUMBER,
        p_modalidad       IN VARCHAR2,
        p_metodo_pago     IN VARCHAR2,
        p_valor           IN NUMBER,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        v_id_membresia    NUMBER;
        v_fecha_venc      DATE;
        v_precio_plan     NUMBER(10,2);
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        BEGIN
            SELECT 1 INTO p_codigo_out FROM CLIENTES
            WHERE numero_identificacion = p_id_cliente AND ROWNUM = 1;
        EXCEPTION WHEN NO_DATA_FOUND THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Cliente no encontrado.';
            RETURN;
        END;

        BEGIN
            SELECT precio_mensual INTO v_precio_plan
            FROM PLANES_MEMBRESIAS WHERE id_plan = p_id_plan;
        EXCEPTION WHEN NO_DATA_FOUND THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Plan no encontrado.';
            RETURN;
        END;

        v_fecha_venc := CASE p_modalidad
            WHEN 'MENSUAL'   THEN ADD_MONTHS(TRUNC(SYSDATE), 1)
            WHEN 'SEMESTRAL' THEN ADD_MONTHS(TRUNC(SYSDATE), 6)
            WHEN 'ANUAL'     THEN ADD_MONTHS(TRUNC(SYSDATE), 12)
            ELSE ADD_MONTHS(TRUNC(SYSDATE), 1)
        END;

        INSERT INTO MEMBRESIAS (id_plan, tipo_membresia, modalidad_pago, valor, fecha_inicio, fecha_vencimiento, estado)
        VALUES (p_id_plan, p_modalidad, p_modalidad, p_valor, TRUNC(SYSDATE), v_fecha_venc, 'ACTIVA')
        RETURNING id_membresia INTO v_id_membresia;

        UPDATE HISTORIAL_MEMBRESIAS
        SET activa = 0, fecha_fin = SYSDATE
        WHERE id_cliente = p_id_cliente AND activa = 1;

        INSERT INTO HISTORIAL_MEMBRESIAS (id_cliente, id_membresia, fecha_asignacion, activa)
        VALUES (p_id_cliente, v_id_membresia, SYSDATE, 1);

        INSERT INTO PAGOS (id_membresia, id_cliente, fecha_pago, valor, metodo_pago, estado_pago)
        VALUES (v_id_membresia, p_id_cliente, SYSDATE, p_valor, p_metodo_pago, 'EXITOSO');

        COMMIT;
        p_codigo_out  := 1;
        p_mensaje_out := 'Membresia renovada exitosamente. Vence: ' ||
                         TO_CHAR(v_fecha_venc, 'DD/MM/YYYY');

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_RENOVAR_MEMBRESIA;

    PROCEDURE SP_REGISTRAR_PROGRESO(
        p_id_cliente      IN VARCHAR2,
        p_peso            IN NUMBER,
        p_altura          IN NUMBER,
        p_porc_grasa      IN NUMBER DEFAULT NULL,
        p_masa_muscular   IN NUMBER DEFAULT NULL,
        p_objetivo        IN VARCHAR2 DEFAULT NULL,
        p_observaciones   IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
    BEGIN
        BEGIN
            SELECT 1 INTO p_codigo_out FROM CLIENTES
            WHERE numero_identificacion = p_id_cliente AND ROWNUM = 1;
        EXCEPTION WHEN NO_DATA_FOUND THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Cliente no encontrado.';
            RETURN;
        END;

        INSERT INTO PROGRESOS (
            id_cliente, fecha_registro, peso, altura,
            porcentaje_grasa, masa_muscular, objetivo, observaciones
        ) VALUES (
            p_id_cliente, SYSDATE, p_peso, p_altura,
            p_porc_grasa, p_masa_muscular, p_objetivo, p_observaciones
        );

        p_codigo_out  := 1;
        p_mensaje_out := 'Progreso registrado exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_REGISTRAR_PROGRESO;

    PROCEDURE SP_CREAR_EJERCICIO(
        p_nombre          IN VARCHAR2,
        p_descripcion     IN VARCHAR2 DEFAULT NULL,
        p_grupo_muscular  IN VARCHAR2 DEFAULT NULL,
        p_series          IN NUMBER DEFAULT NULL,
        p_repeticiones    IN NUMBER DEFAULT NULL,
        p_duracion        IN NUMBER DEFAULT NULL,
        p_nivel           IN VARCHAR2 DEFAULT NULL,
        p_recurso_url     IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
        FROM EJERCICIOS WHERE LOWER(nombre) = LOWER(p_nombre);

        IF v_count > 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Ya existe un ejercicio con ese nombre.';
            RETURN;
        END IF;

        INSERT INTO EJERCICIOS (
            nombre, descripcion, grupo_muscular, series, repeticiones,
            duracion_minutos, nivel, recurso_url
        ) VALUES (
            p_nombre, p_descripcion, p_grupo_muscular, p_series, p_repeticiones,
            p_duracion, p_nivel, p_recurso_url
        );

        p_codigo_out  := 1;
        p_mensaje_out := 'Ejercicio creado exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_CREAR_EJERCICIO;

    PROCEDURE SP_ACTUALIZAR_EJERCICIO(
        p_id_ejercicio    IN NUMBER,
        p_nombre          IN VARCHAR2,
        p_descripcion     IN VARCHAR2 DEFAULT NULL,
        p_grupo_muscular  IN VARCHAR2 DEFAULT NULL,
        p_series          IN NUMBER DEFAULT NULL,
        p_repeticiones    IN NUMBER DEFAULT NULL,
        p_duracion        IN NUMBER DEFAULT NULL,
        p_nivel           IN VARCHAR2 DEFAULT NULL,
        p_recurso_url     IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
    BEGIN
        UPDATE EJERCICIOS SET
            nombre          = p_nombre,
            descripcion     = p_descripcion,
            grupo_muscular  = p_grupo_muscular,
            series          = p_series,
            repeticiones    = p_repeticiones,
            duracion_minutos = p_duracion,
            nivel           = p_nivel,
            recurso_url     = p_recurso_url
        WHERE id_ejercicio = p_id_ejercicio;

        IF SQL%ROWCOUNT = 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Ejercicio no encontrado.';
        ELSE
            p_codigo_out  := 1;
            p_mensaje_out := 'Ejercicio actualizado exitosamente.';
        END IF;

    EXCEPTION
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_ACTUALIZAR_EJERCICIO;

    PROCEDURE SP_AGENDAR_CITA(
        p_id_instructor   IN VARCHAR2,
        p_id_cliente      IN VARCHAR2,
        p_fecha           IN DATE,
        p_hora            IN TIMESTAMP,
        p_tipo_cita       IN VARCHAR2,
        p_notas           IN VARCHAR2 DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        v_count  NUMBER;
    BEGIN
        IF p_fecha <= TRUNC(SYSDATE) THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'La fecha debe ser posterior a hoy.';
            RETURN;
        END IF;

        SELECT COUNT(*) INTO v_count
        FROM CITAS
        WHERE id_instructor = p_id_instructor
          AND fecha = p_fecha
          AND hora = p_hora
          AND estado NOT IN ('CANCELADA', 'COMPLETADA');

        IF v_count > 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'El instructor ya tiene una cita en ese horario.';
            RETURN;
        END IF;

        SELECT COUNT(*) INTO v_count
        FROM CITAS
        WHERE id_cliente = p_id_cliente
          AND fecha = p_fecha
          AND estado NOT IN ('CANCELADA', 'COMPLETADA');

        IF v_count >= 3 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'El cliente ya tiene 3 citas programadas para este dia.';
            RETURN;
        END IF;

        INSERT INTO CITAS (id_instructor, id_cliente, fecha, hora, tipo_cita, estado, notas)
        VALUES (p_id_instructor, p_id_cliente, p_fecha, p_hora, p_tipo_cita, 'PENDIENTE', p_notas);

        p_codigo_out  := 1;
        p_mensaje_out := 'Cita agendada exitosamente.';

    EXCEPTION
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_AGENDAR_CITA;

    PROCEDURE SP_CANCELAR_CITA(
        p_id_cita         IN NUMBER,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        v_id_cliente VARCHAR2(20);
        v_fecha      DATE;
    BEGIN
        SELECT id_cliente, fecha INTO v_id_cliente, v_fecha
        FROM CITAS WHERE id_cita = p_id_cita;

        UPDATE CITAS SET estado = 'CANCELADA'
        WHERE id_cita = p_id_cita AND estado = 'PENDIENTE';

        IF SQL%ROWCOUNT = 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'No se pudo cancelar. La cita no existe o no esta pendiente.';
            RETURN;
        END IF;

        INSERT INTO NOTIFICACIONES (id_cliente, tipo, asunto, contenido, estado_envio, origen)
        VALUES (
            v_id_cliente, 'EMAIL', 'Cita cancelada - GYMBROT',
            'Tu cita #' || p_id_cita || ' del ' ||
            TO_CHAR(v_fecha, 'DD/MM/YYYY') || ' ha sido cancelada.',
            'PENDIENTE', 'SISTEMA'
        );

        p_codigo_out  := 1;
        p_mensaje_out := 'Cita cancelada y notificacion generada.';

    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Cita no encontrada.';
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_CANCELAR_CITA;

    PROCEDURE SP_CREAR_RUTINA_COMPLETA(
        p_id_instructor   IN VARCHAR2,
        p_id_cliente      IN VARCHAR2,
        p_nombre          IN VARCHAR2,
        p_descripcion     IN VARCHAR2 DEFAULT NULL,
        p_fecha_fin       IN DATE DEFAULT NULL,
        p_dias_semana     IN VARCHAR2 DEFAULT NULL,
        p_objetivo        IN VARCHAR2 DEFAULT NULL,
        p_ejercicios      IN NUMBER_ARRAY DEFAULT NULL,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
        v_id_rutina NUMBER;
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo)
        VALUES (p_id_instructor, p_id_cliente, p_nombre, p_descripcion, SYSDATE, p_fecha_fin, p_dias_semana, p_objetivo)
        RETURNING id_rutina INTO v_id_rutina;

        IF p_ejercicios IS NOT NULL THEN
            FOR i IN 1 .. p_ejercicios.COUNT LOOP
                INSERT INTO RUTINA_EJERCICIOS (id_rutina, id_ejercicio, orden)
                VALUES (v_id_rutina, p_ejercicios(i), i);
            END LOOP;
        END IF;

        COMMIT;
        p_codigo_out  := 1;
        p_mensaje_out := 'Rutina creada exitosamente con ID ' || v_id_rutina;

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_CREAR_RUTINA_COMPLETA;

    PROCEDURE SP_REGISTRAR_INGRESO(
        p_id_cliente          IN VARCHAR2,
        p_metodo_verificacion IN VARCHAR2 DEFAULT 'HUELLA',
        p_codigo_out          OUT NUMBER,
        p_mensaje_out         OUT VARCHAR2
    ) IS
        v_activa     NUMBER;
        v_fecha_venc DATE;
    BEGIN
        BEGIN
            SELECT hm.activa, m.fecha_vencimiento
            INTO v_activa, v_fecha_venc
            FROM HISTORIAL_MEMBRESIAS hm
            JOIN MEMBRESIAS m ON hm.id_membresia = m.id_membresia
            WHERE hm.id_cliente = p_id_cliente
              AND hm.activa = 1
              AND ROWNUM = 1;
        EXCEPTION WHEN NO_DATA_FOUND THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Acceso denegado: sin membresia activa.';
            RETURN;
        END;

        IF v_fecha_venc < TRUNC(SYSDATE) THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'Acceso denegado: membresia vencida desde ' ||
                             TO_CHAR(v_fecha_venc, 'DD/MM/YYYY');
            RETURN;
        END IF;

        UPDATE REGISTROS_INGRESOS
        SET hora_salida = SYSTIMESTAMP
        WHERE id_cliente = p_id_cliente
          AND fecha = TRUNC(SYSDATE)
          AND hora_salida IS NULL;

        INSERT INTO REGISTROS_INGRESOS (id_cliente, fecha, hora_entrada, metodo_verificacion, estado_verificacion)
        VALUES (p_id_cliente, TRUNC(SYSDATE), SYSTIMESTAMP, p_metodo_verificacion, 'APROBADO');

        p_codigo_out  := 1;
        p_mensaje_out := 'Ingreso registrado correctamente.';

    EXCEPTION
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_REGISTRAR_INGRESO;

    PROCEDURE SP_REGISTRAR_SALIDA(
        p_id_cliente      IN VARCHAR2,
        p_codigo_out      OUT NUMBER,
        p_mensaje_out     OUT VARCHAR2
    ) IS
    BEGIN
        UPDATE REGISTROS_INGRESOS
        SET hora_salida = SYSTIMESTAMP
        WHERE id_cliente = p_id_cliente
          AND fecha = TRUNC(SYSDATE)
          AND hora_salida IS NULL;

        IF SQL%ROWCOUNT = 0 THEN
            p_codigo_out  := 0;
            p_mensaje_out := 'No hay ingreso abierto para este cliente.';
        ELSE
            p_codigo_out  := 1;
            p_mensaje_out := 'Salida registrada correctamente.';
        END IF;

    EXCEPTION
        WHEN OTHERS THEN
            p_codigo_out  := -1;
            p_mensaje_out := 'Error: ' || SQLERRM;
    END SP_REGISTRAR_SALIDA;

END PKG_GYMBROT_PROC;
/


-- ====================================================================
--  G. TRIGGERS (10)
--  ====================================================================

CREATE OR REPLACE TRIGGER TRG_ACTUALIZAR_ESTADO_MEMBRESIA
AFTER INSERT ON PAGOS
FOR EACH ROW
 WHEN (NEW.estado_pago = 'EXITOSO') BEGIN
    UPDATE MEMBRESIAS
    SET estado = 'ACTIVA'
    WHERE id_membresia = :NEW.id_membresia
      AND estado != 'ACTIVA';
END;
/

CREATE OR REPLACE TRIGGER TRG_CITAS_BI
BEFORE INSERT ON CITAS
FOR EACH ROW
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM CITAS
    WHERE id_instructor = :NEW.id_instructor
      AND fecha = :NEW.fecha
      AND hora = :NEW.hora
      AND estado NOT IN ('CANCELADA', 'COMPLETADA');

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20003,
            'El instructor ya tiene una cita programada en esa fecha y hora.');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_EJERCICIOS_BD
BEFORE DELETE ON EJERCICIOS
FOR EACH ROW
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM RUTINA_EJERCICIOS WHERE id_ejercicio = :OLD.id_ejercicio;

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20001,
            'No se puede eliminar el ejercicio: esta asignado a ' ||
            v_count || ' rutina(s).');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_ESPECIALIDADES_BD
BEFORE DELETE ON ESPECIALIDADES
FOR EACH ROW
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM INSTRUCTORES WHERE id_especialidad = :OLD.id_especialidad;

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20002,
            'No se puede eliminar la especialidad: tiene ' ||
            v_count || ' instructor(es) asignado(s).');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_NOTIFICAR_CANCELACION_CITA
AFTER UPDATE ON CITAS
FOR EACH ROW
 WHEN (NEW.estado = 'CANCELADA' AND OLD.estado = 'PENDIENTE') BEGIN
    INSERT INTO NOTIFICACIONES (id_cliente, tipo, asunto, contenido, estado_envio, origen)
    VALUES (
        :NEW.id_cliente,
        'EMAIL',
        'Cita cancelada - GYMBROT',
        'Tu cita #' || :NEW.id_cita || ' del ' ||
        TO_CHAR(:NEW.fecha, 'DD/MM/YYYY') || ' ha sido cancelada.',
        'PENDIENTE',
        'SISTEMA'
    );
END;
/

CREATE OR REPLACE TRIGGER TRG_PLANES_MEMBRESIAS_BD
BEFORE DELETE ON PLANES_MEMBRESIAS
FOR EACH ROW
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM MEMBRESIAS
    WHERE id_plan = :OLD.id_plan AND estado = 'ACTIVA';

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20004,
            'No se puede eliminar el plan: hay ' ||
            v_count || ' membresia(s) activa(s) asociada(s).');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_PROGRESOS_BI
BEFORE INSERT ON PROGRESOS
FOR EACH ROW
BEGIN
    IF :NEW.imc IS NULL AND :NEW.peso IS NOT NULL
       AND :NEW.altura IS NOT NULL AND :NEW.altura > 0 THEN
        :NEW.imc := ROUND(PKG_GYMBROT_FUNC.FN_CALCULAR_IMC(:NEW.peso, :NEW.altura), 2);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_REGISTRAR_SALIDA_AUTOMATICA
BEFORE INSERT ON REGISTROS_INGRESOS
FOR EACH ROW
DECLARE
    v_id_ingreso_abierto NUMBER;
BEGIN
    BEGIN
        SELECT id_ingreso INTO v_id_ingreso_abierto
        FROM REGISTROS_INGRESOS
        WHERE id_cliente = :NEW.id_cliente
          AND fecha = TRUNC(SYSDATE)
          AND hora_salida IS NULL
          AND ROWNUM = 1;

        UPDATE REGISTROS_INGRESOS
        SET hora_salida = SYSTIMESTAMP
        WHERE id_ingreso = v_id_ingreso_abierto;

    EXCEPTION
        WHEN NO_DATA_FOUND THEN NULL;
    END;
END;
/

CREATE OR REPLACE TRIGGER TRG_USUARIOS_BU
BEFORE UPDATE OF estado ON USUARIOS
FOR EACH ROW
 WHEN (NEW.estado IN ('INACTIVO', 'SUSPENDIDO', 'BLOQUEADO')
      AND OLD.estado NOT IN ('INACTIVO', 'SUSPENDIDO', 'BLOQUEADO')) BEGIN
    UPDATE HISTORIAL_MEMBRESIAS
    SET activa = 0, fecha_fin = SYSDATE
    WHERE id_cliente = :OLD.numero_identificacion AND activa = 1;
END;
/

CREATE OR REPLACE TRIGGER TRG_VERIFICAR_VENCIMIENTO_MEMBRESIA
BEFORE INSERT OR UPDATE ON MEMBRESIAS
FOR EACH ROW
BEGIN
    IF :NEW.fecha_vencimiento < TRUNC(SYSDATE) THEN
        :NEW.estado := 'VENCIDA';
    END IF;
END;
/


-- ====================================================================
--  H. DATOS DE CATALOGO (idempotente - se puede re-ejecutar)
--  ====================================================================

-- ── ADMINISTRADOR ADMIN001 ────────────────────────────────────────
INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre,
    apellidos, telefono, correo, contrasena_hash, estado, fecha_registro, tipo_usuario)
SELECT 'ADMIN001', 'CC', 'admin', 'GYMBROT', '3000000000', 'admin@gymbrot.com',
    '$2a$10$59J2klPQClChLN/d9TT1Vu98x8UJ.oQck32bfCCdF5RisBlB.HOKm',
    'ACTIVO', SYSDATE, 'ADMINISTRADOR'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USUARIOS WHERE numero_identificacion = 'ADMIN001');

INSERT INTO ADMINISTRADORES (numero_identificacion, rol)
SELECT 'ADMIN001', 'SUPERADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMINISTRADORES WHERE numero_identificacion = 'ADMIN001');

-- ── ESPECIALIDADES (5) ─────────────────────────────────────────────
INSERT INTO ESPECIALIDADES (nombre)
SELECT 'Musculacion' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ESPECIALIDADES WHERE nombre = 'Musculacion');
INSERT INTO ESPECIALIDADES (nombre)
SELECT 'Cardio y Resistencia' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ESPECIALIDADES WHERE nombre = 'Cardio y Resistencia');
INSERT INTO ESPECIALIDADES (nombre)
SELECT 'Tren Superior' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ESPECIALIDADES WHERE nombre = 'Tren Superior');
INSERT INTO ESPECIALIDADES (nombre)
SELECT 'Tren Inferior' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ESPECIALIDADES WHERE nombre = 'Tren Inferior');
INSERT INTO ESPECIALIDADES (nombre)
SELECT 'Funcional' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ESPECIALIDADES WHERE nombre = 'Funcional');

-- ── PLANES (Plan Silver / Gold / Black) ────────────────────────────
INSERT INTO PLANES_MEMBRESIAS (nombre, descripcion, precio_mensual, precio_semestral, precio_anual, beneficios)
SELECT 'Plan Silver', 'Acceso basico a sala de pesas y cardio', 80000, 440000, 840000,
       'Acceso sala pesas - Acceso cardio - Casillero basico'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PLANES_MEMBRESIAS WHERE nombre = 'Plan Silver');

INSERT INTO PLANES_MEMBRESIAS (nombre, descripcion, precio_mensual, precio_semestral, precio_anual, beneficios)
SELECT 'Plan Gold', 'Acceso completo mas clases grupales', 130000, 710000, 1360000,
       'Acceso total - Clases grupales - Casillero - Toalla'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PLANES_MEMBRESIAS WHERE nombre = 'Plan Gold');

INSERT INTO PLANES_MEMBRESIAS (nombre, descripcion, precio_mensual, precio_semestral, precio_anual, beneficios)
SELECT 'Plan Black', 'Acceso premium con instructor personal y nutricionista', 200000, 1100000, 2100000,
       'Acceso total - Clases grupales - Instructor personal - Nutricionista - Casillero'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PLANES_MEMBRESIAS WHERE nombre = 'Plan Black');

COMMIT;


-- ====================================================================
--  I. VERIFICACION
--  ====================================================================
SELECT 'TABLAS=' || COUNT(*)              AS verif FROM USER_TABLES;
SELECT 'INDICES=' || COUNT(*)             AS verif FROM USER_INDEXES WHERE INDEX_NAME LIKE 'IDX\_%' ESCAPE '\';
SELECT 'FUNCIONALIDAD'                    AS verif, OBJECT_NAME, STATUS FROM USER_OBJECTS
  WHERE OBJECT_NAME IN ('PKG_GYMBROT_FUNC','PKG_GYMBROT_PROC');
SELECT 'TRIGGERS=' || COUNT(*)            AS verif FROM USER_TRIGGERS;
SELECT 'ADMIN=' || NUMERO_IDENTIFICACION AS verif FROM USUARIOS WHERE numero_identificacion = 'ADMIN001';

-- ====================================================================
--  FIN DEL SCRIPT
-- ====================================================================
