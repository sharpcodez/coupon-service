package pl.empik.task.empikservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "pl.empik.task.empikservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "javax..",
                            "java.sql..", "tools.jackson..", "com.fasterxml..",
                            "..adapter..", "..infrastructure..");

    @ArchTest
    static final ArchRule inboundAdaptersDoNotDependOnOutbound =
            noClasses().that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..");

    @ArchTest
    static final ArchRule outboundAdaptersDoNotDependOnInbound =
            noClasses().that().resideInAPackage("..adapter.out..")
                    .should().dependOnClassesThat().resideInAnyPackage("..adapter.in..");

    @ArchTest
    static final ArchRule persistenceDoesNotDependOnGeoip =
            noClasses().that().resideInAPackage("..adapter.out.persistence..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.out.geoip..");

    @ArchTest
    static final ArchRule geoipDoesNotDependOnPersistence =
            noClasses().that().resideInAPackage("..adapter.out.geoip..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..");

    @ArchTest
    static final ArchRule transactionalMethodsOnlyInPersistenceAdapter =
            noMethods().that().areDeclaredInClassesThat()
                    .resideOutsideOfPackage("..adapter.out.persistence..")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional");

    @ArchTest
    static final ArchRule transactionalClassesOnlyInPersistenceAdapter =
            noClasses().that().resideOutsideOfPackage("..adapter.out.persistence..")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional");
}
