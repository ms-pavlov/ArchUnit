package org.example.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.example.EaistRequestContext;
import org.example.Secure;
import org.example.SecureMultiple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.example.generated.tables.daos.DAOImpl;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Epic("АРМ ТК")
@Feature("Архитектура приложения Backend")
public class JUnitSetyleArchitectureTest {

    private static final String CORE_PACKAGE = "ru.proitr";
    private static final String CONTROLLER_PACKAGE = "ru.proitr.controller";
    private static final String LISTENER_PACKAGE = "ru.proitr.listener";
    private static final String SERVICE_PACKAGE = "ru.proitr.service";
    private static final String CLIENT_PACKAGE = "ru.proitr.core.client";
    private static final String REPOSITORY_PACKAGE = "ru.proitr.repository";
    private static final String PRODUCER_PACKAGE = "ru.proitr.producer";
    private static final String MAPPER_PACKAGE = "ru.proitr.mapper";
    private static final String JOOQ_GEN_PACKAGE = "ru.proitr.generated";
    private static final String DAO_PACKAGE = "ru.proitr.generated.tables.daos";
    private static final String INTERNAL_CONTROLLER_PACKAGE = "oru.proitr.controller.internal";
    private static final String JAKARTA_SERVLET_PACKAGE = "jakarta.servlet";
    private static final String UTIL_PACKAGE = "ru.proitr.util";
    private static final String SERVLET_PACKAGE = "ru.proitr.servlet";
    private static final String SECURITY_PACKAGE = "ru.proitr.security";


    private ClassFileImporter getBaseFileImporter() {
        return new ClassFileImporter().withImportOption(location -> !location.contains("arch"));
    }

    @Test
    @DisplayName("Между пакетами отсутствуют циклические зависимости")
    void no_cycles_by_method_calls_between_slices() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching("(" + CORE_PACKAGE + ").(*)..")
                .namingSlices("Пакет $1.$2")
                .should().beFreeOfCycles();

        rule.check(getBaseFileImporter()
                .importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Соблюдается слоеная архитектура приложения")
    void layered_architecture() {
        ArchRule rule = Architectures.layeredArchitecture()
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
        ArchRule rule = ArchRuleDefinition.noClasses().that()
                .resideOutsideOfPackage(REPOSITORY_PACKAGE + "..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(DAO_PACKAGE + "..");

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Класс EaistRequestContext можно использовать только в классах контролерах")
    void eaist_request_context_only_in_controllers() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .should()
                .resideOutsideOfPackages(CONTROLLER_PACKAGE + "..")
                .andShould()
                .dependOnClassesThat()
                .areAssignableTo(EaistRequestContext.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Все классы сервисы должны быть с аннотацией Service и содержать \"Service\" в имени")
    void all_classes_in_service_package_are_annotated_and_correctly_named() {
        ArchRule rule = ArchRuleDefinition.classes().that()
                .resideInAPackage(SERVICE_PACKAGE + "..")
                .and().areNotNestedClasses()
                .and().areNotInterfaces()
                .should()
                .beAnnotatedWith(Service.class)
                .andShould()
                .haveSimpleNameEndingWith("Service");

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Все классы за пределами пакета сервисов не должны содержать аннотацию Service и \"Service\" в имени")
    void no_services_outside_of_package() {
        ArchRule rule = ArchRuleDefinition.noClasses().that()
                .resideOutsideOfPackage(SERVICE_PACKAGE + "..")
                .should()
                .beAnnotatedWith(Service.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Все методы классов контроллеров должны не должны принимать аргументы класса Map")
    void all_methods_in_classes_controller_package_should_not_accept_arguments_map() {
        ArchRule rule = ArchRuleDefinition.methods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage(CONTROLLER_PACKAGE + "..")
                .and()
                .arePublic()
                .should()
                .notHaveRawParameterTypes(Map.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Все методы с аннотацией SecureMultiple или Secure, должны быть в не Internal контроллерах")
    void all_internal_controller_without_security() {
        ArchRule rule = ArchRuleDefinition.noMethods().that()
                .areAnnotatedWith(SecureMultiple.class)
                .or()
                .areAnnotatedWith(Secure.class)
                .should()
                .bePublic()
                .andShould()
                .beDeclaredInClassesThat()
                .haveNameMatching(".+InternalController")
                .andShould()
                .beDeclaredInClassesThat()
                .resideInAPackage(CONTROLLER_PACKAGE + "..")
                .orShould()
                .beDeclaredInClassesThat()
                .resideInAPackage(INTERNAL_CONTROLLER_PACKAGE + "..")
                .andShould()
                .bePublic();

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Все исключения должны быть специализированны")
    void on_throwable() {
        ArchRule rule = ArchRuleDefinition.noCodeUnits()
                .should()
                .declareThrowableOfType(Error.class)
                .orShould()
                .declareThrowableOfType(Throwable.class)
                .orShould()
                .declareThrowableOfType(Exception.class)
                .orShould()
                .declareThrowableOfType(RuntimeException.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Аннотация @Autowired над конструкторами ставиться по-умолчанию")
    void autowired_for_constructors() {
        ArchRule rule = ArchRuleDefinition.constructors()
                .that()
                .areNotAnnotatedWith(Autowired.class)
                .should()
                .beDeclaredInClassesThat()
                .resideInAPackage(SERVICE_PACKAGE + "..");

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Внедрений через поля не должно быть")
    void autowired_for_fields() {
        ArchRule rule = ArchRuleDefinition.noFields()
                .should()
                .beAnnotatedWith(Autowired.class)
                .orShould()
                .beAnnotatedWith(Value.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Внедрений через методы не должно быть")
    void autowired_for_methods() {
        ArchRule rule = ArchRuleDefinition.noMethods()
                .should()
                .beAnnotatedWith(Autowired.class)
                .orShould()
                .beAnnotatedWith(Value.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Классы jakarta.servlet доступны только в сервлетах, Security-фильтрах и Util классах")
    void  no_servlet_servlet_logic_outside_servlets_and_utils() {
        ArchRule rule =
                ArchRuleDefinition.noClasses().that()
                        .resideOutsideOfPackage(SERVLET_PACKAGE + "..")
                        .and()
                        .resideOutsideOfPackage(SECURITY_PACKAGE + "..")
                        .and()
                        .resideOutsideOfPackage(UTIL_PACKAGE + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage(JAKARTA_SERVLET_PACKAGE + "..");

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Не допускается использование ThreadPoolExecutor с неограниченным количеством потоков")
    void no_unlimited_tread_pool() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .should()
                .callMethod(Executors.class, "newCachedThreadPool")
                .orShould()
                .callMethod(Executors.class, "newCachedThreadPool", ThreadFactory.class);

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }

    @Test
    @DisplayName("Не допускаются jooq выражения для модификации без условия WHERE, кроме INSERT")
    void all_jooq_executes_except_insert_have_where_condition() {
        ArchRule rule = ArchRuleDefinition.classes().that()
                        .areAnnotatedWith(Repository.class)
                        .and()
                        .areNotAssignableTo(DAOImpl.class)
                        .should(
                                new ArchCondition<>(
                                        "check methods with .execute() (dslContext.update() / dslContext.delete() " +
                                                "have a condition where)"
                                ) {
                                    @Override
                                    public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
                                        javaClass.getMethods()
                                                .stream()
                                                .filter(JavaMethod::isMethod)
                                                .filter(method -> method.getCallsFromSelf()
                                                        .stream()
                                                        .anyMatch(call -> "execute".equals(call.getName())) &&
                                                        method.getCallsFromSelf()
                                                                .stream()
                                                                .noneMatch(call -> "insertInto".equals(call.getName()))
                                                )
                                                .filter(method -> method.getCallsFromSelf()
                                                        .stream()
                                                        .noneMatch(call -> "where".equals(call.getName()))
                                                )
                                                .forEach(javaMethod -> conditionEvents.add(
                                                        new SimpleConditionEvent(
                                                                javaClass,
                                                                false,
                                                                javaMethod.getFullName() +
                                                                        " possible doing .execute() without block where()")
                                                ));
                                    }
                                });

        rule.check(getBaseFileImporter().importPackages(CORE_PACKAGE));
    }
}
