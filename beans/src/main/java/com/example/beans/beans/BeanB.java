package com.example.beans.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeanB {

    @Autowired
    private BeanA beana;

    public BeanA getBeana() {
        return beana;
    }

    public BeanB() {
        System.out.println("B's no args construtor");
        System.out.println("Is a null in b's constructor???" + (beana == null));
    }

    public void printMe(){
        System.out.println("Is a null in b's print me???" + (beana == null));
        System.out.println("print me is called in Bean B");
    }
}
