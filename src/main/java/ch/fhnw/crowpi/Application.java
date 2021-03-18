package ch.fhnw.crowpi;

import com.pi4j.context.Context;

public interface Application {
    void execute(Context pi4j);

    default String getName() {
        return this.getClass().getSimpleName();
    }

    default String getDescription() {
        final String classFqdn = this.getClass().getName();
        return "Runs application " + classFqdn;
    }
}
