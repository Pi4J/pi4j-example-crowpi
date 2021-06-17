package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HumiTempComponentTest extends ComponentTest {
    protected HumiTempComponent humiTempComponent;

    @BeforeEach
    void setUp() {
        this.humiTempComponent = new HumiTempComponent();
    }

    @Test
    void testReadFileAndCheckValue() {
        // given
        final String humiPath = "src/test/java/com/pi4j/crowpi/resources/HumiTestFile";
        final String tempPath = "src/test/java/com/pi4j/crowpi/resources/TempTestFile";

        // when
        final var customSensor = new HumiTempComponent(humiPath, tempPath, 10);
        while (customSensor.getHumidity() == 0 || customSensor.getTemperature() == 0) {
            // wait until poller has read and processed both files
            Thread.onSpinWait();
        }

        // then
        assertEquals(43.4, customSensor.getHumidity());
        assertEquals(26.1, customSensor.getTemperature());
    }
}
