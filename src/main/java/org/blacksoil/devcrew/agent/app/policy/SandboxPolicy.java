package org.blacksoil.devcrew.agent.app.policy;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.blacksoil.devcrew.common.exception.DomainException;

/**
 * Проверяет что путь находится внутри разрешённого корня (/projects по умолчанию). Защищает от path
 * traversal атак: ../../../etc/passwd и подобных. Использует toRealPath() для существующих путей,
 * чтобы предотвратить обход через символические ссылки.
 */
public class SandboxPolicy {

  private final Path sandboxRoot;

  public SandboxPolicy(String sandboxRoot) {
    Path base;
    try {
      // toRealPath() разрешает симлинки в самом корне sandbox
      base = Path.of(sandboxRoot).toRealPath();
    } catch (IOException | RuntimeException e) {
      base = Path.of(sandboxRoot).normalize().toAbsolutePath();
    }
    this.sandboxRoot = base;
  }

  /**
   * @throws DomainException если путь null, пустой, относительный или вне sandboxRoot
   */
  public void validatePath(String path) {
    if (path == null || path.isBlank()) {
      throw new DomainException("Путь не может быть пустым — доступ запрещён");
    }

    Path normalized;
    try {
      // toRealPath() разрешает символические ссылки — предотвращает симлинк-атаки
      normalized = Path.of(path).toRealPath();
    } catch (NoSuchFileException e) {
      // Файл не существует — разрешаем симлинки в родительской директории,
      // чтобы корректно работать на macOS где /var -> /private/var
      try {
        var absolute = Path.of(path).toAbsolutePath().normalize();
        var parent = absolute.getParent();
        if (parent != null) {
          try {
            normalized = parent.toRealPath().resolve(absolute.getFileName());
          } catch (IOException ex) {
            normalized = absolute;
          }
        } else {
          normalized = absolute;
        }
      } catch (Exception ex) {
        throw new DomainException("Некорректный путь — доступ запрещён: " + path);
      }
    } catch (IOException | RuntimeException e) {
      throw new DomainException("Некорректный путь — доступ запрещён: " + path);
    }

    if (!normalized.startsWith(sandboxRoot)) {
      throw new DomainException(
          "Путь '%s' находится вне sandbox-корня '%s' — доступ запрещён"
              .formatted(path, sandboxRoot));
    }
  }
}
