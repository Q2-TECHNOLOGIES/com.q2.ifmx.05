/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.ArrayList;

/**
 *
 * @author firman
 */
public class SMSObject {
    private String sms_url;
    private String tar_num;
    private String tar_mode;
    private String tar_msg;
    private String priority;
    private String sms_user;
    private String timestamp;
    
    ArrayList<MessageContent> listMessage;

    public ArrayList<MessageContent> getListMessage() {
        return listMessage;
    }

    public void setListMessage(ArrayList<MessageContent> listMessage) {
        this.listMessage = listMessage;
    }

    public String getSms_url() {
        return sms_url;
    }

    public void setSms_url(String sms_url) {
        this.sms_url = sms_url;
    }

    public String getTar_num() {
        return tar_num;
    }

    public void setTar_num(String tar_num) {
        this.tar_num = tar_num;
    }

    public String getTar_mode() {
        return tar_mode;
    }

    public void setTar_mode(String tar_mode) {
        this.tar_mode = tar_mode;
    }

    public String getTar_msg() {
        return tar_msg;
    }

    public void setTar_msg(String tar_msg) {
        this.tar_msg = tar_msg;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSms_user() {
        return sms_user;
    }

    public void setSms_user(String sms_user) {
        this.sms_user = sms_user;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
}
