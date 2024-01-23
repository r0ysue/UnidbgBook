package com.dta.r0dbg.linux.socket;

import java.io.IOException;

public interface UdpHandler {
    void handle(byte[] request) throws IOException;
}
