package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.NfcComponent;
import com.pi4j.crowpi.components.exceptions.NfcException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;

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
                    for (int i = 0; i <= 0x3F; i++) {
                        final var contents = ByteHelpers.toString(nfc.readCard((byte) i));
                        System.out.println("Card contents at block [" + i + "]: " + contents);
                    }
                } catch (NfcException e) {
                    e.printStackTrace();
                } finally {
                    nfc.deauthenticate();
                }
            }

            sleep(10000);
        }
    }
}
