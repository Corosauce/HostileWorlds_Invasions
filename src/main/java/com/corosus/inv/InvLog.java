package com.corosus.inv;

import com.corosus.inv.config.ConfigInvasion;

public class InvLog {

    /**
     * For seldom used but important things to print out in production
     *
     * @param string
     */
    public static void log(String string) {
        if (ConfigInvasion.useLoggingLog) {
            System.out.println(string);
        }
    }

    /**
     * For logging warnings/errors
     *
     * @param string
     */
    public static void err(String string) {
        if (ConfigInvasion.useLoggingError) {
            System.out.println(string);
        }
    }

    /**
     * For debugging things
     *
     * @param string
     */
    public static void dbg(String string) {
        if (ConfigInvasion.useLoggingDebug) {
            System.out.println(string);
        }
    }

}
