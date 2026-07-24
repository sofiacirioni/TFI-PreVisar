package ar.edu.utn.frc.previsar.services.Impl;

import java.math.BigDecimal;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import ar.edu.utn.frc.previsar.config.PrevisarMailProperties;
import ar.edu.utn.frc.previsar.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final PrevisarMailProperties props;

    @Async
    @Override
    public void notificarPagoAcreditado(Long expedienteId, String destinatario, BigDecimal monto) {
        enviar(destinatario, "PreVisar - Pago acreditado",
                """
                        Hola,

                        Registramos la acreditación del pago del arancel correspondiente al expediente N° %d.
                        Monto: $ %s

                        Ya podés continuar con el armado del expediente ingresando a PreVisar.
                        """.formatted(expedienteId, monto));
    }

    @Async
    @Override
    public void notificarSolicitudRolRevisor(SolicitudRevisor d) {
        enviar(props.colegio(), "PreVisar - Solicitud de rol revisor",
                """
                        El siguiente profesional solicita acceso a las funciones de revisor:

                        Nombre: %s %s
                        Matrícula / N° de orden: %s / %s
                        Correo: %s
                        Provincia: %s

                        Mensaje: %s

                        Si corresponde autorizarlo, debe habilitarse el rol desde la administración del sistema.
                        """.formatted(d.nombre(), d.apellido(), d.matricula(), d.numeroOrden(),
                        d.email(), d.provincia(), d.mensaje() == null ? "(sin mensaje)" : d.mensaje()));
    }

    /**
     * Nunca propaga: un fallo de correo no puede romper la operación que lo
     * disparó. Si el destinatario no está configurado (ej. la casilla de la
     * institución), se omite y se avisa en el log.
     */
    private void enviar(String para, String asunto, String cuerpo) {
        if (!props.habilitado()) {
            log.info("Correo deshabilitado (previsar.mail.habilitado=false), se omite envío a {}", para);
            return;
        }
        if (para == null || para.isBlank()) {
            log.warn("Sin destinatario configurado para «{}»: se omite el envío", asunto);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.from());
            msg.setTo(para);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
            log.info("Correo enviado a {}: {}", para, asunto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }
}
