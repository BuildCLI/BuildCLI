package dev.buildcli;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the ports-and-adapters boundaries of the RFC:
 * domain <- ports <- application <- infrastructure/cli/eval, and third-party frameworks only in infrastructure.
 */
@AnalyzeClasses(packages = "dev.buildcli", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_DEPENDS_ON_NOTHING_INTERNAL = noClasses().that().resideInAPackage("dev.buildcli.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.buildcli.ports..", "dev.buildcli.application..", "dev.buildcli.infrastructure..",
                    "dev.buildcli.cli..");

    @ArchTest
    static final ArchRule PORTS_ONLY_KNOW_THE_DOMAIN = noClasses().that().resideInAPackage("dev.buildcli.ports..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.buildcli.application..", "dev.buildcli.infrastructure..", "dev.buildcli.cli..");

    @ArchTest
    static final ArchRule APPLICATION_ONLY_KNOWS_DOMAIN_AND_PORTS = noClasses().that().resideInAPackage("dev.buildcli.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.buildcli.infrastructure..", "dev.buildcli.cli..");

    @ArchTest
    static final ArchRule NOTHING_DEPENDS_ON_THE_ENTRY_POINTS = noClasses()
            .that().resideOutsideOfPackages("dev.buildcli.cli..")
            .should().dependOnClassesThat().resideInAnyPackage("dev.buildcli.cli..");

    @ArchTest
    static final ArchRule FRAMEWORKS_STAY_IN_INFRASTRUCTURE = noClasses()
            .that().resideOutsideOfPackage("dev.buildcli.infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.langchain4j..", "dev.tamboui..", "java.sql..", "org.sqlite..", "com.fasterxml.jackson..")
            .as("LangChain4j, TamboUI, JDBC and Jackson must stay in the infrastructure package");
}
