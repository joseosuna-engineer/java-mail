package com.prottonne.mail.service;

import com.prottonne.mail.dto.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Base64;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailServiceImpl {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String MAILSMTPSTARTTLSREQUIRED = "mail.smtp.starttls.required";
    private static final String MAILSMTPSTARTTLSENABLE = "mail.smtp.starttls.enable";
    private static final String TRUE = "true";
    private static final String MAILSMTPAUTH = "mail.smtp.auth";
    private static final String MAILSMTPPORT = "mail.smtp.port";
    private static final String SMTPS = "smtps";
    private static final String MAILTRANSPORTPROTOCOL = "mail.transport.protocol";

    @Value("${smtp.user}")
    private String smtpUser;
    @Value("${smtp.pass}")
    private String smtpPass;
    @Value("${smtp.host}")
    private String smtpHost;
    @Value("${smtp.port}")
    private String smtpPort;

    public Boolean send(Request request) throws Exception {

        // Create a Properties object to contain connection configuration information.
        Properties props = System.getProperties();
        props.put(MAILTRANSPORTPROTOCOL, SMTPS);
        props.put(MAILSMTPPORT, smtpPort);

        // Set properties indicating that we want to use STARTTLS to encrypt the connection.
        // The SMTP session will begin on an unencrypted connection, and then the client
        // will issue a STARTTLS command to upgrade to an encrypted connection.
        props.put(MAILSMTPAUTH, TRUE);
        props.put(MAILSMTPSTARTTLSENABLE, TRUE);
        props.put(MAILSMTPSTARTTLSREQUIRED, TRUE);

        // Create a Session object to represent a mail session with the specified properties.
        Session session = Session.getDefaultInstance(props);

        //Text + file Multipart
        MimeMultipart multiPart = new MimeMultipart();

        // text part
        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setContent(request.getContent(), "text/html; charset=utf-8");
        multiPart.addBodyPart(textBodyPart);

        // attached part
        BodyPart attachedPart = new MimeBodyPart();

        byte[] attachedFile = Base64.getDecoder().decode(request.getBase64AttachedFile().getBytes());

        DataSource dataSource = new ByteArrayDataSource(attachedFile, "application/pdf");
        try {
            attachedPart.setDataHandler(new DataHandler(dataSource));
            attachedPart.setFileName(request.getFileName());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        multiPart.addBodyPart(attachedPart);

        // Add all the Recipients
        InternetAddress[] internetAddress = new InternetAddress[request.getRecipients().size()];
        for (int i = 0; request.getRecipients().size() > i; i++) {
            internetAddress[i] = new InternetAddress(request.getRecipients().get(i));
        }

        Address address = new InternetAddress(request.getOutputAddress(), request.getOutputName());

        MimeMessage message = new MimeMessage(session);

        message.setFrom(address);
        message.setRecipients(Message.RecipientType.TO, internetAddress);
        message.setSubject(request.getSubject());
        message.setContent(multiPart);

        // Create a transport.
        Transport transport = session.getTransport();

        // Connect to smtp server
        transport.connect(smtpHost, smtpUser, smtpPass);

        // Send the email.
        transport.sendMessage(message, message.getAllRecipients());

        // Close and terminate the connection.
        transport.close();

        return Boolean.TRUE;

    }

}
