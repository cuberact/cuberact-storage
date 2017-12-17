# cuberact-resources
FileSystem abstraction  

### Goals

- simple and unified access to files in zip/jar file or directory
- deferred write to file - usefull in event system

## Examples

##### Basic example
```java
Storage storage = new Storage("path/to/directory/or/zip-file");
Resource resource = storage.getResource("first_file");
resource.write("hello", false);
resource.write(" world", true);

//and somewhere 
Resource resource = storage.findResource("*file");
String helloWorld = resource.readToString();
```

##### Read all png from classpath (directories or jars)
```java
URLClassLoader CLASSLOADER = (URLClassLoader) ClassLoader.getSystemClassLoader();
URL[] urls = CLASSLOADER.getURLs();
for (URL url : urls) {
    Storage storage = new Storage(url.getPath());
    List<Resource> pngResources = storage.findResources("**.png");
    pngResources.forEach(resource -> System.out.println(resource.getUri()));
}
```


##### Deferred write (usefull for write files from user events - mouse move, mouse up, etc.)
```java
Storage storage = new Storage("path/to/directory/or/zip-file");
Resource resource = storage.getResource("test_file");
for (int i = 0; i < 100; i++) {
    resource.writeDeferred("content_" + i);
}
String value = resource.readToString(); //read immediately execute deferred writes - only last one 
System.out.println(value); // -> content_99
```

## License

__cuberact-json__ is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2017 Michal Nikodim

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```