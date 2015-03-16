package org.sharedhealth.mci.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.exception.HidGenerationException;

import static java.lang.String.valueOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.utils.DefaultHidGenerator.RANDOM_BITS_SIZE;
import static org.sharedhealth.mci.utils.DefaultHidGenerator.WORKER_ID_BITS_SIZE;

public class DefaultHidGeneratorTest {

    @Mock
    private MCIProperties properties;
    private DefaultHidGenerator hidGenerator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        hidGenerator = new DefaultHidGenerator(properties);
        when(properties.getWorkerId()).thenReturn(valueOf(hidGenerator.getMaxWorkerId()));
    }

    @Test
    public void shouldGenerate10DigitHidWithChecksum() {
        String hidWithCheckSum = hidGenerator.generate();
        assertNotNull(hidWithCheckSum);
        assertEquals(11, hidWithCheckSum.length());

        String checksum = hidWithCheckSum.substring(hidWithCheckSum.length() - 1);
        assertEquals(1, checksum.length());

        String hid = hidWithCheckSum.substring(0, hidWithCheckSum.length() - 1);
        assertEquals(10, hid.length());

        assertEquals((int) Integer.valueOf(checksum), hidGenerator.generateChecksum(Long.valueOf(hid)));
    }

    @Test
    public void shouldGenerateHidWithValidBits() {
        String hidWithCheckSum = hidGenerator.generate();
        assertNotNull(hidWithCheckSum);
        assertEquals(11, hidWithCheckSum.length());

        String hid = hidWithCheckSum.substring(0, hidWithCheckSum.length() - 1);
        assertNotNull(hid);
        assertEquals(10, hid.length());

        hid = valueOf(Long.valueOf(hid) - hidGenerator.getMin10DigitNumber());
        assertNotNull(hid);

        String hidBinary = Long.toBinaryString(Long.valueOf(hid));
        assertNotNull(hidBinary);
        int hidBinaryLength = hidBinary.length();

        String timestampBits = hidBinary.substring(0, hidBinaryLength - WORKER_ID_BITS_SIZE - RANDOM_BITS_SIZE);
        assertNotNull(timestampBits);

        long currentTime = hidGenerator.getCurrentTimeMins();
        long epochTime = hidGenerator.getEpochTimeMins();
        long time = Long.valueOf(timestampBits, 2);
        assertTrue(time <= (currentTime - epochTime));

        String workerIdAndRandomBits = hidBinary.substring(timestampBits.length());
        assertNotNull(workerIdAndRandomBits);

        String workerIdBits = workerIdAndRandomBits.substring(0, WORKER_ID_BITS_SIZE);
        assertNotNull(workerIdBits);
        assertEquals(hidGenerator.getMaxWorkerId(), (long) Long.valueOf(workerIdBits, 2));

        String randomBits = workerIdAndRandomBits.substring(workerIdBits.length());
        assertNotNull(randomBits);

        assertEquals(hidBinary, timestampBits + workerIdBits + randomBits);
    }

    @Test
    public void shouldValidateWhether10DigitNumber() {
        assertFalse(hidGenerator.is10DigitNumber(1));
        assertFalse(hidGenerator.is10DigitNumber(12345678901L));
        assertTrue(hidGenerator.is10DigitNumber(1234567890L));
    }

    @Test
    public void shouldGenerateChecksum() {
        assertEquals(6, hidGenerator.generateChecksum(12345));
        assertEquals(3, hidGenerator.generateChecksum(123456));
        assertEquals(1, hidGenerator.generateChecksum(1234567));
    }

    @Test
    public void shouldGenerateWorkerId() {
        long maxWorkerId = hidGenerator.getMaxWorkerId();
        when(properties.getWorkerId()).thenReturn(valueOf(maxWorkerId - 2));
        assertTrue(hidGenerator.getWorkerId() > 0);
    }

    @Test(expected = HidGenerationException.class)
    public void shouldThrowExceptionWhenGeneratedWorkerIdIsNegative() {
        when(properties.getWorkerId()).thenReturn(valueOf(-1));
        hidGenerator.getWorkerId();
    }

    @Test(expected = HidGenerationException.class)
    public void shouldThrowExceptionWhenGeneratedWorkerIdExceedsMaxLimit() {
        long maxWorkerId = hidGenerator.getMaxWorkerId();
        when(properties.getWorkerId()).thenReturn(valueOf(maxWorkerId + 1));
        hidGenerator.getWorkerId();
    }

    @Test
    public void shouldGenerateRandomNumber() {
        for (int i = 0; i < 5; i++) {
            assertTrue(hidGenerator.generateRandomNumber() <= hidGenerator.getMaxRandomNumber());
        }
    }
}