package dev.buildcli.core.constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DependencyConstants {
    public static final Map<String, List<String>> DEPENDENCIES = new HashMap<>();

    static {
        DEPENDENCIES.put("spring-starter", List.of("org.springframework.boot:spring-boot-starter","org.springframework.boot:spring-boot-starter-test"));
        DEPENDENCIES.put("spring-web", List.of("org.springframework.boot:spring-boot-starter-web"));
        DEPENDENCIES.put("spring-security", List.of("org.springframework.boot:spring-boot-starter-security","org.springframework.security:spring-security-test"));
        DEPENDENCIES.put("spring-security-oauth2", List.of("org.springframework.boot:spring-boot-starter-oauth2-authorization-server","org.springframework.boot:spring-boot-starter-oauth2-client","org.springframework.boot:spring-boot-starter-oauth2-resource-server"));
        DEPENDENCIES.put("spring-data-jpa", List.of("org.springframework.boot:spring-boot-starter-data-jpa"));
        DEPENDENCIES.put("spring-data-jdbc", List.of("org.springframework.boot:spring-boot-starter-data-jdbc"));
        DEPENDENCIES.put("spring-data-elasticsearch", List.of("org.springframework.boot:spring-boot-starter-data-elasticsearch"));
        DEPENDENCIES.put("spring-data-mongodb", List.of("org.springframework.boot:spring-boot-starter-data-mongodb"));
        DEPENDENCIES.put("spring-data-redis", List.of("org.springframework.boot:spring-boot-starter-data-redis"));
        DEPENDENCIES.put("spring-data-redis-reactive", List.of("org.springframework.boot:spring-boot-starter-data-redis-reactive"));
        DEPENDENCIES.put("spring-actuator", List.of("org.springframework.boot:spring-boot-starter-actuator"));
        DEPENDENCIES.put("spring-batch", List.of("org.springframework.boot:spring-boot-starter-batch"));
        DEPENDENCIES.put("spring-mail", List.of("org.springframework.boot:spring-boot-starter-mail"));
        DEPENDENCIES.put("spring-devtools", List.of("org.springframework.boot:spring-boot-devtools"));
        DEPENDENCIES.put("lombok", List.of("org.projectlombok:lombok"));
    }
}
