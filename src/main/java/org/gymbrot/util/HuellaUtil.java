package org.gymbrot.util;

import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.DPFPGlobal;

public class HuellaUtil {

    public byte[] serializarTemplate(DPFPTemplate template) {
        if (template == null) return null;
        try {
            return template.serialize();
        } catch (Exception e) {
            System.err.println("Error serializando template: " + e.getMessage());
            return null;
        }
    }

    public DPFPTemplate deserializarTemplate(byte[] templateBytes) {
        if (templateBytes == null || templateBytes.length == 0) return null;
        try {
            DPFPTemplate template = DPFPGlobal.getTemplateFactory().createTemplate();
            template.deserialize(templateBytes);
            return template;
        } catch (Exception e) {
            System.err.println("Error deserializando template: " + e.getMessage());
            return null;
        }
    }
}
