package org.cuberact.storage;

import org.junit.jupiter.api.*;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class ResourceTest {

    @Test
    public void path_FullName_Name_Extension() {
        Resource resource = new Resource(null, Storage.normalizePath("/dir/file.txt"));
        Assertions.assertEquals("/dir/file.txt", resource.getPath());
        Assertions.assertEquals("file.txt", resource.getFullName());
        Assertions.assertEquals("file", resource.getName());
        Assertions.assertEquals("txt", resource.getExtension());
    }
}
