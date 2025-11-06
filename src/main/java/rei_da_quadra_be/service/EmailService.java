package rei_da_quadra_be.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailConfirmacao(String destinatario, String token) throws MessagingException {
        String assunto = "Confirme seu cadastro - Rei da Quadra";
        String linkConfirmacao = "http://localhost:4200/confirm-email?token=" + token;

        String corpo = """
            <h2>Bem-vindo ao Rei da Quadra!</h2>
            <p>Para concluir seu cadastro, clique no link abaixo:</p>
            <a href="%s" style="background:#283040;color:white;padding:10px 20px;border-radius:6px;text-decoration:none;">Confirmar Email</a>
            <br><br>
            <p>Se você não se cadastrou, ignore este email.</p>
        """.formatted(linkConfirmacao);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(corpo, true);

        mailSender.send(message);
    }
}
