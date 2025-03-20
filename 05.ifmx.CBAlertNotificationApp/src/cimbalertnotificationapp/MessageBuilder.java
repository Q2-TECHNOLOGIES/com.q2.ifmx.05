/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbalertnotificationapp;

import java.util.ArrayList;
import java.util.HashMap;
import model.EmailObject;
import model.MessageContent;
import model.SMSObject;

/**
 *
 * @author firman
 */
public class MessageBuilder { 
    
    public String smsMessageBuilder(ArrayList<MessageContent> list){
        MessageContent obj = list.get(0);
        String msg = obj.getTrx_date()+", "+
                obj.getAlert_id()+", "+
                obj.getParty_id()+", "+
                obj.getCust_name()+", "+
                obj.getTrx_type()+", "+
                obj.getTrx_amt()+", "+
                obj.getResponse()+", "+
                obj.getScore()+", "+
                obj.getChannel_type();
        return msg;
    }
    
    public String emailMessageBuilder(ArrayList<MessageContent> list){
        String mail_body = "<p style='font-family:Arial;'>There is a new RCM alert with the details below:</p>";
        mail_body += "<table border='0' style='font-family:calibri;' width='100%'>";
        mail_body += "<tr style='background-color:#424851'>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Transaction Date & Time</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>CWI Alert ID</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Customer Party ID</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Customer Name</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Transaction Type</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Amount</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Hold/No Hold</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Score</td>"+
                        "<td style='font-weight:bold; color:#ffffff' align='center'>Channel Type</td>"+
                    "</tr>";
        for(int i=0; i<list.size(); i++){
            MessageContent obj = list.get(i);
            mail_body += "<tr style='background-color:#f2f2f2'>"+
                        "<td>"+obj.getTrx_date()+"</td>"+
                        "<td>"+obj.getAlert_id()+"</td>"+
                        "<td >"+obj.getParty_id()+"</td>"+
                        "<td>"+obj.getCust_name()+"</td>"+
                        "<td>"+obj.getTrx_type()+"</td>"+
                        "<td>"+obj.getTrx_amt()+"</td>"+
                        "<td>"+obj.getResponse()+"</td>"+
                        "<td>"+obj.getScore()+"</td>"+
                        "<td>"+obj.getChannel_type()+"</td>"+
                    "</tr>";
        }
        mail_body += "</table><br>";
        
        mail_body += "<p>This email is generated automatically by Actimize Anti Fraud System. Please do not reply to this email.</p>";
        
        return mail_body;
    }
}
