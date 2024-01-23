package com.dta.r0dbg.linux.socket;

public interface ISocket {
    int AF_LOCAL = 1;

    //cmd
    int F_SETFL = 4;

    int SOCK_STREAM = 1;
    int SOCK_DGRAM = 2;
}
