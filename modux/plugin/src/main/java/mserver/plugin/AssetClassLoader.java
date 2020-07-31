package mserver.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.function.Supplier;

public class AssetClassLoader extends URLClassLoader {
    private final String name;
    private final Supplier<URLClassLoader> supplier;

    public AssetClassLoader(String name, Supplier<URLClassLoader> supplier, ClassLoader parent) {
        super(new URL[]{}, parent);
        this.name = name;
        this.supplier = supplier;
    }

    @Override
    public URL findResource(String name) {
        URL resource = supplier.get().findResource(name);
        System.out.println(name + "   " + resource);
        return resource == null ? super.findResource(name) : resource;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> resource = supplier.get().findResources(name);
        return resource == null ? super.findResources(name) : resource;
    }

    @Override
    public String toString() {
        return "AssetClassLoader{" +
                "name='" + name + '\'' +
                '}';
    }
}
