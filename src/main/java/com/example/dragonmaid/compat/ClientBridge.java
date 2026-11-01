package com.example.dragonmaid.compat;

/** Reflection bridge to the client-only handlers so the (P1) network layer compiles before P3 restores them. */
public final class ClientBridge {
    private ClientBridge() { }

    public static void call(String className, String methodName, Object argument) {
        try {
            Class<?> type = Class.forName(className);
            for (java.lang.reflect.Method method : type.getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].isInstance(argument)) {
                    method.invoke(null, argument);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Client renderer layer not restored yet (task-4): silently skip the client-only side effect.
        }
    }
}
