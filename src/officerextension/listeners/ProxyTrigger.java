package officerextension.listeners;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public abstract class ProxyTrigger implements Triggerable {

    /** [proxy] implements an obfuscated interface
     *  in com.fs.starfarer.ui. Need a proxy in order to avoid using the interface name, which
     *  is obfuscated. */
    private final Object proxy;

    /** Creates a proxy for the "trigger-like" interface [interfc] in the obfuscated game code.
     *  Said interface should have a single method named [methodName].
     *  The method should have return type [void].
     *  When the proxy's method gets called, re-routes the method call to [this.trigger]. */
    public ProxyTrigger(Class<?> interfc, final String methodName) {
        proxy = java.lang.reflect.Proxy.newProxyInstance(
                interfc.getClassLoader(),
                new Class<?>[] {interfc},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws UnsupportedOperationException {
                        if (method.getName().equals("equals") && args.length == 1) {
                            return proxy == args[0];
                        }
                        if (method.getName().equals("hashCode") && args.length == 0) {
                            return System.identityHashCode(proxy);
                        }
                        if (method.getName().equals("toString") && args.length == 0) {
                            return proxy.getClass() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                        }
                        if (method.getName().equals(methodName)) {
                            trigger(args);
                            return null;
                        }
                        throw new UnsupportedOperationException("Methods other than " + methodName + " are not supported.");
                    }
                }
        );
    }

    public Object getProxy() {
        return proxy;
    }

    @Override
    public abstract void trigger(Object... args);
}
