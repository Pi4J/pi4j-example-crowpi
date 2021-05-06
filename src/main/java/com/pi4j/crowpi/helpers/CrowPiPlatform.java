package com.pi4j.crowpi.helpers;

import com.pi4j.plugin.raspberrypi.platform.RaspberryPiPlatform;

/**
 * Custom Raspberry Pi platform class to avoid Pi4J loading the platform providers which do not work out of the box.
 * To achieve this, the method `getProviders` gets overridden to return an empty list of platform providers.
 * FIXME: This can probably be removed once a solution for https://github.com/Pi4J/pi4j-v2/issues/17 exists
 */
public class CrowPiPlatform extends RaspberryPiPlatform {
    @Override
    protected String[] getProviders() {
        return new String[]{};
    }
}
