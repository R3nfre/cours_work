package com.example.course_work;

import com.example.course_work.encryption.NTRUEncrypt;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) throws Exception {
        NTRUEncrypt NTRUEncrypt = new NTRUEncrypt();
        BigInteger num = NTRUEncrypt.genPrime(128);
        if(num.isProbablePrime(50))
        {
            System.out.println("num is prime : " + num);
        }
        else {
            System.out.println("num is not prime");
        }
    }
}
