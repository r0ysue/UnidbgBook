package com.dta.r0dbg.bakend;

public class BackendException extends RuntimeException{
    public BackendException(){

    }

    public BackendException(String message){
        super(message);
    }
}
