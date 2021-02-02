package com.example.beans.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeanA {
    @Autowired
    private BeanB b;

    public BeanA() {
        System.out.println("A's no args construtor");
    }

    public void print(){
        System.out.println("bean a post construct is called");
        System.out.println("Is bean b is null in bean a 's post " + (b == null) + " bean a in current b(" + b.getBeana());
        b.printMe();
    }
}
