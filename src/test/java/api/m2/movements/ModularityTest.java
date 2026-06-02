package api.m2.movements;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void detectModules() {
        var modules = ApplicationModules.of(MovementsApplication.class);
        modules.forEach(module -> System.out.println(module.toString()));
    }
}
