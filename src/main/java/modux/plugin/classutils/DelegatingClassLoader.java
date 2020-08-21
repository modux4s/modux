/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package modux.plugin.classutils;

public class DelegatingClassLoader extends ClassLoader {

    private final ClassLoader buildLoader;

    public DelegatingClassLoader(ClassLoader commonLoader, ClassLoader buildLoader) {
        super(commonLoader);
        this.buildLoader = buildLoader;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.contains("modux.shared")) {
            return buildLoader.loadClass(name);
        } else {
            return super.loadClass(name, resolve);
        }
    }
/*

    @Override
    public URL getResource(String name) {
        URL resource = resourceCL.get().getResource(name);

        return resource == null ?  super.getResource(name): resource;
    }

    @Override
    protected URL findResource(String name) {
        URL resource = resourceCL.get().findResource(name);
        return resource ==null ? super.findResource(name) : resource;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> resource = resourceCL.get().findResources(name);
        return resource ==null ? super.findResources(name) : resource;
    }
*/

    @Override
    public String toString() {
        return "DelegatingClassLoader, using parent: " + getParent();
    }
}
