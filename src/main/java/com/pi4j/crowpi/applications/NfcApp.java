package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.NfcComponent;
import com.pi4j.crowpi.components.exceptions.NfcException;

public class NfcApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var nfc = new NfcComponent(pi4j);

        while (true) {
            final boolean isPresent = nfc.isCardPresent();
            System.out.println("Card present: " + isPresent);

            if (isPresent) {
                try {
                    System.out.println("Card serial: " + nfc.readCardSerial());
                } catch (NfcException e) {
                    e.printStackTrace();
                }
            }

            sleep(1000);
        }
    }
}
