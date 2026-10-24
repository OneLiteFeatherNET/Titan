package net.onelitefeather.titan.core.architecture.fixture.reflection;

import java.lang.reflect.Field;

/** Rule fixture: reads a private field through reflection, which the no-reflection rule must report. */
public final class BypassesAccessModifier {

    private final String secret = "value";

    public String read() throws ReflectiveOperationException {
        Field field = BypassesAccessModifier.class.getDeclaredField("secret");
        field.setAccessible(true);
        return (String) field.get(this);
    }
}
