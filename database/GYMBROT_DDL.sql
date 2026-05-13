-- ============================================================
--  GYMBROT — Script DDL v5.0
--  Base de datos
-- ============================================================


-- ============================================================
--  1. TABLAS INDEPENDIENTES / CATÁLOGO
-- ============================================================

CREATE TABLE ESPECIALIDADES (
    id_especialidad   NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR2(100)   NOT NULL
);

CREATE TABLE PLANES_MEMBRESIAS (
    id_plan           NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR2(100)   NOT NULL,
    descripcion       VARCHAR2(500),
    precio_mensual    NUMBER(10,2),
    precio_semestral  NUMBER(10,2),
    precio_anual      NUMBER(10,2),
    beneficios        CLOB
);

CREATE TABLE PLANTILLAS_MENSAJE (
    id_plantilla          NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                VARCHAR2(100)   NOT NULL,
    tipo                  VARCHAR2(20)    NOT NULL
        CONSTRAINT ck_plantilla_tipo CHECK (tipo IN ('SMS','EMAIL','WHATSAPP')),
    asunto                VARCHAR2(200),
    cuerpo_html           CLOB,
    cuerpo_texto          CLOB,
    variables_disponibles VARCHAR2(500),
    activa                NUMBER(1)       DEFAULT 1 NOT NULL
        CONSTRAINT ck_plantilla_activa CHECK (activa IN (0,1))
);

CREATE TABLE EJERCICIOS (
    id_ejercicio      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR2(150)   NOT NULL,
    descripcion       CLOB,
    grupo_muscular    VARCHAR2(100),
    series            NUMBER(3),
    repeticiones      NUMBER(3),
    duracion_minutos  NUMBER(5),
    nivel             VARCHAR2(20)
        CONSTRAINT ck_ejercicio_nivel CHECK (nivel IN ('PRINCIPIANTE','INTERMEDIO','AVANZADO')),
    recurso_url       VARCHAR2(500)
);


-- ============================================================
--  2. USUARIOS (supertipo)
-- ============================================================

CREATE TABLE USUARIOS (
    numero_identificacion  VARCHAR2(20)    PRIMARY KEY,
    tipo_identificacion    VARCHAR2(10)    NOT NULL
        CONSTRAINT ck_usu_tipo_id CHECK (tipo_identificacion IN ('CC','CE','PP','TI')),
    nombre                 VARCHAR2(200)   NOT NULL,
    apellidos              VARCHAR2(200)   NOT NULL,
    telefono               VARCHAR2(20),
    correo                 VARCHAR2(150),
    contrasena_hash        VARCHAR2(255),
    foto_url               VARCHAR2(500),
    estado                 VARCHAR2(20)    DEFAULT 'ACTIVO'
        CONSTRAINT ck_usu_estado CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO','BLOQUEADO')),
    fecha_registro         DATE            DEFAULT SYSDATE,
    tipo_usuario           VARCHAR2(20)    NOT NULL
        CONSTRAINT ck_usu_tipo CHECK (tipo_usuario IN ('CLIENTE','INSTRUCTOR','ADMINISTRADOR'))
);


-- ============================================================
--  3. MEMBRESIAS
-- ============================================================

CREATE TABLE MEMBRESIAS (
    id_membresia      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_plan           NUMBER          NOT NULL,
    tipo_membresia    VARCHAR2(50),
    modalidad_pago    VARCHAR2(20)
        CONSTRAINT ck_memb_modalidad CHECK (modalidad_pago IN ('MENSUAL','SEMESTRAL','ANUAL')),
    valor             NUMBER(10,2),
    fecha_inicio      DATE,
    fecha_vencimiento DATE,
    estado            VARCHAR2(20)    DEFAULT 'ACTIVA'
        CONSTRAINT ck_memb_estado CHECK (estado IN ('ACTIVA','VENCIDA','SUSPENDIDA','CANCELADA')),
    CONSTRAINT fk_memb_plan FOREIGN KEY (id_plan)
        REFERENCES PLANES_MEMBRESIAS (id_plan)
);


-- ============================================================
--  4. CLIENTES (subtipo de USUARIOS)
-- ============================================================

CREATE TABLE CLIENTES (
    numero_identificacion  VARCHAR2(20)    PRIMARY KEY,
    direccion              VARCHAR2(300),
    fecha_nacimiento       DATE,
    huella_dactilar        BLOB,
    CONSTRAINT fk_cli_usuario FOREIGN KEY (numero_identificacion)
        REFERENCES USUARIOS (numero_identificacion)
);


-- ============================================================
--  5. INSTRUCTORES (subtipo de USUARIOS)
-- ============================================================

CREATE TABLE INSTRUCTORES (
    numero_identificacion  VARCHAR2(20)    PRIMARY KEY,
    id_especialidad        NUMBER          NOT NULL,
    disponibilidad         VARCHAR2(200),
    fecha_contratacion     DATE,
    CONSTRAINT fk_inst_usuario FOREIGN KEY (numero_identificacion)
        REFERENCES USUARIOS (numero_identificacion),
    CONSTRAINT fk_inst_especialidad FOREIGN KEY (id_especialidad)
        REFERENCES ESPECIALIDADES (id_especialidad)
);


-- ============================================================
--  6. ADMINISTRADORES (subtipo de USUARIOS)
-- ============================================================

CREATE TABLE ADMINISTRADORES (
    numero_identificacion  VARCHAR2(20)    PRIMARY KEY,
    rol                    VARCHAR2(30)    DEFAULT 'ADMIN'
        CONSTRAINT ck_adm_rol CHECK (rol IN ('SUPERADMIN','ADMIN','RECEPCION')),
    CONSTRAINT fk_adm_usuario FOREIGN KEY (numero_identificacion)
        REFERENCES USUARIOS (numero_identificacion)
);


-- ============================================================
--  7. HISTORIAL_MEMBRESIAS
-- ============================================================

CREATE TABLE HISTORIAL_MEMBRESIAS (
    id_historial      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente        VARCHAR2(20)    NOT NULL,
    id_membresia      NUMBER          NOT NULL,
    fecha_asignacion  DATE            DEFAULT SYSDATE NOT NULL,
    fecha_fin         DATE,
    activa            NUMBER(1)       DEFAULT 1 NOT NULL
        CONSTRAINT ck_hist_activa CHECK (activa IN (0,1)),
    CONSTRAINT fk_hist_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion),
    CONSTRAINT fk_hist_membresia FOREIGN KEY (id_membresia)
        REFERENCES MEMBRESIAS (id_membresia)
);


-- ============================================================
--  8. PAGOS
-- ============================================================

CREATE TABLE PAGOS (
    id_pago                 NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_membresia            NUMBER          NOT NULL,
    id_cliente              VARCHAR2(20)    NOT NULL,
    fecha_pago              DATE            DEFAULT SYSDATE NOT NULL,
    valor                   NUMBER(10,2)    NOT NULL,
    metodo_pago             VARCHAR2(30)    NOT NULL
        CONSTRAINT ck_pago_metodo CHECK (metodo_pago IN
            ('EFECTIVO','TRANSFERENCIA','TARJETA','NEQUI','DAVIPLATA')),
    estado_pago             VARCHAR2(20)    DEFAULT 'EXITOSO'
        CONSTRAINT ck_pago_estado CHECK (estado_pago IN ('EXITOSO','RECHAZADO','PENDIENTE')),
    referencia_transaccion  VARCHAR2(100),
    observaciones           VARCHAR2(500),
    CONSTRAINT fk_pago_membresia FOREIGN KEY (id_membresia)
        REFERENCES MEMBRESIAS (id_membresia),
    CONSTRAINT fk_pago_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion)
);


-- ============================================================
--  9. PROGRESOS
-- ============================================================

CREATE TABLE PROGRESOS (
    id_progreso        NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente         VARCHAR2(20)    NOT NULL,
    fecha_registro     DATE            DEFAULT SYSDATE NOT NULL,
    peso               NUMBER(6,2),
    altura             NUMBER(5,2),
    imc                NUMBER(5,2),
    porcentaje_grasa   NUMBER(5,2),
    masa_muscular      NUMBER(6,2),
    objetivo           VARCHAR2(200),
    observaciones      CLOB,
    CONSTRAINT fk_prog_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion)
);


-- ============================================================
--  10. RUTINAS
-- ============================================================

CREATE TABLE RUTINAS (
    id_rutina         NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_instructor     VARCHAR2(20)    NOT NULL,
    id_cliente        VARCHAR2(20)    NOT NULL,
    nombre            VARCHAR2(150)   NOT NULL,
    descripcion       CLOB,
    fecha_creacion    DATE            DEFAULT SYSDATE,
    fecha_fin         DATE,
    dias_semana       VARCHAR2(100),
    objetivo          VARCHAR2(200),
    CONSTRAINT fk_rut_instructor FOREIGN KEY (id_instructor)
        REFERENCES INSTRUCTORES (numero_identificacion),
    CONSTRAINT fk_rut_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion)
);


-- ============================================================
--  11. RUTINA_EJERCICIOS
-- ============================================================

CREATE TABLE RUTINA_EJERCICIOS (
    id_rutina         NUMBER          NOT NULL,
    id_ejercicio      NUMBER          NOT NULL,
    orden             NUMBER(3)       NOT NULL,
    dia_semana        VARCHAR2(15)
        CONSTRAINT ck_ruteje_dia CHECK (dia_semana IN
            ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')),
    notas_instructor  CLOB,
    CONSTRAINT pk_rutina_ejercicios PRIMARY KEY (id_rutina, id_ejercicio),
    CONSTRAINT fk_ruteje_rutina FOREIGN KEY (id_rutina)
        REFERENCES RUTINAS (id_rutina),
    CONSTRAINT fk_ruteje_ejercicio FOREIGN KEY (id_ejercicio)
        REFERENCES EJERCICIOS (id_ejercicio)
);


-- ============================================================
--  12. REGISTROS_INGRESOS
-- ============================================================

CREATE TABLE REGISTROS_INGRESOS (
    id_ingreso            NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente            VARCHAR2(20)    NOT NULL,
    fecha                 DATE            DEFAULT SYSDATE NOT NULL,
    hora_entrada          TIMESTAMP,
    hora_salida           TIMESTAMP,
    metodo_verificacion   VARCHAR2(20)    DEFAULT 'HUELLA'
        CONSTRAINT ck_regi_metodo CHECK (metodo_verificacion IN ('HUELLA','QR','MANUAL')),
    estado_verificacion   VARCHAR2(20)    DEFAULT 'APROBADO'
        CONSTRAINT ck_regi_estado CHECK (estado_verificacion IN ('APROBADO','RECHAZADO','ERROR_LECTOR')),
    CONSTRAINT fk_regi_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion)
);


-- ============================================================
--  13. SESIONES_GYMBROT
-- ============================================================

CREATE TABLE SESIONES_GYMBROT (
    id_sesion       NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente      VARCHAR2(20)    NOT NULL,
    fecha           DATE            DEFAULT SYSDATE,
    hora_inicio     TIMESTAMP,
    hora_fin        TIMESTAMP,
    contexto_activo CLOB,
    CONSTRAINT fk_ses_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion)
);


-- ============================================================
--  14. CITAS
-- ============================================================

CREATE TABLE CITAS (
    id_cita       NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_instructor VARCHAR2(20)    NOT NULL,
    id_cliente    VARCHAR2(20)    NOT NULL,
    fecha         DATE            NOT NULL,
    hora          TIMESTAMP,
    tipo_cita     VARCHAR2(30)
        CONSTRAINT ck_cita_tipo CHECK (tipo_cita IN ('EVALUACION','SEGUIMIENTO','NUTRICION','CONSULTA')),
    estado        VARCHAR2(20)    DEFAULT 'PENDIENTE'
        CONSTRAINT ck_cita_estado CHECK (estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','COMPLETADA')),
    notas         CLOB,
    CONSTRAINT fk_cita_instructor FOREIGN KEY (id_instructor)
        REFERENCES INSTRUCTORES (numero_identificacion),
    CONSTRAINT fk_cita_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion)
);


-- ============================================================
--  15. MENSAJES_GYMBROT
-- ============================================================

CREATE TABLE MENSAJES_GYMBROT (
    id_mensaje      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_sesion       NUMBER          NOT NULL,
    id_cita         NUMBER,
    remitente       VARCHAR2(10)    NOT NULL
        CONSTRAINT ck_msg_remitente CHECK (remitente IN ('CLIENTE','BOT')),
    contenido       CLOB            NOT NULL,
    timestamp_msg   TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    tipo_intencion  VARCHAR2(30)
        CONSTRAINT ck_msg_intencion CHECK (tipo_intencion IN
            ('CONSULTA','AGENDA_CITA','VER_PROGRESO','SALUDO','NOTIFICACION','OTRO')),
    CONSTRAINT fk_msg_sesion FOREIGN KEY (id_sesion)
        REFERENCES SESIONES_GYMBROT (id_sesion),
    CONSTRAINT fk_msg_cita FOREIGN KEY (id_cita)
        REFERENCES CITAS (id_cita)
);


-- ============================================================
--  16. NOTIFICACIONES
-- ============================================================

CREATE TABLE NOTIFICACIONES (
    id_notificacion NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente      VARCHAR2(20)    NOT NULL,
    id_plantilla    NUMBER,
    tipo            VARCHAR2(20)    NOT NULL
        CONSTRAINT ck_notif_tipo CHECK (tipo IN ('SMS','EMAIL','WHATSAPP')),
    asunto          VARCHAR2(200),
    contenido       CLOB            NOT NULL,
    estado_envio    VARCHAR2(20)    DEFAULT 'PENDIENTE'
        CONSTRAINT ck_notif_estado CHECK (estado_envio IN ('PENDIENTE','ENVIADO','FALLIDO','CANCELADO')),
    fecha_envio     TIMESTAMP       DEFAULT SYSTIMESTAMP,
    origen          VARCHAR2(20)    DEFAULT 'SISTEMA'
        CONSTRAINT ck_notif_origen CHECK (origen IN ('BOT','SISTEMA','MANUAL')),
    CONSTRAINT fk_notif_cliente FOREIGN KEY (id_cliente)
        REFERENCES CLIENTES (numero_identificacion),
    CONSTRAINT fk_notif_plantilla FOREIGN KEY (id_plantilla)
        REFERENCES PLANTILLAS_MENSAJE (id_plantilla)
);


-- ============================================================
--  ÍNDICES
-- ============================================================

CREATE INDEX idx_usu_tipo             ON USUARIOS             (tipo_usuario, estado);
CREATE INDEX idx_usu_correo           ON USUARIOS             (correo);
CREATE INDEX idx_hist_cliente_activa  ON HISTORIAL_MEMBRESIAS (id_cliente, activa);
CREATE INDEX idx_pagos_cliente        ON PAGOS                (id_cliente, fecha_pago);
CREATE INDEX idx_pagos_membresia      ON PAGOS                (id_membresia);
CREATE INDEX idx_memb_vencimiento     ON MEMBRESIAS           (fecha_vencimiento, estado);
CREATE INDEX idx_progresos_cliente    ON PROGRESOS            (id_cliente);
CREATE INDEX idx_rutinas_cliente      ON RUTINAS              (id_cliente);
CREATE INDEX idx_rutinas_instructor   ON RUTINAS              (id_instructor);
CREATE INDEX idx_regi_cliente_fecha   ON REGISTROS_INGRESOS   (id_cliente, fecha);
CREATE INDEX idx_sesiones_cliente     ON SESIONES_GYMBROT     (id_cliente);
CREATE INDEX idx_citas_cliente_fecha  ON CITAS                (id_cliente, fecha);
CREATE INDEX idx_citas_instructor     ON CITAS                (id_instructor);
CREATE INDEX idx_mensajes_sesion      ON MENSAJES_GYMBROT     (id_sesion);
CREATE INDEX idx_notif_cliente_fecha  ON NOTIFICACIONES       (id_cliente, fecha_envio);
CREATE INDEX idx_notif_estado         ON NOTIFICACIONES       (estado_envio);


-- ============================================================
--  FIN DEL SCRIPT — GYMBROT DDL v5.0
-- ============================================================


