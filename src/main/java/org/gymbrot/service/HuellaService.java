package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.util.HuellaUtil;

import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.DPFPCapturePriority;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Servicio para gestión de huellas dactilares usando DigitalPersona SDK.
 * Maneja enrolamiento, verificación y almacenamiento de templates.
 */
public class HuellaService {

    private static HuellaService instancia;

    private ClienteDAO clienteDAO;
    private HuellaUtil huellaUtil;
    private DPFPCapture lector;
    private DPFPEnrollment enroller;
    private DPFPVerification verificador;
    private DPFPFeatureExtraction extractor;

    // Estado del proceso de enrolamiento
    private boolean enrolandoActivo = false;
    private String idClienteEnrolando = null;

    // Estado del lector
    private Boolean lectorConectado = null; // null = desconocido, true = conectado, false = desconectado

    // Múltiples callbacks para cambios de estado del lector
    private final List<Consumer<Boolean>> statusListeners = new ArrayList<>();

    // Polling periódico para detectar conexión/desconexión en tiempo real
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;

    public static HuellaService getInstancia() {
        if (instancia == null) {
            instancia = new HuellaService();
        }
        return instancia;
    }

    public HuellaService() {
        this.clienteDAO = new ClienteDAO();
        this.huellaUtil = new HuellaUtil();
        try {
            this.verificador = DPFPGlobal.getVerificationFactory().createVerification();
            this.extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        } catch (Exception e) {
            System.err.println("⚠ No se pudieron inicializar componentes de verificación: " + e.getMessage());
        }
    }

    /**
     * Registra un callback para cambios de estado del lector.
     * Notifica inmediatamente con el estado actual si ya se conoce.
     *
     * @param callback Consumer que recibe true (conectado) o false (desconectado)
     */
    public void addStatusListener(Consumer<Boolean> callback) {
        statusListeners.add(callback);
        if (lectorConectado != null) {
            callback.accept(lectorConectado);
        }
    }

    /**
     * Elimina un callback previamente registrado.
     */
    public void removeStatusListener(Consumer<Boolean> callback) {
        statusListeners.remove(callback);
    }

    /**
     * Inicializa y activa el lector de huellas digitales.
     *
     * @return true si se inicializó correctamente
     */
    public boolean iniciarLector() {
        try {
            if (lector != null) {
                System.out.println("⚠ Lector ya está activo");
                return true;
            }

            lector = DPFPGlobal.getCaptureFactory().createCapture();

            if (lector == null) {
                System.err.println("✗ No se pudo crear el lector");
                notificarStatus(false);
                return false;
            }

            lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
                @Override
                public void readerConnected(DPFPReaderStatusEvent e) {
                    System.out.println("✓ Lector conectado");
                    notificarStatus(true);
                }
                @Override
                public void readerDisconnected(DPFPReaderStatusEvent e) {
                    System.out.println("✗ Lector desconectado");
                    notificarStatus(false);
                }
            });

            lector.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_HIGH);

            // Intentar iniciar captura — si falla (DpHost ocupando el lector) no importa,
            // la detección de conectividad se hace vía ReadersCollection API
            try {
                lector.startCapture();
                System.out.println("✓ Captura iniciada correctamente");
            } catch (Exception e) {
                System.out.println("⚠ startCapture falló (posible conflicto con DpHost): " + e.getMessage());
            }

            // Detectar lector conectado mediante ReadersCollection (no requiere acceso exclusivo)
            boolean conectado = lectorEstaConectado();
            notificarStatus(conectado);

            // Iniciar polling para detectar cambios en tiempo real
            iniciarPolling();

            return conectado;

        } catch (Exception e) {
            System.err.println("Error al iniciar lector: " + e.getMessage());
            e.printStackTrace();
            lector = null;
            notificarStatus(false);
            return false;
        }
    }

    /**
     * Verifica si hay al menos un lector de huellas conectado al sistema
     * usando ReadersCollection API (no requiere startCapture).
     */
    private boolean lectorEstaConectado() {
        try {
            DPFPReadersCollection readers = DPFPGlobal.getReadersFactory().getReaders();
            boolean presente = readers != null && !readers.isEmpty();
            if (presente) {
                System.out.println("✓ Lector detectado: " + readers.get(0).getProductName());
            } else {
                System.out.println("✗ No se detectaron lectores");
            }
            return presente;
        } catch (Exception e) {
            System.out.println("⚠ Error al consultar ReadersCollection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inicia un polling periódico (cada 1s) para detectar cambios de conexión
     * del lector cuando los eventos del SDK no están disponibles.
     */
    public void iniciarPolling() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "lector-polling");
                t.setDaemon(true);
                return t;
            });
        }
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel(false);
        }
        pollingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean actual = lectorEstaConectado();
                if (lectorConectado == null || actual != lectorConectado) {
                    notificarStatus(actual);
                }
            } catch (Exception e) {
                // ignorar errores en polling
            }
        }, 0, 1, TimeUnit.SECONDS);
        System.out.println("✓ Polling de lector iniciado (cada 1s)");
    }

    /**
     * Detiene el polling periódico.
     */
    public void detenerPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        System.out.println("✗ Polling de lector detenido");
    }

    /**
     * Notifica a todos los callbacks registrados sobre un cambio de estado.
     */
    private void notificarStatus(boolean conectado) {
        lectorConectado = conectado;
        System.out.println("ℹ notificarStatus: " + (conectado ? "conectado" : "desconectado") + " | listeners: " + statusListeners.size());
        for (Consumer<Boolean> listener : statusListeners) {
            listener.accept(conectado);
        }
    }

    /**
     * Detiene y libera el lector de huellas digitales.
     */
    public void detenerLector() {
        detenerPolling();
        try {
            if (lector != null) {
                lector.stopCapture();
                lector = null;
                System.out.println("✓ Lector de huellas detenido");
            }

            enrolandoActivo = false;
            idClienteEnrolando = null;
            enroller = null;

        } catch (Exception e) {
            System.err.println("Error al detener lector: " + e.getMessage());
        }
    }
    /**
     * Inicia el proceso de enrolamiento de huella para un cliente.
     * Requiere capturar la misma huella 4 veces para garantizar calidad.
     *
     * @param idCliente ID del cliente a enrolar
     * @return true si se inició correctamente
     */
    public boolean enrollar(String idCliente) {
        try {
            // Verificar que el lector esté activo
            if (lector == null) {
                System.err.println("✗ Debe iniciar el lector primero");
                return false;
            }

            // Verificar que no haya otro enrolamiento en curso
            if (enrolandoActivo) {
                System.err.println("✗ Ya hay un enrolamiento en curso");
                return false;
            }

            // Verificar que el cliente exista
            Cliente cliente = clienteDAO.buscarPorId(idCliente);
            if (cliente == null) {
                System.err.println("✗ Cliente no encontrado");
                return false;
            }

            // Iniciar proceso de enrolamiento
            enroller = DPFPGlobal.getEnrollmentFactory().createEnrollment();
            enrolandoActivo = true;
            idClienteEnrolando = idCliente;

            System.out.println("✓ Enrolamiento iniciado para: " + cliente.getNombre());
            System.out.println("  Coloque el dedo en el lector 4 veces");
            System.out.println("  Muestras capturadas: 0/4");

            return true;

        } catch (Exception e) {
            System.err.println("Error al iniciar enrolamiento: " + e.getMessage());
            return false;
        }
    }

    /**
     * Procesa una muestra capturada durante el enrolamiento.
     *
     * @param sample Muestra capturada por el lector
     * @return true si se procesó correctamente, false si hay que repetir
     */
    public boolean procesarMuestraEnrolamiento(DPFPSample sample) {
        try {
            if (!enrolandoActivo || enroller == null) {
                System.err.println("✗ No hay enrolamiento activo");
                return false;
            }

            // Extraer características de la muestra
            DPFPFeatureSet features = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);

            if (features == null) {
                System.err.println("✗ Calidad de huella insuficiente - Intente de nuevo");
                return false;
            }

            // Agregar muestra al enrolamiento
            enroller.addFeatures(features);

            // Mostrar progreso
            int muestrasCapturadas = enroller.getFeaturesNeeded();
            int muestrasRestantes = enroller.getFeaturesNeeded();
            System.out.println("  Muestras capturadas: " + muestrasCapturadas + "/4");

            // Verificar si ya se completó el enrolamiento
            if (enroller.getFeaturesNeeded() <= 0) {
                return finalizarEnrolamiento();
            }

            return true;

        } catch (DPFPImageQualityException e) {
            System.err.println("✗ Calidad de imagen insuficiente");
            return false;
        } catch (Exception e) {
            System.err.println("Error procesando muestra: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finaliza el enrolamiento y guarda el template en la base de datos.
     */
    private boolean finalizarEnrolamiento() {
        try {
            // Crear el template final
            DPFPTemplate template = enroller.getTemplate();

            if (template == null) {
                System.err.println("✗ No se pudo crear el template");
                return false;
            }

            // Guardar en base de datos
            boolean guardado = guardarHuella(idClienteEnrolando, template);

            if (guardado) {
                System.out.println("✓ Enrolamiento completado exitosamente!");
                System.out.println("  Huella registrada para cliente: " + idClienteEnrolando);
            }

            // Limpiar estado
            enrolandoActivo = false;
            idClienteEnrolando = null;
            enroller = null;

            return guardado;

        } catch (Exception e) {
            System.err.println("Error finalizando enrolamiento: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extrae características de una muestra de huella.
     */
    private DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose) {
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }
    /**
     * Verifica una muestra de huella contra todos los templates en la BD.
     *
     * @param sample Muestra capturada por el lector
     * @return ID del cliente identificado o null si no se reconoce
     */
    public String verificar(DPFPSample sample) {
        try {
            // Extraer características de la muestra
            DPFPFeatureSet features = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);

            if (features == null) {
                System.err.println("✗ Calidad de huella insuficiente");
                return null;
            }

            // Obtener todos los clientes con huella registrada
            List<Cliente> clientes = clienteDAO.obtenerTemplatesHuella();

            if (clientes.isEmpty()) {
                System.err.println("✗ No hay huellas registradas en el sistema");
                return null;
            }

            System.out.println("Verificando contra " + clientes.size() + " huellas registradas...");

            // Comparar contra cada template
            for (Cliente cliente : clientes) {
                try {
                    // Deserializar template
                    byte[] templateBytes = cliente.getHuellaDactilar();
                    DPFPTemplate template = huellaUtil.deserializarTemplate(templateBytes);

                    if (template == null) {
                        continue;
                    }

                    // Verificar coincidencia
                    DPFPVerificationResult result = verificador.verify(features, template);

                    if (result.isVerified()) {
                        System.out.println("✓ Huella verificada!");
                        System.out.println("  Cliente: " + cliente.getNombre() + " " + cliente.getApellidos());
                        System.out.println("  Confianza: " + result.getFalseAcceptRate());

                        return cliente.getNumeroIdentificacion();
                    }

                } catch (Exception e) {
                    // Continuar con el siguiente cliente
                    continue;
                }
            }

            System.err.println("✗ Huella no reconocida");
            return null;

        } catch (Exception e) {
            System.err.println("Error en verificación: " + e.getMessage());
            return null;
        }
    }

    /**
     * Guarda el template de huella dactilar de un cliente en la BD.
     *
     * @param idCliente ID del cliente
     * @param template Template de huella a guardar
     * @return true si se guardó correctamente
     */
    public boolean guardarHuella(String idCliente, DPFPTemplate template) {
        try {
            // Serializar el template a bytes
            byte[] templateBytes = huellaUtil.serializarTemplate(template);

            if (templateBytes == null) {
                System.err.println("✗ Error al serializar template");
                return false;
            }

            // Buscar el cliente
            Cliente cliente = clienteDAO.buscarPorId(idCliente);

            if (cliente == null) {
                System.err.println("✗ Cliente no encontrado");
                return false;
            }

            // Establecer la huella
            cliente.setHuellaDactilar(templateBytes);

            // Actualizar en BD
            if (!clienteDAO.actualizar(cliente)) {
                System.err.println("✗ Error al guardar huella en BD");
                return false;
            }

            System.out.println("✓ Huella guardada correctamente");
            System.out.println("  Tamaño del template: " + templateBytes.length + " bytes");

            return true;

        } catch (Exception e) {
            System.err.println("Error al guardar huella: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si el lector está activo.
     */
    public boolean lectorActivo() {
        return lector != null;
    }

    /**
     * Verifica si hay un proceso de enrolamiento en curso.
     */
    public boolean estaEnrolando() {
        return enrolandoActivo;
    }

    /**
     * Cancela el enrolamiento actual.
     */
    public void cancelarEnrolamiento() {
        enrolandoActivo = false;
        idClienteEnrolando = null;
        enroller = null;
        System.out.println("⚠ Enrolamiento cancelado");
    }
}