package com.dta.lesson31;

import java.io.UnsupportedEncodingException;

public class Encrypt {
    public static void main(String[] args) {
        System.out.println(a(new byte[]{46, 1, -100, -4, -87}, 168));
    }
    public static String a(byte[] bArr, int i) {
        try {
            return new String(a(bArr, (long) i), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(new byte[0]);
        }
    }

    public static byte[] a(byte[] bArr, long j) {
        for (int i = 0; ((long) i) < j; i++) {
            for (int i2 = 0; i2 < bArr.length; i2++) {
                bArr[i2] = (byte) (((bArr[i2] >> 4) & 15) + ((bArr[i2] & 15) << 4));
            }
            for (int length = bArr.length - 1; length >= 0; length--) {
                if (length != 0) {
                    bArr[length] = (byte) (bArr[length] ^ bArr[length - 1]);
                } else {
                    bArr[length] = (byte) (bArr[length] ^ bArr[bArr.length - 1]);
                }
                bArr[length] = (byte) (bArr[length] ^ 150);
            }
            for (int length2 = bArr.length - 1; length2 >= 0; length2--) {
                if (length2 != 0) {
                    bArr[length2] = (byte) (bArr[length2] - bArr[length2 - 1]);
                } else {
                    bArr[length2] = (byte) (bArr[length2] - bArr[bArr.length - 1]);
                }
                bArr[length2] = (byte) (bArr[length2] - 58);
            }
        }
        return bArr;
    }
}
