package com.theshmuz.app.util;

import com.theshmuz.app.ShmuzHelper;

public class ThreeTypes {

    public static final int SHMUZ = 1;
    public static final int PARSHA = 2;

    public static final String STR_SHMUZ = "shmuz";
    public static final String STR_PARSHA = "parsha";
    public static final String STR_ARTICLE = "article";

    public static String getString(int type) {
        switch(type){
            case SHMUZ:
                return STR_SHMUZ;
            case PARSHA:
                return STR_PARSHA;
        }
        return null;
    }

    public static int getInt(String type) {
        if(type.equals(STR_SHMUZ)) {
            return SHMUZ;
        }
        if(type.equals(STR_PARSHA)) {
            return PARSHA;
        }
        return 50;
    }

    public static boolean isSeries(String type) {
        return !type.equals(STR_SHMUZ) && !type.equals(STR_PARSHA);
    }
}
