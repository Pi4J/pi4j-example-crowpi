package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.rfid.RfidCardType;

public class RfidUnsupportedCardException extends RfidException {
    private final RfidCardType cardType;

    public RfidUnsupportedCardException(RfidCardType cardType) {
        super("Unsupported card type: " + cardType);
        this.cardType = cardType;
    }

    public RfidCardType getCardType() {
        return cardType;
    }
}
