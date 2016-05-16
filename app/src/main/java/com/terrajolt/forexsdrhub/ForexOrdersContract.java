package com.terrajolt.forexsdrhub;

import android.provider.BaseColumns;

/**
 * Created by William on 3/26/2016.
 */
public final class ForexOrdersContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public ForexOrdersContract() {}

        /* Inner class that defines the table contents */
        public static abstract class OrderEntry implements BaseColumns {
            public static final String TABLE_NAME = "orderbook";
            public static final String COLUMN_NAME_ENTRY_ID = "entryid";
            public static final String COLUMN_NAME_EXEC_TIME = "executiontime";
            public static final String COLUMN_NAME_OPTION_TYPE = "optiontype";
            public static final String COLUMN_NAME_CURRENCY1 = "currency1";
            public static final String COLUMN_NAME_CURRENCY2 = "currency2";
            public static final String COLUMN_NAME_NOTIONAL1 = "notionalamount1";
            public static final String COLUMN_NAME_NOTIONAL2 = "notionalamount2";
            public static final String COLUMN_NAME_STRIKE_PRICE = "strikeprice";
            public static final String COLUMN_NAME_OPTION_CURR = "optioncurrency";
            public static final String COLUMN_NAME_PREMIUM = "premium";
            public static final String COLUMN_NAME_EXPIRATION = "expirationdate";

        }
}
