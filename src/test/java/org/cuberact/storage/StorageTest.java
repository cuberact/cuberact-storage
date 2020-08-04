package org.cuberact.storage;

import java.io.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class StorageTest {

    @Test
    public void existsWriteRead_Directory() throws IOException {
        existsWriteReadTest("junit_test_dir1");
    }

    @Test
    public void existsWriteRead_Zip() throws IOException {
        existsWriteReadTest("junit_test1.zip");
    }

    @Test
    public void findResources_Directory() {
        findResourcesTest("junit_test_dir2");
    }

    @Test
    public void findResources_Zip() {
        findResourcesTest("junit_test2.zip");
    }

    private void findResourcesTest(String path) {
        Storage storage = new Storage(path);
        assertFalse(storage.exists());
        try {
            storage.getResource("john.txt").write("content", false);
            storage.getResource("bob.txt").write("content", false);
            storage.getResource("alice.txt").write("content", false);
            storage.getResource("first/john.txt").write("content", false);
            storage.getResource("first/bob.txt").write("content", false);
            storage.getResource("first/alice.txt").write("content", false);
            storage.getResource("second/john.txt").write("content", false);
            storage.getResource("second/bob.txt").write("content", false);
            storage.getResource("second/alice.txt").write("content", false);

            assertEquals(1, storage.findResources("john.txt").size());  //john.txt
            assertEquals(2, storage.findResources("**/john.txt").size()); //first/john.txt, second/john.txt
            assertEquals(3, storage.findResources("**john.txt").size()); //john.txt, first/john.txt, second/john.txt
            assertEquals(6, storage.findResources("**o*.txt").size()); //john.txt, first/john.txt, second/john.txt, bob.txt, first/bob.txt, second/bob.txt

            assertNotNull(storage.findResource("alice.txt"));

            try {
                storage.findResource("**bob.txt");
                fail("expected more than one resource by glob **bob.txt");
            } catch (StorageException e) {
                //ok
            }
        } finally {
            storage.delete();
            assertFalse(storage.exists());
        }
    }

    private void existsWriteReadTest(String path) throws IOException {
        Storage storage = new Storage(path);
        assertFalse(storage.exists());
        try {
            Resource resource = storage.getResource("subdir/test.txt");
            assertFalse(resource.exists());
            resource.write("FIRST", false);
            assertTrue(resource.exists());

            resource.delete();
            assertFalse(resource.exists());

            resource.write("A", false);
            resource.write("B", true);
            resource.write("C", true);
            assertTrue(resource.exists());
            assertEquals("ABC", resource.readToString());
            assertEquals("ABC", new String(resource.readToBytes()));

            byte[] fromIs = resource.readFromInputStream(is -> {
                byte[] bytesFromStream = new byte[3];
                is.read(bytesFromStream);
                return bytesFromStream;
            });

            assertEquals("ABC", new String(fromIs));

            resource.write("DEF".getBytes(), true);
            resource.write(new ByteArrayInputStream("GHI".getBytes()), true);
            resource.write(new CharArrayReader("JKL".toCharArray()), true);
            assertEquals("ABCDEFGHIJKL", resource.readToString());

            resource.write("XYZ", false);
            assertTrue(resource.exists());
            assertEquals("XYZ", resource.readToString());

            assertTrue(storage.exists());
        } finally {
            storage.delete();
            assertFalse(storage.exists());
        }
    }
}
