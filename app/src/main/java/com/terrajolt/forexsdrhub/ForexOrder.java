package com.terrajolt.forexsdrhub;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by William on 3/19/2016.
 */
public class ForexOrder {

    /*
    Orders must be made aware of the dissemination ID, since that'll be the identifier for querying
    the database when we wish to remove orders that are reported as "CANCEL"
    */
    private int disseminationID;
    private float strikePrice;
    private float optionPremium;
    private String optionType;
    private Date optionExpirationDate;
    private long notionalAmount1;
    private long notionalAmount2;
    private String notionalCurrency1;
    private String notionalCurrency2;
    private String optionCurrency;
    private String executionTimestamp;

    public ForexOrder() {

    }

    public ForexOrder(List<String> orderParams) {
        //Here we assume a formatting of the list in proper order
    }

    public ForexOrder(int disID,String execTime,String notCur1,String notCur2,long notAm1,long notAm2,float strk,String opType,String opCur,float opPrem,String opExDat) {
        //Here, we don't
        disseminationID = disID;
        executionTimestamp = execTime;
        notionalCurrency1 = notCur1;
        notionalCurrency2 = notCur2;
        notionalAmount1 = notAm1;
        notionalAmount2 = notAm2;
        strikePrice = strk;
        optionType = opType;
        optionCurrency = opCur;
        optionPremium = opPrem;
        //SimpleDateFormat dateFormat1 = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
        try {
            optionExpirationDate = dateFormat2.parse(opExDat);
            optionExpirationDate.setHours(10);
        } catch (ParseException e) {
            Log.v("LOG_TAG",opExDat);
            Log.e("LOG_TAG",e.getMessage());
        }
    }

    @Override
    public String toString() {
        super.toString();
        String resultString = "";
        StringBuilder sb = new StringBuilder();
        if (optionType.equals("C-")) {
            sb.append("Call option, ");
            if (notionalCurrency1.equals("USD")) {
                sb.append(notionalCurrency1+"/"+notionalCurrency2+", ");
            } else {
                sb.append(notionalCurrency2+"/"+notionalCurrency1+", ");
            }
            sb.append("Base currency notional "+Long.toString(notionalAmount1)+", ");
            sb.append("Counter currency notional "+Long.toString(notionalAmount2)+", ");

            sb.append("Strike price "+Float.toString(strikePrice)+", ");
            sb.append("Premium "+Float.toString(optionPremium)+", ");

            sb.append("Expiration Date "+optionExpirationDate.toString());

        } else {
            sb.append("Put option, ");
            if (notionalCurrency1.equals("USD")) {
                sb.append(notionalCurrency2+"/"+notionalCurrency1+", ");
            } else {
                sb.append(notionalCurrency1+"/"+notionalCurrency2+", ");
            }
            sb.append("Base currency notional "+Long.toString(notionalAmount1)+", ");
            sb.append("Counter currency notional "+Long.toString(notionalAmount2)+", ");

            sb.append("Strike price "+Float.toString(strikePrice)+", ");
            sb.append("Premium "+Float.toString(optionPremium)+", ");
            sb.append("Expiration Date "+optionExpirationDate.toString());
        }
        resultString = sb.toString();

        return resultString;
    }

    public Date getOptionExpirationDate() {
        return optionExpirationDate;
    }

    public String getCurrencyPair() {
        return notionalCurrency1+"/"+notionalCurrency2;
    }

    public String getOptionType() {
        if (optionType.equals("C-")) {
            return "Call Option";
        } else {
            return "Put Option";
        }

    }

    public String getAmountPair() {
        return Long.toString(notionalAmount1)+"/"+Long.toString(notionalAmount2);
    }

    public float getStrikePrice() {
        return strikePrice;
    }

    public float getOptionPremium() {
        return optionPremium;
    }

    public String getExecutionTimestamp() {
        return executionTimestamp;
    }
}
