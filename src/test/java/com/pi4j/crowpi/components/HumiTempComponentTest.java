package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.components.exceptions.MeasurementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

        while (customSensor.getHumidity() == 0) {
            // wait until poller has read the file
        }

        // then
        assertEquals(43.4, customSensor.getHumidity());
        assertEquals( 26.1, customSensor.getTemperature());
    }
}
