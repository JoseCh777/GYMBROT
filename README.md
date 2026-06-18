# GYMBROT — Sistema de Gestión Integral de Gimnasios

**GYMBROT** es una aplicación de escritorio JavaFX para la administración completa de gimnasios. 
* Integra control de acceso con datos biométrico.
* Chatbot con IA.
* Sistema de notificaciones automáticas (SMS/email).
* Gestion de clientes, instructores, membresías, pagos y citas.
* Panel de progreso fisico de los clientes.
* Panel financiero con gráficos en tiempo real.
---

## Equipo de Desarrollo

| Nombre | Rol | Responsabilidad |
|---|---|---|
| Jose Antonio Chinchia Gutierrez | Líder / Scrum Master | Integración biométrica, interfaz gráfica (frontend), coordinación del equipo y revisión de Pull Requests |
| Samuel David Caro Borrero | Desarrollador 1 | Lógica del backend: capa de servicios, DAOs, reglas de negocio |
| Carlos Daniel Ospino Vivic | Desarrollador 2 | Integración del chatbot GymBrot, sistema de notificaciones (SMS/email) y apoyo al backend |
| Alberto Mario Mendoza Charris | Desarrollador 3 | Apoyo al frontend, pruebas de interfaz y asignación de tareas adicionales según avance del proyecto |

---

## Requerimientos del Sistema

### Software obligatorio

| Componente | Versión mínima | Instalación |
|---|---|---|
| **Java Development Kit (JDK)** | 21 | [Adoptium Temurin](https://adoptium.net/) |
| **Apache Maven** | 3.9+ | `winget install Apache.Maven` o manual |
| **Oracle Database** | 21c XE / 23c | [Oracle XE](https://www.oracle.com/database/technologies/appdev/xe.html) |
| **ojdbc11.jar** | 23.6 | Incluido en `libs/ojdbc11.jar` |

### Dependencias Maven (resueltas automáticamente)

| Dependencia | Versión | Propósito |
|---|---|---|
| `org.openjfx:javafx-controls` | 21 | JavaFX base |
| `org.openjfx:javafx-fxml` | 21 | Carga de vistas FXML |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.1 | Serialización JSON (API Groq) |
| `com.sun.mail:javax.mail` | 1.6.2 | Envío de correos electrónicos |
| `com.twilio.sdk:twilio` | 9.14.0 | Envío de SMS |
| `org.mindrot:jbcrypt` | 0.4 | Hashing y verificación de contraseñas |

### SDKs externos (`.jar` en `/libs/`)

| Archivo | Versión | Propósito |
|---|---|---|
| `ojdbc11.jar` | 23.6 | Driver JDBC para Oracle |
| `dpotapi.jar` | 1.6.1 | DigitalPersona — API de captura de huella |
| `dpotjni.jar` | 1.6.1 | DigitalPersona — Binding nativo JNI |
| `dpfpenrollment.jar` | 1.6.1 | DigitalPersona — Registro de huellas |
| `dpfpverification.jar` | 1.6.1 | DigitalPersona — Verificación 1:1 |

### Hardware (opcional)

- **Lector biométrico**: DigitalPersona U.are.U (para autenticación por huella)

### Variables de entorno (obligatorias)

```
GYMBROT_DB_URL=jdbc:oracle:thin:@localhost:1521:XE
GYMBROT_DB_USER=gymbrot_admin
GYMBROT_DB_PASS=tu_password
```

### Archivo `config.properties` (raíz del proyecto)

```
GROQ_API_KEY=gsk_...                         → Chatbot Gymbro AI
TWILIO_ACCOUNT_SID=AC...                     → Notificaciones SMS
TWILIO_AUTH_TOKEN=...
TWILIO_PHONE_NUMBER=+1581...
GMAIL=gymbrot.notificaciones@gmail.com        → Notificaciones email
GMAIL_PASSWORD=...
```

---

## Arquitectura del Proyecto

### Vista general (MVC)

```
FXML (Vista) ──fx:controller──> CONTROLLER ──> SERVICE ──> DAO ──> Oracle DB
      │                             │              │            │
      │                        SesionManager   HuellaUtil    DatabaseConnection
      │                        AlertaPersonalizada           (JDBC Singleton)
      │                        ValidacionUtil
      ▼
  gymbrot.css (estilos globales)
```

### Flujo de comunicación entre capas

```
┌──────────┐    llama a      ┌──────────┐    llama a     ┌──────────┐
│FXML/View │ <──fx:id─────── │Controller│ ──────────────>│ Service  │
│(layouts) │   onAction      │(eventos) │                │(negocio) │
└──────────┘                 └──────────┘                └────┬─────┘
                                                              │
                                                    llama a   │
                                                              ▼
                                                     ┌──────────────┐
                                                     │     DAO      │
                                                     │  (JDBC CRUD) │
                                                     └──────┬───────┘
                                                            │
                                                      DatabaseConnection
                                                        (Singleton)
                                                            │
                                                     Oracle Database
```

### Estructura de directorios

```
GYMBROT/
├── config.properties           ← API keys (Groq, Twilio, Gmail)
├── pom.xml                     ← Maven build + dependencias
├── README.md
├── docs/                       ← Diagramas de arquitectura (PlantUML + Mermaid)
├── libs/                       ← JARs externos (ojdbc11, DigitalPersona SDK)
├── database/                   ← Esquema Oracle + seeds
│   ├── BD                      ← Documentación de la capa
│   ├── GYMBROT_DDL.sql
│   ├── GYMBROT_SEED.sql
│   └── GYMBROT_RUTINAS_SEED.sql
├── capture/                    ← CapturadorHuella.cs + .exe (C#), captura de huellas DigitalPersona
│   ├── android-chrome-192x192.png
│   └── logo.png
├── .ai/                        ← Archivos de inteligencia artificial
│   └── prompts/                ← Prompts del sistema para Gymbro AI
└── src/
    ├── main/java/org/gymbrot/
    │   ├── Main.java           ← Entry point (Stage, Scene, navegación)
    │   ├── TitleBarController.java ← Barra de título personalizada
    │   ├── controller/         ← 24 controladores JavaFX
    │   ├── dao/                ← 20 Data Access Objects
    │   ├── model/              ← 18 entidades de dominio
    │   ├── service/            ← 20 servicios de negocio
    │   └── util/               ← 8 utilidades transversales
    └── main/resources/
        ├── css/gymbrot.css     ← Estilo global oscuro
        ├── fonts/              ← Lexend, Inter, Space Grotesk (9 archivos .ttf)
        ├── fxml/               ← 23 vistas FXML
        ├── images/             ← logo.png
        └── models/             ← Modelos 3D
```

---

## Base de Datos — Esquema Oracle

### Diagrama de tablas (19 tablas)

```
                              ┌─────────────────────────────────┐
                              │           USUARIOS              │
                              │  (CC/CE/PP/TI — tipo_usuario —  │
                              │   ACTIVO/INACTIVO/SUSPENDIDO)   │
                              └───────┬──────────┬──────────┬───┘
                                      │          │          │
                        ┌─────────────┼──────────┼──────────┼──────────────┐
                        │             │          │          │              │
                        ▼             ▼          ▼          ▼              ▼
                  ┌──────────┐  ┌──────────┐ ┌──────────┐ ┌──────────┐  ┌──────────┐
                  │ CLIENTES │  │INSTRUCT. │ │ADMIN.    │ │(cuentas  │  │(futuro)  │
                  │(huella)  │  │(espec.)  │ │(rol)     │ │  de      │  │          │
                  └────┬─────┘  └────┬─────┘ └──────────┘ │sistema)  │  └──────────┘
                       │             │                    └──────────┘
                       │        ┌────▼──────────┐
                       │        │ESPECIALIDADES │
                       │        └───────────────┘
                       │
     ┌─────────────────┼──────────────┬─────────────────┬────────────────┬───────────────┐
     │                 │              │                 │                │               │
     ▼                 ▼              ▼                 ▼                ▼               ▼
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐    ┌──────────┐    ┌──────────────┐
│MEMBRESIAS│    │PROGRESOS │    │ RUTINAS  │    │   CITAS   │    │REG_INGR. │    │ SESIONES_GYM │
│ ──────── │    │          │    │ ──────── │    │(instructor│    │(huella)  │    │ ──────────── │
│PLANES_M  │    │          │    │RUT_EJERC │    │ + cliente)│    │          │    │ MENSAJES_GYM │
│ ──────── │    │          │    │ ──────── │    └───────────┘    └──────────┘    └──────────────┘
│  PAGOS   │    │          │    │ EJERCICI │
└──────────┘    └──────────┘    └──────────┘
┌──────────┐    ┌──────────────────┐
│HIST_MEM. │    │ NOTIFICACIONES   │
│(cliente+ │    │ ───────────────  │
│membresia)│    │ PLANTILLAS_MSG   │
└──────────┘    └──────────────────┘
```

### Tablas principales

| Tabla | Propósito |
|---|---|
| `USUARIOS` | Supertipo con datos comunes (nombre, correo, contraseña, foto, estado) |
| `CLIENTES` | Subtipo: dirección, fecha nacimiento, huella dactilar (BLOB) |
| `INSTRUCTORES` | Subtipo: especialidad del instructor |
| `ADMINISTRADORES` | Subtipo: rol (SUPERADMIN, ADMIN, RECEPCION) |
| `ESPECIALIDADES` | Catálogo de especialidades de instructores |
| `PLANES_MEMBRESIAS` | Catálogo de planes (mensual/semestral/anual) con precios y beneficios |
| `MEMBRESIAS` | Membresía asignada a un cliente con fechas, estado y modalidad |
| `HISTORIAL_MEMBRESIAS` | Trazabilidad de membresías asignadas a cada cliente |
| `PAGOS` | Transacciones con método (EFECTIVO, TRANSFERENCIA, TARJETA, NEQUI, DAVIPLATA) |
| `EJERCICIOS` | Catálogo de ejercicios con grupo muscular, nivel, duración |
| `RUTINAS` | Plan de entrenamiento personalizado por cliente e instructor |
| `RUTINA_EJERCICIOS` | Relación muchos-a-muchos entre rutinas y ejercicios (orden, día) |
| `PROGRESOS` | Medidas corporales (peso, altura, IMC, % grasa, masa muscular) |
| `CITAS` | Agendamiento cliente-instructor con tipo y estado |
| `REGISTROS_INGRESOS` | Control de entrada/salida con verificación biométrica |
| `NOTIFICACIONES` | Cola de notificaciones (SMS/EMAIL) con estado de envío |
| `PLANTILLAS_MENSAJE` | Plantillas reutilizables para SMS/EMAIL/WHATSAPP |
| `SESIONES_GYMBROT` | Sesiones conversacionales del chatbot por cliente |
| `MENSAJES_GYMBROT` | Mensajes individuales del chatbot con intención detectada |

### Índices

19 índices sobre columnas de búsqueda frecuente: `tipo_usuario`, `correo`, `fecha_vencimiento`, `id_cliente`, `estado_envio`, etc.

---

## Módulos y Funcionalidades

### 1. Login y Autenticación

- Inicio de sesión con usuario/contraseña (BCrypt)
- Validación de rol `ADMINISTRADOR`
- Indicador de estado del lector biométrico (conectado/desconectado)
- Animaciones de entrada en la tarjeta de login
- Sesión administrada por `SessionManager`

### 2. Dashboard (`/dashboard`)

- **Métricas**: total miembros activos, miembros en el gimnasio ahora, ingresos del mes
- **Gráfica de asistencia semanal**: barras por día de la semana (hoy resaltado en verde)
- **Demografía**: distribución etaria (adulto, menor, senior) con porcentajes
- **Horas pico**: histograma de 06:00 a 21:00
- **Alternancia diario/semanal** con animaciones
- Punto verde animado indicando "en vivo"

### 3. Gestión de Clientes (`/clientes`)

- CRUD completo con tabla de búsqueda y filtros
- Creación de cliente con formularios validados
- Perfil detallado: datos personales, membresía activa, progreso físico
- Asignación de membresía con selección de plan y modalidad
- Pago de membresía con selección de método

### 4. Gestión de Instructores (`/instructores`)

- CRUD completo con tabla
- Especialidades y disponibilidad
- Perfil con citas asignadas

### 5. Membresías y Planes (`/membresias`)

- Catálogo de planes con precios (mensual/semestral/anual)
- Asignación a clientes con control de fechas
- **Scheduler automático**: `MembresiaScheduler` actualiza membresías vencidas cada 24h
- Historial de membresías por cliente

### 6. Pagos (`/pago-membresia`)

- Formulario de pago con selector de método
- Métodos: EFECTIVO, TRANSFERENCIA, TARJETA, NEQUI, DAVIPLATA
- Referencia de transacción y observaciones
- Estados: EXITOSO, RECHAZADO, PENDIENTE

### 7. Finanzas (`/finanzas`)

- **Cards**: ingresos del mes, miembros activos, total ingresos
- **Gráfico de barras**: ingresos mensuales (12 meses, mes actual resaltado)
- **Gráfico de pastel**: distribución por método de pago
- **Gráfico de barras**: nuevos clientes por mes
- **Cards de ingresos por plan**
- **Tabla de pagos vencidos**
- **Historial de pagos** completo

### 8. Citas (`/citas`)

- Agendamiento de citas cliente-instructor
- Tipos: EVALUACION, SEGUIMIENTO, NUTRICION, CONSULTA
- Estados: PENDIENTE, CONFIRMADA, CANCELADA, COMPLETADA
- Perfil de cita con notas

### 9. Rutinas y Ejercicios (`/rutinas`, `/ejercicios`)

- Directorio de ejercicios con grupo muscular, nivel, series, repeticiones
- Asignación de rutinas por cliente con días de la semana
- Ejercicios ordenados dentro de cada rutina

### 10. Progreso Físico (`/progreso`)

- Registro de peso, altura, IMC, porcentaje de grasa, masa muscular
- Seguimiento histórico por cliente

### 11. Gymbro AI — Chatbot Inteligente (`/gymbro-ai`)

- Chat contextual con IA usando Groq API (LLaMA)
- Sesiones por cliente con historial persistente
- Intenciones detectadas: CONSULTA, AGENDA_CITA, VER_PROGRESO, SALUDO, NOTIFICACION
- Integración con agenda de citas y datos del cliente

### 12. Notificaciones

- **SMS**: vía Twilio API
- **Email**: vía JavaMail (Gmail SMTP)
- Plantillas de mensaje reutilizables
- Cola de notificaciones con estados (PENDIENTE, ENVIADO, FALLIDO)

### 13. Control de Ingreso Biométrico (`/registro-entrada`)

- Captura de huella con DigitalPersona U.are.U
- Verificación 1:1 contra la huella almacenada del cliente
- Registro histórico de ingresos/salidas
- Métodos de verificación: HUELLA, QR, MANUAL

---

## Documentación Arquitectónica

Los diagramas actualizados de la arquitectura completa del proyecto se encuentran en `docs/`:

| Recurso | Formato | Ubicación |
|---|---|---|
| Diagrama de clases completo (5 capas) | PlantUML | `docs/diagrama-clases.puml` |
| Diagramas por capa (modelo, dao, service, controller, util) | Mermaid | `docs/DIAGRAMA_CLASES.md` |
| Archivos individuales PlantUML | `.puml` | `docs/plantuml/` |

---

## Compilación y Ejecución

### Prerrequisitos

```bash
# Verificar Java 21
java --version

# Verificar Maven
mvn --version

# Configurar variables de entorno para Oracle
set GYMBROT_DB_URL=jdbc:oracle:thin:@localhost:1521:XE
set GYMBROT_DB_USER=gymbrot_admin
set GYMBROT_DB_PASS=mi_password

# (PowerShell)
$env:GYMBROT_DB_URL="jdbc:oracle:thin:@localhost:1521:XE"
$env:GYMBROT_DB_USER="gymbrot_admin"
$env:GYMBROT_DB_PASS="mi_password"
```

### Base de datos

```bash
# Ejecutar scripts en orden
sqlplus system/mi_password@XE @database/GYMBROT_DDL.sql
sqlplus system/mi_password@XE @database/GYMBROT_SEED.sql
sqlplus system/mi_password@XE @database/GYMBROT_RUTINAS_SEED.sql
```

### Compilar y ejecutar

```bash
# Compilar
mvn compile

# Ejecutar
mvn javafx:run
```

### Package (JAR)

```bash
mvn package
java --module-path target --module org.gymbrot/org.gymbrot.Main
```

---

## Licencia

Uso interno — proyecto académico UPC.
