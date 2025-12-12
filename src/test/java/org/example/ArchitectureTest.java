package org.example;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.example.generated.tables.daos.DAOImpl;
import org.jooq.DSLContext;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "org.example", importOptions = ExcludeTestsImportOption.class)
public class ArchitectureTest {

    private static final String CORE_PACKAGE = "org.example";
    private static final String CONTROLLER_PACKAGE = "org.example.controller";
    private static final String LISTENER_PACKAGE = "org.example.listener";
    private static final String SERVICE_PACKAGE = "org.example.service";
    private static final String CLIENT_PACKAGE = "org.example.client";
    private static final String REPOSITORY_PACKAGE = "org.example.repository";
    private static final String PRODUCER_PACKAGE = "org.example.producer";
    private static final String MAPPER_PACKAGE = "org.example.mapper";
    private static final String JOOQ_GEN_PACKAGE = "org.example.generated";
    private static final String DAO_PACKAGE = "org.example.generated.tables.daos";
    private static final String INTERNAL_CONTROLLER_PACKAGE = "org.example.controller.internal";
    private static final String JAKARTA_SERVLET_PACKAGE = "jakarta.servlet";
    private static final String UTIL_PACKAGE = "org.example.util";
    private static final String SERVLET_PACKAGE = "org.example.servlet";
    private static final String SECURITY_PACKAGE = "org.example.security";


    private static final String ENUM_PACKAGE = "org.example.enums";

    @ArchTest
    public static final ArchRule layered_architecture =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()

                    .layer("Inbound")
                    .definedBy(
                            CONTROLLER_PACKAGE + "..",
                            LISTENER_PACKAGE + "..",
                            SERVLET_PACKAGE + ".."
                    )

                    .layer("Service")
                    .definedBy(
                            SERVICE_PACKAGE + ".."
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
                    // Классы слоя Inbound не должны использоваться в классах других слоев
                    .whereLayer("Inbound").mayNotBeAccessedByAnyLayer()
                    // Классы слоя Service можно использовать только в классах слоя Inbound
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Inbound")
                    // Классы слоев Repository и Producer можно использовать только в классах слоя Service
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                    .whereLayer("Producer").mayOnlyBeAccessedByLayers("Service")
                    // Классы слоя JooqInfrastructure можно использовать только в классах слоев Repository и Mapper
                    .whereLayer("JooqInfrastructure")
                    .mayOnlyBeAccessedByLayers("Mapper", "Repository");

    // Только классы сервисы могут зависеть от классов клиентов
    @ArchTest
    public static final ArchRule no_classes_except_services_depend_on_clients =
            noClasses().that()
                    .resideOutsideOfPackage(SERVICE_PACKAGE + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(CLIENT_PACKAGE + "..")
                    .because("Только классы сервисы могут зависеть от классов клиентов");

    // Только классы репозитории могут зависеть от классов ДАО
    @ArchTest
    public static final ArchRule no_classes_depend_on_dao_except_repo =
            noClasses().that()
                    .resideOutsideOfPackage(REPOSITORY_PACKAGE + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(DAO_PACKAGE + "..");

    // Класс EaistRequestContext можно использовать только в классах контролерах
    @ArchTest
    public static final ArchRule eaist_request_context_only_in_controllers =
            noClasses()
                    .should()
                    .resideOutsideOfPackages(CONTROLLER_PACKAGE + "..")
                    .andShould()
                    .dependOnClassesThat()
                    .areAssignableTo(EaistRequestContext.class);

    // Класс KafkaTemplate можно использовать только в классах продюсерах
    @ArchTest
    public static final ArchRule kafka_template_only_on_producer_lvl =
            noClasses()
                    .should()
                    .resideOutsideOfPackages(PRODUCER_PACKAGE + "..")
                    .andShould()
                    .dependOnClassesThat()
                    .areAssignableTo(KafkaTemplate.class);

    // Аннотацию Transactional можно использовать только в классах сервисах
    @ArchTest
    public static final ArchRule transactional_should_be_only_in_services =
            noClasses()
                    .should()
                    .resideOutsideOfPackages(SERVICE_PACKAGE + "..")
                    .andShould()
                    .dependOnClassesThat()
                    .areAssignableTo(Transactional.class);

    // TransactionTemplate можно использовать только в классах сервисах
    @ArchTest
    public static final ArchRule transactionTemplate_should_be_only_in_services =
            noClasses()
                    .should()
                    .resideOutsideOfPackages(SERVICE_PACKAGE + "..")
                    .andShould()
                    .dependOnClassesThat()
                    .areAssignableTo(TransactionTemplate.class);

    // DSLContext можно использовать только в классах репозиториях
    @ArchTest
    public static final ArchRule dsl_context_should_be_only_in_repositories_and_daos =
            noClasses()
                    .should()
                    .resideOutsideOfPackages(REPOSITORY_PACKAGE + "..")
                    .andShould()
                    .dependOnClassesThat()
                    .areAssignableTo(DSLContext.class);

    // Все классы контроллеры должны быть с аннотацией RestController и содержать "Controller" в имени
    @ArchTest
    public static final ArchRule all_classes_in_controller_package_are_annotated_and_correctly_named_1 =
            classes()
                    .that()
                    .resideInAPackage(CONTROLLER_PACKAGE + "..")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .beAnnotatedWith(RestController.class)
                    .andShould()
                    .haveSimpleNameContaining("Controller");

    // Все интерфейсы контроллеры должны содержать "Api" в имени
    @ArchTest
    public static final ArchRule all_classes_in_controller_package_are_annotated_and_correctly_named_2 =
            classes()
                    .that()
                    .resideInAPackage(CONTROLLER_PACKAGE + "..")
                    .and()
                    .areInterfaces()
                    .should()
                    .haveSimpleNameContaining("Api");

    // Все классы за пределами пакета контроллеров не должны содержать аннотацию RestController и "Controller" в имени
    @ArchTest
    public static final ArchRule no_controllers_outside_of_package =
            noClasses().that()
                    .resideOutsideOfPackage(CONTROLLER_PACKAGE + "..")
                    .should()
                    .beAnnotatedWith(RestController.class)
                    .orShould()
                    .haveSimpleNameContaining("Controller");

    // Все классы сервисы должны быть с аннотацией Service и содержать "Service" в имени
    @ArchTest
    public static final ArchRule all_classes_in_service_package_are_annotated_and_correctly_named =
            classes().that()
                    .resideInAPackage(SERVICE_PACKAGE + "..")
                    .and().areNotNestedClasses()
                    .and().areNotInterfaces()
                    .should()
                    .beAnnotatedWith(Service.class)
                    .andShould()
                    .haveSimpleNameContaining("Service")
                    .because("Все классы сервисы должны быть с аннотацией Service и содержать \"Service\" в имени");

    // Все классы за пределами пакета сервисов не должны содержать аннотацию Service и "Service" в имени
    @ArchTest
    public static final ArchRule no_services_outside_of_package =
            noClasses().that()
                    .resideOutsideOfPackage(SERVICE_PACKAGE + "..")
                    .should()
                    .beAnnotatedWith(Service.class);

    // Все классы репозитории должны быть с аннотацией Repository и содержать "Repository" в имени
    @ArchTest
    public static final ArchRule all_classes_in_repository_package_are_annotated_and_correctly_named =
            classes().that()
                    .resideInAPackage(REPOSITORY_PACKAGE + "..")
                    .and().areNotNestedClasses()
                    .and().areNotInterfaces()
                    .should()
                    .beAnnotatedWith(Repository.class)
                    .andShould()
                    .haveSimpleNameContaining("Repository");

    // Все классы за пределами пакета репозиториев не должны содержать аннотацию Repository и "Repository" в имени
    @ArchTest
    public static final ArchRule no_repositories_and_dao_outside_of_package =
            noClasses().that()
                    .resideOutsideOfPackages(
                            REPOSITORY_PACKAGE + "..",
                            DAO_PACKAGE + ".."
                    )
                    .should()
                    .beAnnotatedWith(Repository.class)
                    .orShould()
                    .haveSimpleNameContaining("Repository")
                    .orShould()
                    .haveSimpleNameContaining("Dao");

    // Аннотацию Secure нельзя использовать в интерфейсах
    @ArchTest
    public static final ArchRule secure_should_not_be_in_interfaces =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .areInterfaces()
                    .should()
                    .notBeAnnotatedWith(Secure.class);

    // Не допускаются jooq выражения для модификации без условия WHERE, кроме INSERT
    @ArchTest
    public static final ArchRule all_jooq_executes_except_insert_have_where_condition =
            classes().that()
                    .areAnnotatedWith(Repository.class)
                    .and()
                    .areNotAssignableTo(DAOImpl.class)
                    .should(
                            new ArchCondition<JavaClass>(
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
                                            .forEach(javaMethod -> {
                                                conditionEvents.add(
                                                        new SimpleConditionEvent(
                                                                javaClass,
                                                                false,
                                                                javaMethod.getFullName() +
                                                                        " possible doing .execute() without block where()")
                                                );
                                            });
                                }
                            });

    // Классы событий должны содержать хотя бы один метод с аннотацией EventListener и конструктор
    @ArchTest
    public static final ArchRule all_events_are_used_in_system =
            classes().that()
                    .resideInAPackage("ru.proitr.contracts.events")
                    .should(
                            new ArchCondition<JavaClass>("should be used in methods that annotated @EventListener at least once") {
                                @Override
                                public void check(JavaClass javaClass, ConditionEvents conditionEvents) {

                                    var hasListener = javaClass.getCodeUnitCallsToSelf().stream().map(JavaAccess::getOwner).anyMatch(owner -> owner.isAnnotatedWith(EventListener.class));

                                    if (Boolean.FALSE.equals(hasListener)) {
                                        conditionEvents.add(
                                                new SimpleConditionEvent(
                                                        javaClass,
                                                        false,
                                                        javaClass.getName() + " doesn't have event listener")
                                        );
                                    }

                                }
                            }
                    )
                    .andShould(
                            new ArchCondition<>("should be created somewhere") {
                                @Override
                                public void check(JavaClass javaClass, ConditionEvents conditionEvents) {

                                    var hasNotContructorCall = javaClass.getConstructorCallsToSelf().isEmpty();

                                    if (hasNotContructorCall) {
                                        conditionEvents.add(
                                                new SimpleConditionEvent(
                                                        javaClass,
                                                        false,
                                                        javaClass.getName() + " doesn't created")
                                        );
                                    }

                                }
                            }
                    );

    // Классы jakarta.servlet доступны только в сервлетах, Security-фильтрах и Util классах
    @ArchTest
    public static final ArchRule no_servlet_servlet_logic_outside_servlets_and_utils =
            noClasses().that()
                    .resideOutsideOfPackage(SERVLET_PACKAGE + "..")
                    .and()
                    .resideOutsideOfPackage(SECURITY_PACKAGE + "..")
                    .and()
                    .resideOutsideOfPackage(UTIL_PACKAGE + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(JAKARTA_SERVLET_PACKAGE + "..");

    //Все методы классов контроллеров должны не должны принимать аргументы класса Map
    @ArchTest
    public static final ArchRule  all_methods_in_classes_controller_package_should_not_accept_arguments_map =
            methods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage(CONTROLLER_PACKAGE + "..")
                .and()
                .arePublic()
                .should()
                .notHaveRawParameterTypes(Map.class);

    //В пакете enums могут быть только enum
    @ArchTest
    public static final ArchRule enum_only =  noClasses().that()
            .resideInAPackage(
                    ENUM_PACKAGE + ".."
            )
            .should()
            .notBeEnums();

    //Все методы с аннотацией SecureMultiple или Secure, должны быть в не Internal контроллерах
    @ArchTest
    public static final ArchRule all_internal_controller_without_security = noMethods().that()
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

    //Все методы в не Internal контроллерах должны быть с аннотацией SecureMultiple или Secure
    @ArchTest
    public static final ArchRule all_not_internal_controller_with_security = noMethods().that()
                .areNotAnnotatedWith(SecureMultiple.class)
                .and()
                .areNotAnnotatedWith(Secure.class)
                .should()
                .bePublic()
                .andShould()
                .beDeclaredInClassesThat()
                .haveNameNotMatching(".+InternalController")
                .andShould()
                .beDeclaredInClassesThat()
                .resideInAPackage(CONTROLLER_PACKAGE + "..")
                .andShould()
                .beDeclaredInClassesThat()
                .resideOutsideOfPackage(INTERNAL_CONTROLLER_PACKAGE + "..")
                .andShould()
                .beDeclaredInClassesThat()
                .areNotInterfaces();

    //Не допускается использование ThreadPoolExecutor с неограниченным количеством потоков
    @ArchTest
    public static final ArchRule no_unlimited_tread_pool = noClasses()
            .should()
            .callMethod(Executors.class, "newCachedThreadPool")
            .orShould()
            .callMethod(Executors.class, "newCachedThreadPool", ThreadFactory.class);
}
