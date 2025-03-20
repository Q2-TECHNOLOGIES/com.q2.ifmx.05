/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.ArrayList;

/**
 *
 * @author 4dm1n
 */
public class EmailObject {
    private String smtp_host;
    private String smtp_port;
    private String email_user;
    private String email_password;
    private String email_subject;
    private String email_message;
    private ArrayList<String> list_recipients;
    private ArrayList<MessageContent> list_message;

    public ArrayList<MessageContent> getList_message() {
        return list_message;
    }

    public void setList_message(ArrayList<MessageContent> list_message) {
        this.list_message = list_message;
    }

    public ArrayList<String> getList_recipients() {
        return list_recipients;
    }

    public void setList_recipients(ArrayList<String> list_recipients) {
        this.list_recipients = list_recipients;
    }

    public String getSmtp_host() {
        return smtp_host;
    }

    public void setSmtp_host(String smtp_host) {
        this.smtp_host = smtp_host;
    }

    public String getSmtp_port() {
        return smtp_port;
    }

    public void setSmtp_port(String smtp_port) {
        this.smtp_port = smtp_port;
    }

    public String getEmail_user() {
        return email_user;
    }

    public void setEmail_user(String email_user) {
        this.email_user = email_user;
    }

    public String getEmail_password() {
        return email_password;
    }

    public void setEmail_password(String email_password) {
        this.email_password = email_password;
    }

    public String getEmail_subject() {
        return email_subject;
    }

    public void setEmail_subject(String email_subject) {
        this.email_subject = email_subject;
    }

    public String getEmail_message() {
        return email_message;
    }

    public void setEmail_message(String email_message) {
        this.email_message = email_message;
    }
    
    
}
