package com.guestbot.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_PREFIX = "verify:code:";
    private static final int CODE_TTL_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@guestbot.app}")
    private String fromEmail;

    public void sendVerificationCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        sendEmail(email, code);
    }

    public boolean verifyCode(String email, String code) {
        String stored = redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (stored != null && stored.equals(code)) {
            redisTemplate.delete(CODE_PREFIX + email);
            return true;
        }
        return false;
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void sendEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("GuestBot — код подтверждения");
            message.setText(
                "Здравствуйте!\n\n" +
                "Ваш код подтверждения для регистрации в GuestBot:\n\n" +
                "  " + code + "\n\n" +
                "Код действителен 10 минут.\n\n" +
                "Если вы не регистрировались — просто проигнорируйте это письмо."
            );
            mailSender.send(message);
            log.info("Verification code sent to {}", email);
        } catch (MailException e) {
            // В dev режиме mail может быть не настроен — логируем код
            log.warn("Failed to send email to {}. Code for dev: {}", email, redisTemplate.opsForValue().get(CODE_PREFIX + email));
        }
    }
}
