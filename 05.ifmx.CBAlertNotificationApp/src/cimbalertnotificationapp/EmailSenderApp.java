/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbalertnotificationapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import model.EmailObject;
import model.MessageContent;
//import org.apache.log4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author firman
 */
public class EmailSenderApp {
    //final static Logger logger = Logger.getLogger(EmailSenderApp.class);
    
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("EmailSenderApp");
    
    
    public boolean sendEmail(EmailObject emailObject){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        
        Properties email_prop = new Properties();
        email_prop.put("mail.smtp.auth", "false");
        email_prop.put("mail.smtp.starttls.enable", "false");
        email_prop.put("mail.smtp.host", emailObject.getSmtp_host());
        email_prop.put("mail.smtp.port", emailObject.getSmtp_port());
        
        Session session = Session.getInstance(email_prop,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailObject.getEmail_user(), emailObject.getEmail_password());
                }
            }
        );
        
        ArrayList<String> list_recipients = emailObject.getList_recipients();
        String tmp_mail_recipients = "";
        InternetAddress[] internetAddress = new InternetAddress[list_recipients.size()];
        for(int i=0; i<list_recipients.size(); i++){
            InternetAddress addr = new InternetAddress();
            addr.setAddress(list_recipients.get(i));
            internetAddress[i] = addr;
            tmp_mail_recipients += list_recipients.get(i)+";";
        }
        
        try{
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailObject.getEmail_user()));
            message.setRecipients(Message.RecipientType.TO, internetAddress);
            message.setSubject(emailObject.getEmail_subject());
            message.setHeader("X-Priority", "1");
            message.setContent(emailObject.getEmail_message(), "text/html; charset=utf-8");
            Transport.send(message);
            
            logger.info("######EMAIL NOTIFICATION######");
            logger.info("EMAIL SENT TO : "+tmp_mail_recipients);
            logger.info("TIMESTAMP : "+dateFormat.format(new Date()));
            
            String alert_id = "";
            ArrayList<MessageContent> listMsgContent = emailObject.getList_message();
            if(listMsgContent.size() > 0){
                alert_id = listMsgContent.get(0).getAlert_id();
            }
            
            logger.info("ALERT CWI : "+alert_id);
            logger.info("STATUS : EMAIL SENT");
            logger.info("######END OF EMAIL NOTIFICATION######");
            
            System.out.println("######EMAIL NOTIFICATION######");
            System.out.println("EMAIL SENT TO : "+tmp_mail_recipients);
            System.out.println("TIMESTAMP : "+dateFormat.format(new Date()));
            System.out.println("ALERT CWI : "+alert_id);
            System.out.println("STATUS : EMAIL SENT");
            System.out.println("######END OF EMAIL NOTIFICATION######");
            
            return true;
        } catch(MessagingException e){
            System.out.println(e.getMessage());
            return false;
        }
    }
}
