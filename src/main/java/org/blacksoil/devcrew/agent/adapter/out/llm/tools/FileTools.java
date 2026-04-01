package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Инструменты для работы с файловой системой.
 * Все пути проверяются через SandboxPolicy — запрещён доступ вне /projects.
 * Передаются агенту через LangChain4j AiServices.
 */
@Component
@RequiredArgsConstructor
public class FileTools {

    private final SandboxPolicy sandboxPolicy;

    @Tool("Read the full content of a file at the given absolute path")
    public String readFile(String absolutePath) {
        try {
            sandboxPolicy.validatePath(absolutePath);
            return Files.readString(Path.of(absolutePath));
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR: не удалось прочитать файл " + absolutePath + ": " + e.getMessage();
        }
    }

    @Tool("Write content to a file at the given absolute path, creating parent directories if needed")
    public String writeFile(String absolutePath, String content) {
        try {
            sandboxPolicy.validatePath(absolutePath);
            var path = Path.of(absolutePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return "OK: файл записан " + absolutePath;
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR: не удалось записать файл " + absolutePath + ": " + e.getMessage();
        }
    }

    @Tool("List all files in a directory at the given absolute path (non-recursive)")
    public String listFiles(String absolutePath) {
        try {
            sandboxPolicy.validatePath(absolutePath);
            try (var stream = Files.list(Path.of(absolutePath))) {
                var files = stream
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.joining("\n"));
                return files.isEmpty() ? "(пустая директория)" : files;
            }
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR: не удалось прочитать директорию " + absolutePath + ": " + e.getMessage();
        }
    }
}
