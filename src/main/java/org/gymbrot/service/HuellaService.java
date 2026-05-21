package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.util.HuellaUtil;

import com.digitalpersona.onetouch.DPFPCapture;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapturePriority;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

import java.util.List;

/**
 * Servicio para gestión de huellas dactilares usando DigitalPersona SDK.
 * Maneja enrolamiento, verificación y almacenamiento de templates.
 */
public class HuellaService {

    private ClienteDAO clienteDAO;
    private HuellaUtil huellaUtil;
    private DPFPCapture lector;
    private DPFPEnrollment enroller;
    private DPFPVerification verificador;
    private DPFPFeatureExtraction extractor;

    // Estado del proceso de enrolamiento
    private boolean enrolandoActivo = false;
    private String idClienteEnrolando = null;

    public HuellaService() {
        this.clienteDAO = new ClienteDAO();
        this.huellaUtil = new HuellaUtil();
        this.verificador = DPFPGlobal.getVerificationFactory().createVerification();
        this.extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
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

            // Crear instancia del lector
            lector = DPFPGlobal.getCaptureFactory().createCapture();

            if (lector == null) {
                System.err.println("✗ No se pudo crear el lector");
                return false;
            }

            // Configurar prioridad de captura
            lector.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_HIGH);

            // Iniciar captura
            lector.startCapture();

            System.out.println("✓ Lector de huellas iniciado correctamente");
            System.out.println("  Dispositivo: " + lector.getReaderDescription());

            return true;

        } catch (Exception e) {
            System.err.println("Error al iniciar lector: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Detiene y libera el lector de huellas digitales.
     */
    public void detenerLector() {
        try {
            if (lector != null) {
                lector.stopCapture();
                lector = null;
                System.out.println("✓ Lector de huellas detenido");
            }

            // Limpiar estado de enrolamiento
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
            if (enroller.getTemplateStatus() == DPFPEnrollment.TEMPLATE_STATUS_READY) {
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