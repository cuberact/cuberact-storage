package org.cuberact.storage;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class ResourceTest {

    @Test
    public void path_FullName_Name_Extension() {
        Resource resource = new Resource(null, Storage.normalizePath("/dir/file.txt"));
        Assert.assertEquals("dir/file.txt", resource.getPath());
        Assert.assertEquals("file.txt", resource.getFullName());
        Assert.assertEquals("file", resource.getName());
        Assert.assertEquals("txt", resource.getExtension());
    }
}
