package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner;
import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Инструменты для работы с Docker из агента DevOps.
 * Все пути проверяются через SandboxPolicy.
 * Передаются агенту через LangChain4j AiServices.
 */
@Component
@RequiredArgsConstructor
public class DockerTools {

    private final CommandRunner commandRunner;
    private final SandboxPolicy sandboxPolicy;

    @Tool("Build a Docker image from the Dockerfile in the given project directory with the specified tag (e.g. myapp:1.0)")
    public String dockerBuild(String projectPath, String imageTag) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "docker", "build", "-t", imageTag, ".");
            if (result.exitCode() != 0) {
                return "ERROR: docker build завершился с ошибкой\n" + result.output();
            }
            return "OK: образ собран " + imageTag + "\n" + result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Push a Docker image to a registry (e.g. registry.io/myapp:1.0)")
    public String dockerPush(String projectPath, String imageTag) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "docker", "push", imageTag);
            if (result.exitCode() != 0) {
                return "ERROR: docker push завершился с ошибкой\n" + result.output();
            }
            return "OK: образ отправлен в registry: " + imageTag + "\n" + result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Pull latest images and start services defined in docker-compose.yml in the given project directory")
    public String dockerComposeUp(String projectPath) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(
                new File(projectPath), "docker", "compose", "up", "-d", "--pull", "always"
            );
            if (result.exitCode() != 0) {
                return "ERROR: docker compose up завершился с ошибкой\n" + result.output();
            }
            return "OK: сервисы запущены\n" + result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Pull latest images defined in docker-compose.yml without starting them")
    public String dockerComposePull(String projectPath) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "docker", "compose", "pull");
            if (result.exitCode() != 0) {
                return "ERROR: docker compose pull завершился с ошибкой\n" + result.output();
            }
            return "OK: образы обновлены\n" + result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("List all local Docker images in the given project directory")
    public String dockerImageList(String projectPath) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "docker", "images");
            return result.output().isBlank() ? "(нет локальных образов)" : result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
