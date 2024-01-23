package com.dta.lesson53.controller;

import com.dta.lesson53.source.MainActivity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StringFromJNIController {

    private MainActivity mainActivity = new MainActivity();

    @GetMapping("/stringfromjni")
    public synchronized String stringFromJni(String name){
        return mainActivity.stringFromJNI() + name;
    }
}
