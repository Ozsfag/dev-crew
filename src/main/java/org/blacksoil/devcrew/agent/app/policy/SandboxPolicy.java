package org.blacksoil.devcrew.agent.app.policy;

import org.blacksoil.devcrew.common.exception.DomainException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Проверяет что путь находится внутри разрешённого корня (/projects по умолчанию).
 * Защищает от path traversal атак: ../../../etc/passwd и подобных.
 */
@Component
public class SandboxPolicy {

    private final Path sandboxRoot;

    public SandboxPolicy(String sandboxRoot) {
        this.sandboxRoot = Path.of(sandboxRoot).normalize().toAbsolutePath();
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
            normalized = Path.of(path).normalize().toAbsolutePath();
        } catch (Exception e) {
            throw new DomainException("Некорректный путь — доступ запрещён: " + path);
        }

        if (!normalized.startsWith(sandboxRoot)) {
            throw new DomainException(
                "Путь '%s' находится вне sandbox-корня '%s' — доступ запрещён"
                    .formatted(path, sandboxRoot)
            );
        }
    }
}
