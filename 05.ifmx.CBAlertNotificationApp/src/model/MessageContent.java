/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author 4dm1n
 */
public class MessageContent {
    private String alert_id;
    private String party_id;
    private String cust_name;
    private String trx_type;
    private String trx_amt;
    private String trx_date;
    private String response;
    private String score;
    private String channel_type;

    public String getAlert_id() {
        return alert_id;
    }

    public void setAlert_id(String alert_id) {
        this.alert_id = alert_id;
    }

    public String getParty_id() {
        return party_id;
    }

    public void setParty_id(String party_id) {
        this.party_id = party_id;
    }

    public String getCust_name() {
        return cust_name;
    }

    public void setCust_name(String cust_name) {
        this.cust_name = cust_name;
    }

    public String getTrx_type() {
        return trx_type;
    }

    public void setTrx_type(String trx_type) {
        this.trx_type = trx_type;
    }

    public String getTrx_amt() {
        return trx_amt;
    }

    public void setTrx_amt(String trx_amt) {
        this.trx_amt = trx_amt;
    }

    public String getTrx_date() {
        return trx_date;
    }

    public void setTrx_date(String trx_date) {
        this.trx_date = trx_date;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getChannel_type() {
        return channel_type;
    }

    public void setChannel_type(String channel_type) {
        this.channel_type = channel_type;
    }
    
    
}
