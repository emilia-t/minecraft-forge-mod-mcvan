package com.myacghome.mcvan.util;
import java.util.Random;

public class CreateRandomNumber{
    public static int CreateRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }
}