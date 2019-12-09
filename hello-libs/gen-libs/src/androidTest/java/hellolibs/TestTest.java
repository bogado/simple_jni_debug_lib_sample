package hellolibs;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestTest {
    @Test
    public void simple() {
        com.example.Test a = new com.example.Test();
        assertEquals(a.stringFromJNI(), "Good bye from JNI LIBS!");
    }
}