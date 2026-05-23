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
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
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

    public interface EnrollmentCallback {
        void onStatus(String status);
        void onProgress(int captured, int total);
        void onSampleRejected(String reason);
        void onComplete(DPFPTemplate template);
        void onError(String error);
    }

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
    // Callback para eventos de captura (enrolamiento)
    private EnrollmentCallback enrollCallback;
    // Flag que indica si la captura activa (startCapture) está funcionando
    private boolean capturaActiva = false;

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
        // NOTA: verificador y extractor se crean bajo demanda (lazy)
        // para evitar que su inicialización nativa interfiera con startCapture()
    }

    /**
     * Registra un callback para cambios de estado del lector.
     * Notifica inmediatamente con el estado actual si ya se conoce.
     *
     * @param callback Consumer que recibe true (conectado) o false (desconectado)
     */
    public void addStatusListener(Consumer<Boolean> callback) {
        statusListeners.add(callback);
        callback.accept(lectorConectado != null ? lectorConectado : false);
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
        // El lector se crea bajo demanda en habilitarCaptura() para evitar
        // que la creación temprana interfiera con el estado de la API nativa.
        // Solo iniciamos el polling para detectar conectividad.
        iniciarPolling();
        return true;
    }

    /**
     * Verifica si hay al menos un lector de huellas conectado al sistema
     * usando ReadersCollection API (no requiere startCapture).
     */
    private boolean lectorEstaConectado() {
        try {
            DPFPReadersCollection readers = DPFPGlobal.getReadersFactory().getReaders();
            return readers != null && !readers.isEmpty();
        } catch (Exception e) {
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
    }

    private void notificarStatus(boolean conectado) {
        lectorConectado = conectado;
        for (Consumer<Boolean> listener : statusListeners) {
            listener.accept(conectado);
        }
    }

    // ─── DpHost control ─────────────────────────────────────────────────
    private boolean detenerDpHost() {
        try {
            Process p = new ProcessBuilder("sc", "stop", "DpHost").start();
            p.waitFor(5, TimeUnit.SECONDS);
            // Force-kill cualquier proceso residual de DpHost
            new ProcessBuilder("taskkill", "/f", "/im", "DpHostW.exe").start();
            Thread.sleep(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean reiniciarDpHost() {
        try {
            new ProcessBuilder("sc", "start", "DpHost").start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean forzarLimpiezaAPI() {
        try {
            // Forzar limpieza de la API DPFP en nuestro proceso:
            // 1. Forzar garbage collection + finalization para liberar objetos nativos
            // 2. Llamar DPFPApi_Exit via rundll32
            System.gc();
            System.runFinalization();
            Thread.sleep(300);
            ProcessBuilder pb = new ProcessBuilder(
                "rundll32.exe",
                "C:\\Windows\\System32\\DPFPApi.dll",
                "DPFPApi_Exit"
            );
            Process p = pb.start();
            p.waitFor(3, TimeUnit.SECONDS);
            Thread.sleep(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean iniciarDpHost() {
        try {
            Process p = new ProcessBuilder("sc", "start", "DpHost").start();
            p.waitFor(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Intenta cargar los controladores del kernel manualmente.
     * DpHost los carga al iniciar; al detenerlo se descargan.
     */
    private boolean cargarDriversKernel() {
        try {
            new ProcessBuilder("sc", "start", "dpK00701").start();
            new ProcessBuilder("sc", "start", "usbdpfp").start();
            Thread.sleep(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Captura activa (on-demand) ──────────────────────────────────────

    private String obtenerIdDispositivoUSB() {
        try {
            Process p = new ProcessBuilder("pnputil", "/enum-devices").start();
            String output = new String(p.getInputStream().readAllBytes());
            for (String line : output.split("\n")) {
                if (line.contains("USB\\VID_05BA&PID_000A")) {
                    return line.replaceAll(".*Id. de instancia:\\s*", "").trim();
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("⚠ Error obteniendo ID USB: " + e.getMessage());
            return null;
        }
    }

    private boolean reiniciarApiDPFP() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "rundll32.exe",
                "C:\\Windows\\System32\\DPFPApi.dll",
                "DPFPApi_Exit"
            );
            Process p = pb.start();
            p.waitFor(3, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Habilita la captura activa del lector.
     * El sample C++ funciona con DpHost activo sin problemas.
     * El problema de nuestra app era que verificador/extractor
     * se inicializaban antes que startCapture() y eso interfería.
     * Ahora son lazy, así que solo creamos lector + startCapture.
     */
    public boolean habilitarCaptura() {
        if (capturaActiva) return true;
        try {
            if (lector == null) {
                lector = DPFPGlobal.getCaptureFactory().createCapture();
                if (lector == null) return false;
                lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
                    @Override public void readerConnected(DPFPReaderStatusEvent e) { notificarStatus(true); }
                    @Override public void readerDisconnected(DPFPReaderStatusEvent e) { notificarStatus(false); }
                });
                lector.addDataListener(new DPFPDataAdapter() {
                    @Override public void dataAcquired(DPFPDataEvent e) {
                        if (enrolandoActivo) procesarMuestraEnrolamiento(e.getSample());
                    }
                });
                lector.addSensorListener(new DPFPSensorAdapter() {
                    @Override public void fingerTouched(DPFPSensorEvent e) {
                        if (enrolandoActivo && enrollCallback != null) enrollCallback.onStatus("Procesando huella...");
                    }
                    @Override public void fingerGone(DPFPSensorEvent e) {
                        if (enrolandoActivo && enrollCallback != null) enrollCallback.onStatus("Dedo retirado, espere...");
                    }
                });
                lector.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_HIGH);
            }

            lector.startCapture();
            capturaActiva = true;
            System.out.println("✓ Captura activa iniciada");
            return true;
        } catch (Exception e) {
            System.err.println("✗ Error al habilitar captura: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void recrearLector() {
        try { if (lector != null) lector.stopCapture(); } catch (Exception e) { }
        lector = DPFPGlobal.getCaptureFactory().createCapture();
        if (lector == null) return;
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
        lector.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(DPFPDataEvent e) {
                if (enrolandoActivo) {
                    procesarMuestraEnrolamiento(e.getSample());
                }
            }
        });
        lector.addSensorListener(new DPFPSensorAdapter() {
            @Override
            public void fingerTouched(DPFPSensorEvent e) {
                if (enrolandoActivo && enrollCallback != null) {
                    enrollCallback.onStatus("Procesando huella...");
                }
            }
            @Override
            public void fingerGone(DPFPSensorEvent e) {
                if (enrolandoActivo && enrollCallback != null) {
                    enrollCallback.onStatus("Dedo retirado, espere...");
                }
            }
        });
        lector.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_HIGH);
    }

    /**
     * Deshabilita la captura activa y restaura DpHost.
     */
    public void deshabilitarCaptura() {
        try {
            if (lector != null) lector.stopCapture();
        } catch (Exception e) { }
        capturaActiva = false;
        System.out.println("⏳ Restaurando DpHost...");
        iniciarDpHost();
        // Reanudar polling
        iniciarPolling();
        System.out.println("✓ Captura desactivada");
    }

    // ─── Enrollment con captura real (similar a PruebaBiometrica) ───────

    /**
     * Inicia el proceso completo de enrolamiento con captura real:
     * 1. Crea el enroller
     * 2. Detiene DpHost y activa startCapture()
     * 3. Los eventos dataAcquired/fingerTouched actualizan el progreso vía callback
     * 4. Al completar las 4 muestras, deshabilita captura y restaura DpHost
     */
    public void iniciarEnrolamientoConCaptura(EnrollmentCallback callback) {
        this.enrollCallback = callback;

        try {
            enroller = DPFPGlobal.getEnrollmentFactory().createEnrollment();
            enrolandoActivo = true;
            idClienteEnrolando = null;
        } catch (Exception e) {
            if (callback != null) callback.onError("Error al crear enroller: " + e.getMessage());
            return;
        }

        if (callback != null) callback.onStatus("Preparando lector...");

        CompletableFuture.runAsync(() -> {
            boolean ok = habilitarCaptura();
            if (ok) {
                if (callback != null) callback.onStatus("Coloque el dedo en el lector");
            } else {
                enrolandoActivo = false;
                enroller = null;
                if (callback != null) callback.onError("No se pudo iniciar la captura. Asegúrese de que el lector está conectado.");
            }
        });
    }

    /**
     * Obtiene el template generado al finalizar el enrolamiento.
     */
    public DPFPTemplate obtenerTemplate() {
        try {
            return enroller != null ? enroller.getTemplate() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Indica si la captura activa (startCapture) está funcionando.
     */
    public boolean isCapturaActiva() {
        return capturaActiva;
    }
    
    /**
     * Detiene y libera el lector de huellas digitales.
     */
    public void detenerLector() {
        detenerPolling();
        try {
            if (capturaActiva) {
                try { lector.stopCapture(); } catch (Exception e) { }
                iniciarDpHost();
                capturaActiva = false;
            }
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
                return false;
            }

            // Extraer características de la muestra
            DPFPFeatureSet features = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);

            if (features == null) {
                if (enrollCallback != null) enrollCallback.onSampleRejected("Calidad de huella insuficiente");
                return false;
            }

            int neededAntes = enroller.getFeaturesNeeded();
            enroller.addFeatures(features);
            int neededDespues = enroller.getFeaturesNeeded();
            int capturadas = 4 - neededDespues;

            if (neededAntes == neededDespues) {
                // La muestra no se agregó (repetida o inválida)
                if (enrollCallback != null) enrollCallback.onSampleRejected("Muestra repetida o inválida");
                return false;
            }

            if (enrollCallback != null) {
                enrollCallback.onProgress(capturadas, 4);
                if (capturadas < 4) {
                    enrollCallback.onStatus("Muestra " + capturadas + "/4 capturada");
                }
            }

            // Verificar si ya se completó el enrolamiento
            if (neededDespues <= 0) {
                DPFPTemplate template = obtenerTemplate();
                deshabilitarCaptura();
                enrolandoActivo = false;
                enroller = null;
                if (template != null) {
                    if (enrollCallback != null) enrollCallback.onComplete(template);
                } else {
                    if (enrollCallback != null) enrollCallback.onError("Error al generar template");
                }
            }

            return true;

        } catch (DPFPImageQualityException e) {
            if (enrollCallback != null) enrollCallback.onSampleRejected("Calidad de imagen insuficiente");
            return false;
        } catch (Exception e) {
            if (enrollCallback != null) enrollCallback.onError("Error: " + e.getMessage());
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
            if (extractor == null) {
                extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            }
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

            // Inicializar verificador bajo demanda
            if (verificador == null) {
                verificador = DPFPGlobal.getVerificationFactory().createVerification();
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