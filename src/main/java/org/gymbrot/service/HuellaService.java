package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.util.HuellaUtil;

import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class HuellaService {

    public interface EnrollmentCallback {
        void onStatus(String status);
        void onProgress(int captured, int total);
        void onSampleRejected(String reason);
        void onComplete(byte[] templateBytes);
        void onError(String error);
    }

    public interface VerificacionCallback {
        void onStatus(String status);
        void onIdentificado(Cliente cliente);
        void onNoIdentificado();
        void onError(String error);
    }

    private static HuellaService instancia;

    private ClienteDAO clienteDAO;
    private HuellaUtil huellaUtil;
    private DPFPVerification verificador;
    private DPFPFeatureExtraction extractor;

    private Boolean lectorConectado = null;
    private final List<Consumer<Boolean>> statusListeners = new ArrayList<>();
    private EnrollmentCallback enrollCallback;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;
    private Process procesoCaptura;
    private boolean enrolamientoActivo = false;
    private String idClienteEnrolando = null;

    public static HuellaService getInstancia() {
        if (instancia == null) {
            instancia = new HuellaService();
        }
        return instancia;
    }

    public HuellaService() {
        this.clienteDAO = new ClienteDAO();
        this.huellaUtil = new HuellaUtil();
    }

    public void addStatusListener(Consumer<Boolean> callback) {
        statusListeners.add(callback);
        callback.accept(lectorConectado != null ? lectorConectado : false);
    }

    public void removeStatusListener(Consumer<Boolean> callback) {
        statusListeners.remove(callback);
    }

    public boolean iniciarLector() {
        iniciarPolling();
        return true;
    }

    public void detenerLector() {
        cancelarEnrolamiento();
        detenerPolling();
    }

    private boolean lectorEstaConectado() {
        try {
            DPFPReadersCollection readers = DPFPGlobal.getReadersFactory().getReaders();
            return readers != null && !readers.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

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
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

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

    private String encontrarEjecutable() {
        String[] candidatos = {
            "capture/CapturadorHuella.exe",
            "../capture/CapturadorHuella.exe",
            System.getProperty("user.dir") + "/capture/CapturadorHuella.exe"
        };
        for (String c : candidatos) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }

    public void iniciarEnrolamientoConCaptura(EnrollmentCallback callback) {
        this.enrollCallback = callback;

        System.out.println("[HuellaService] iniciarEnrolamientoConCaptura llamado");
        String exePath = encontrarEjecutable();
        System.out.println("[HuellaService] exePath=" + exePath + " user.dir=" + System.getProperty("user.dir"));
        if (exePath == null) {
            System.err.println("[HuellaService] ERROR: No se encuentra CapturadorHuella.exe");
            if (callback != null) callback.onError("No se encuentra CapturadorHuella.exe en capture/");
            return;
        }

        enrolamientoActivo = true;

        CompletableFuture.runAsync(() -> {
            try {
                if (callback != null) callback.onStatus("Preparando lector...");

                System.out.println("[HuellaService] Iniciando proceso: " + exePath);
                ProcessBuilder pb = new ProcessBuilder(exePath);
                pb.redirectErrorStream(false);
                procesoCaptura = pb.start();
                System.out.println("[HuellaService] Proceso iniciado, PID=" + procesoCaptura.pid());

                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(procesoCaptura.getErrorStream()));
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(procesoCaptura.getInputStream()));

                StringBuilder templateBase64 = new StringBuilder();
                Thread stderrThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stderrReader.readLine()) != null) {
                            if (line.startsWith("DEBUG:")) {
                                System.out.println("[C# DEBUG] " + line.substring(6));
                                continue;
                            }
                            if (callback == null) continue;
                            if (line.startsWith("STATUS:")) {
                                callback.onStatus(line.substring(7));
                            } else if (line.startsWith("PROGRESS:")) {
                                String[] parts = line.substring(9).split("/");
                                if (parts.length == 2) {
                                    int capturadas = Integer.parseInt(parts[0]);
                                    int total = Integer.parseInt(parts[1]);
                                    callback.onProgress(capturadas, total);
                                }
                            } else if (line.startsWith("SAMPLE_REJECTED:")) {
                                callback.onSampleRejected(line.substring(16));
                            } else if (line.startsWith("ERROR:")) {
                                callback.onError(line.substring(6));
                            }
                        }
                    } catch (Exception e) {
                    }
                });
                stderrThread.setDaemon(true);
                stderrThread.start();

                Thread stdoutThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stdoutReader.readLine()) != null) {
                            templateBase64.append(line);
                        }
                    } catch (Exception e) {
                    }
                });
                stdoutThread.setDaemon(true);
                stdoutThread.start();

                System.out.println("[HuellaService] Esperando proceso...");
                boolean finished = procesoCaptura.waitFor(120, TimeUnit.SECONDS);
                int exitCode = procesoCaptura.exitValue();
                System.out.println("[HuellaService] Proceso terminado. exitCode=" + exitCode + " finished=" + finished + " stdoutLen=" + templateBase64.length());

                if (!finished) {
                    procesoCaptura.destroyForcibly();
                    System.err.println("[HuellaService] Timeout 120s");
                    if (callback != null) callback.onError("Tiempo de espera agotado (120s)");
                    return;
                }
                stderrThread.join(2000);
                stdoutThread.join(2000);
                procesoCaptura = null;

                if (templateBase64.length() > 0) {
                    System.out.println("[HuellaService] Decodificando template...");
                    byte[] templateBytes = Base64.getDecoder().decode(templateBase64.toString());
                    System.out.println("[HuellaService] Template OK, " + templateBytes.length + " bytes, llamando onComplete");
                    if (callback != null) callback.onComplete(templateBytes);
                } else {
                    System.err.println("[HuellaService] No se recibio template (stdout vacio)");
                    if (callback != null) callback.onError("No se recibio template del capturador");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Error: " + e.getMessage());
            } finally {
                enrolamientoActivo = false;
                idClienteEnrolando = null;
                procesoCaptura = null;
            }
        });
    }

    public boolean estaEnrolando() {
        return enrolamientoActivo;
    }

    public boolean lectorActivo() {
        return lectorConectado != null && lectorConectado;
    }

    public boolean isCapturaActiva() {
        return procesoCaptura != null && procesoCaptura.isAlive();
    }

    public void deshabilitarCaptura() {
        cancelarEnrolamiento();
    }

    public void cancelarEnrolamiento() {
        if (procesoCaptura != null && procesoCaptura.isAlive()) {
            procesoCaptura.destroyForcibly();
            procesoCaptura = null;
        }
        enrolamientoActivo = false;
        idClienteEnrolando = null;
    }

    public void verificarConCaptura(VerificacionCallback callback) {
        String exePath = encontrarEjecutable();
        if (exePath == null) {
            if (callback != null) callback.onError("No se encuentra CapturadorHuella.exe");
            return;
        }

        List<Cliente> clientes = clienteDAO.obtenerTemplatesHuella();
        if (clientes == null || clientes.isEmpty()) {
            if (callback != null) callback.onError("No hay huellas registradas en el sistema");
            return;
        }

        enrolamientoActivo = true;

        CompletableFuture.runAsync(() -> {
            try {
                if (callback != null) callback.onStatus("Preparando lector...");

                ProcessBuilder pb = new ProcessBuilder(exePath, "verify");
                pb.redirectErrorStream(false);
                procesoCaptura = pb.start();

                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(procesoCaptura.getErrorStream()));
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(procesoCaptura.getInputStream()));
                var stdinWriter = new java.io.OutputStreamWriter(procesoCaptura.getOutputStream());

                for (Cliente c : clientes) {
                    byte[] tb = c.getHuellaDactilar();
                    if (tb != null && tb.length > 0) {
                        stdinWriter.write(Base64.getEncoder().encodeToString(tb));
                        stdinWriter.write("\n");
                    }
                }
                stdinWriter.close();

                StringBuilder resultado = new StringBuilder();
                Thread stderrThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stderrReader.readLine()) != null) {
                            if (line.startsWith("DEBUG:")) {
                                System.out.println("[C# DEBUG] " + line.substring(6));
                                continue;
                            }
                            if (callback == null) continue;
                            if (line.startsWith("STATUS:")) {
                                callback.onStatus(line.substring(7));
                            } else if (line.startsWith("ERROR:")) {
                                callback.onError(line.substring(6));
                            }
                        }
                    } catch (Exception e) { }
                });
                stderrThread.setDaemon(true);
                stderrThread.start();

                Thread stdoutThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = stdoutReader.readLine()) != null) {
                            resultado.append(line);
                        }
                    } catch (Exception e) { }
                });
                stdoutThread.setDaemon(true);
                stdoutThread.start();

                boolean finished = procesoCaptura.waitFor(120, TimeUnit.SECONDS);
                if (!finished) {
                    procesoCaptura.destroyForcibly();
                    if (callback != null) callback.onError("Tiempo de espera agotado (120s)");
                    return;
                }
                stderrThread.join(2000);
                stdoutThread.join(2000);

                String result = resultado.toString().trim();
                System.out.println("[HuellaService] verify result: " + result);

                if (result.startsWith("MATCH:")) {
                    int idx = Integer.parseInt(result.substring(6));
                    if (idx >= 0 && idx < clientes.size()) {
                        if (callback != null) callback.onIdentificado(clientes.get(idx));
                    } else {
                        if (callback != null) callback.onNoIdentificado();
                    }
                } else {
                    if (callback != null) callback.onNoIdentificado();
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Error: " + e.getMessage());
            } finally {
                enrolamientoActivo = false;
                procesoCaptura = null;
            }
        });
    }

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

    public String verificar(DPFPSample sample) {
        try {
            DPFPFeatureSet features = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
            if (features == null) {
                System.err.println("Calidad de huella insuficiente");
                return null;
            }
            if (verificador == null) {
                verificador = DPFPGlobal.getVerificationFactory().createVerification();
            }
            List<Cliente> clientes = clienteDAO.obtenerTemplatesHuella();
            if (clientes.isEmpty()) {
                System.err.println("No hay huellas registradas en el sistema");
                return null;
            }
            for (Cliente cliente : clientes) {
                try {
                    byte[] templateBytes = cliente.getHuellaDactilar();
                    DPFPTemplate template = huellaUtil.deserializarTemplate(templateBytes);
                    if (template == null) continue;
                    DPFPVerificationResult result = verificador.verify(features, template);
                    if (result.isVerified()) {
                        return cliente.getNumeroIdentificacion();
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error en verificación: " + e.getMessage());
            return null;
        }
    }

    public boolean guardarHuella(String idCliente, DPFPTemplate template) {
        try {
            byte[] templateBytes = huellaUtil.serializarTemplate(template);
            if (templateBytes == null) return false;
            Cliente cliente = clienteDAO.buscarPorId(idCliente);
            if (cliente == null) return false;
            cliente.setHuellaDactilar(templateBytes);
            return clienteDAO.actualizar(cliente);
        } catch (Exception e) {
            System.err.println("Error al guardar huella: " + e.getMessage());
            return false;
        }
    }
}
