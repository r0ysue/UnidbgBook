package com.dta.lesson31;

public class Decode {
    public static void main(String[] args) {
        String s = "ek`fz@q2^x/t^fn0mF^6/^rb`qanqntfg^E`hq|";
        for (char c:s.toCharArray()){
            System.out.printf(""+(char)(c+1));
        }
    }
}
