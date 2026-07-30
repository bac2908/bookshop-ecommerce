package vn.fs.backend.application.service;

import java.io.IOException;

import jakarta.mail.MessagingException;

import org.springframework.stereotype.Service;

import vn.fs.backend.application.dto.MailInfo;

/**
 * @author DongTHD
 *
 */
@Service
public interface SendMailService {
	void run();

	void queue(String to, String subject, String body);

	void queue(MailInfo mail);

	void send(MailInfo mail) throws MessagingException, IOException;
}
