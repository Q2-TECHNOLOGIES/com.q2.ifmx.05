/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class fileNameChecker {
    

    public static Boolean isValidDate(String date) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            df.setLenient(false);
            df.parse(date);
            return true;
        }
        catch (ParseException e) {
            return false;
        }
    }

    public static boolean isValidFileName(String fileName) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("address-based-");
        list.add("domestic-transfer-");
        list.add("enrollment-");
        list.add("info-changes-");
        list.add("internal-transfer-");
        list.add("login-");
        list.add("p2p-");
        list.add("wire-");
        Boolean status = false;
        for (String temp : list) {
            if (!temp.equals(fileName)) continue;
            status = true;
            break;
        }
        return status;
    }

    public static Boolean checkingFileName(String filename) {
        Pattern pattern = Pattern.compile("(.*)(\\d{4}\\d{2}\\d{2})(.xml)", 40);
        Matcher messages = pattern.matcher(filename);
        Boolean status = false;
        if (messages.matches()) {
            messages.matches();
            if (fileNameChecker.isValidFileName(messages.group(1)) && fileNameChecker.isValidDate(messages.group(2)).booleanValue()) {
                status = true;
            }
        }
        return status;
    }
}
