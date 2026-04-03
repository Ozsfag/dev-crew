package org.blacksoil.devcrew.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit-тест, закрепляющий правила гексагональной архитектуры. Нарушение правил = красный тест.
 */
class HexagonalArchitectureTest {

  private static final String BASE = "org.blacksoil.devcrew";
  private static JavaClasses classes;

  @BeforeAll
  static void loadClasses() {
    classes = new ClassFileImporter().importPackages(BASE);
  }

  @Test
  void domain_should_not_depend_on_spring() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
    rule.check(classes);
  }

  @Test
  void domain_should_not_depend_on_app_or_adapter() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..app..", "..adapter..");
    rule.check(classes);
  }

  @Test
  void app_should_not_depend_on_adapter() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..app..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..");
    rule.check(classes);
  }

  @Test
  void controllers_should_not_use_stores_directly() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..adapter.in..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Store");
    rule.check(classes);
  }

  @Test
  void controllers_should_not_have_transactional() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..adapter.in..")
            .should()
            .beAnnotatedWith("org.springframework.transaction.annotation.Transactional");
    rule.check(classes);
  }
}
