package com.dta.r0dbg.linux;

public class SysCallException extends RuntimeException{
    public SysCallException(String message) {
        super(message);
    }
}
