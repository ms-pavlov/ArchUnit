package ru.proitr.tk.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@Epic("АРМ ТК")
@Feature("Архитектура приложения Backend")
public class ArchitectureTest {

    private static final String CORE_PACKAGE = "ru.proitr.tk";
    private static final String CONTROLLER_PACKAGE = "ru.proitr.tk.controller";
    private static final String LISTENER_PACKAGE = "ru.proitr.tk.listener";
    private static final String SERVICE_PACKAGE = "ru.proitr.tk.service";
    private static final String CLIENT_PACKAGE = "ru.proitr.tk.core.client";
    private static final String REPOSITORY_PACKAGE = "ru.proitr.tk.repository";
    private static final String PRODUCER_PACKAGE = "ru.proitr.tk.producer";
    private static final String MAPPER_PACKAGE = "ru.proitr.tk.mapper";
    private static final String JOOQ_GEN_PACKAGE = "ru.proitr.tk.generated";
    private static final String DAO_PACKAGE = "ru.proitr.tk.generated.tables.daos";
    private static final String CONDITION_BUILDER_PACKAGE = "ru.proitr.tk.repository.conditionbuilder";

    private ClassFileImporter getBaseFileImporter() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeJars())
                .withImportOption(new ImportOption.DoNotIncludeArchives())
                .withImportOption(new ImportOption.DoNotIncludeTests());
    }

    @Test
    @DisplayName("Между пакетами отсутствуют циклические зависимости")
    void no_cycles_by_method_calls_between_slices() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching("(" + CORE_PACKAGE + ").(*)..")
                .namingSlices("$1 of $2")
                .should().beFreeOfCycles();

        rule.check(getBaseFileImporter()
                .importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Соблюдается слоеная архитектура приложения")
    void layered_architecture() {
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()

                .layer("Inbound")
                .definedBy(
                        CONTROLLER_PACKAGE + "..",
                        LISTENER_PACKAGE + ".."
                )

                .layer("Service")
                .definedBy(
                        SERVICE_PACKAGE + "..",
                        CLIENT_PACKAGE + ".."
                )

                .layer("Mapper")
                .definedBy(MAPPER_PACKAGE + "..")

                .layer("Repository")
                .definedBy(
                        REPOSITORY_PACKAGE + ".."
                )

                .layer("Producer")
                .definedBy(
                        PRODUCER_PACKAGE + ".."
                )

                .layer("JooqInfrastructure")
                .definedBy(
                        JOOQ_GEN_PACKAGE + ".."
                )

                .whereLayer("Inbound").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Inbound")

                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Producer").mayOnlyBeAccessedByLayers("Service")

                .whereLayer("JooqInfrastructure").mayOnlyBeAccessedByLayers("Repository");

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Только классы репозитории могут зависеть от классов ДАО")
    void no_classes_depend_on_dao() {
        ArchRule rule = noClasses().that()
                .resideOutsideOfPackage(REPOSITORY_PACKAGE + "..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(DAO_PACKAGE + "..");

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }


}
