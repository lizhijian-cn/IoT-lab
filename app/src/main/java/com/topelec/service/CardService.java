package com.topelec.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CardService {
    final static Map<String, Integer> BalanceMap = new ConcurrentHashMap<>();

    final static RuntimeException ex = new RuntimeException("卡片未注册");

    public boolean checkRegisterState(String cardNo) {
        return BalanceMap.containsKey(cardNo);
    }

    public void register(String cardNo) {
        BalanceMap.put(cardNo, 0);
    }

    public void unregister(String cardNo) {
        BalanceMap.remove(cardNo);
    }

    public int getBalance(String cardNo) {
        if (!checkRegisterState(cardNo)) {
            throw ex;
        } else {
            return BalanceMap.get(cardNo);
        }
    }

    public int consume(String cardNo, int cost) {
        if (!checkRegisterState(cardNo)) {
            throw ex;
        }
        int balance = BalanceMap.get(cardNo) - cost;
        BalanceMap.put(cardNo, balance);
        return balance;
    }

    public int recharge(String cardNo, int cost) {
        if (!BalanceMap.containsKey(cardNo)) {
            throw ex;
        }
        int balance = BalanceMap.get(cardNo) + cost;
        BalanceMap.put(cardNo, balance);
        return balance;
    }
}