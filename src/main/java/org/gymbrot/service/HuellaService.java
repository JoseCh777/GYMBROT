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
}