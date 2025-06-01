package my.code.transactionproxy.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashUtilTest {

    @Test
    void shouldGenerateSha256HashForSimpleObject() {
        // Given
        TestObject obj = new TestObject("test", 123);

        // When
        String hash1 = HashUtil.sha256(obj);
        String hash2 = HashUtil.sha256(obj);

        // Then
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertTrue(hash1.length() > 0);
    }

    @Test
    void shouldGenerateDifferentHashesForDifferentObjects() {
        // Given
        TestObject obj1 = new TestObject("test1", 123);
        TestObject obj2 = new TestObject("test2", 456);

        // When
        String hash1 = HashUtil.sha256(obj1);
        String hash2 = HashUtil.sha256(obj2);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldHandleLocalDateCorrectly() {
        // Given
        TestObjectWithDate obj1 = new TestObjectWithDate("test", LocalDate.of(2023, 1, 1));
        TestObjectWithDate obj2 = new TestObjectWithDate("test", LocalDate.of(2023, 1, 1));

        // When
        String hash1 = HashUtil.sha256(obj1);
        String hash2 = HashUtil.sha256(obj2);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldThrowRuntimeExceptionForNonSerializableObject() {
        // Given
        Object nonSerializable = new Object() {
            public Object getSelf() {
                return this;
            } // Creates circular reference
        };

        // When & Then
        assertThrows(RuntimeException.class, () -> HashUtil.sha256(nonSerializable));
    }

    static class TestObject {
        public String name;
        public int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    static class TestObjectWithDate {
        public String name;
        public LocalDate date;

        public TestObjectWithDate(String name, LocalDate date) {
            this.name = name;
            this.date = date;
        }
    }
}