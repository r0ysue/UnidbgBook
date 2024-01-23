package com.dta.lesson29;

import java.nio.charset.StandardCharsets;

public class a {
    private static final char[] a = {'i', '5', 'j', 'L', 'W', '7', 'S', '0', 'G', 'X', '6', 'u', 'f', '1', 'c', 'v', '3', 'n', 'y', '4', 'q', '8', 'e', 's', '2', 'Q', '+', 'b', 'd', 'k', 'Y', 'g', 'K', 'O', 'I', 'T', '/', 't', 'A', 'x', 'U', 'r', 'F', 'l', 'V', 'P', 'z', 'h', 'm', 'o', 'w', '9', 'B', 'H', 'C', 'M', 'D', 'p', 'E', 'a', 'J', 'R', 'Z', 'N'};

    public static String a(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= bArr.length - 1; i += 3) {
            byte[] bArr2 = new byte[4];
            byte b = 0;
            for (int i2 = 0; i2 <= 2; i2++) {
                if (i + i2 <= bArr.length - 1) {
                    bArr2[i2] = (byte) (b | ((bArr[i + i2] & 255) >>> ((i2 * 2) + 2)));
                    b = (byte) ((((bArr[i + i2] & 255) << (((2 - i2) * 2) + 2)) & 255) >>> 2);
                } else {
                    bArr2[i2] = b;
                    b = 64;
                }
            }
            bArr2[3] = b;
            for (int i3 = 0; i3 <= 3; i3++) {
                if (bArr2[i3] <= 63) {
                    sb.append(a[bArr2[i3]]);
                } else {
                    sb.append('=');
                }
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        //String s = new String(a);
        String a = a("123".getBytes(StandardCharsets.UTF_8));
        System.out.println(a);
    }
}