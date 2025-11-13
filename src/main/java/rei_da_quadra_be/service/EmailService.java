package rei_da_quadra_be.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.model.User;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailConfirmacao(User user, String token) throws MessagingException {

    String assunto = "Confirme seu cadastro - Rei da Quadra Club";
    String linkConfirmacao = "http://localhost:4200/confirm-email?token=" + token;

    // Logo hospedada no Imgur pra aparecer no email
    String logoUrl = "https://i.imgur.com/xX9K3yG.png";

    String corpo = """
        <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 35px;">
            <div style="max-width: 650px; margin: auto; background: #ffffff; 
                        border-radius: 12px; padding: 35px; box-shadow: 0 4px 12px rgba(0,0,0,0.10);">

                <!-- LOGO CENTRALIZADA -->
                <div style="text-align: center; margin-bottom: 25px;">
                    <img src='%s' alt='Rei da Quadra Club' 
                         style='max-width: 180px; width: 100%%; height: auto;' />
                </div>

                <h2 style="color: #283040; text-align: left; font-size: 22px;">
                    Olá, <strong>%s</strong>!
                </h2>

                <p style="font-size: 16px; color: #444; line-height: 1.6;">
                    Seu cadastro no <strong>Rei da Quadra Club</strong> está quase pronto!<br><br>
                    Por favor, confirme seu e-mail clicando no botão abaixo:
                </p>

                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" 
                       style="background-color: #283040; color: white; text-decoration: none; 
                              padding: 15px 35px; border-radius: 6px; 
                              font-weight: bold; font-size: 16px; display: inline-block;">
                       Confirmar meu E-mail
                    </a>
                </div>

                <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">

                <p style="font-size: 14px; color: #666; line-height: 1.6;">
                    Se você não se cadastrou, por favor ignore este e-mail.<br>
                    Este e-mail foi enviado automaticamente. Não responda a esta mensagem.
                </p>

            </div>
        </div>
        """.formatted(logoUrl, user.getNome(), linkConfirmacao);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(user.getEmail());
        helper.setSubject(assunto);
        helper.setText(corpo, true);

        mailSender.send(message);
    }
}
