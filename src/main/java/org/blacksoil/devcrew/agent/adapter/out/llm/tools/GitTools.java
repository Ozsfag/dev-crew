package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Инструменты для работы с Git-репозиторием.
 * Все пути проверяются через SandboxPolicy.
 * Передаются агенту через LangChain4j AiServices.
 */
@Component
@RequiredArgsConstructor
public class GitTools {

    private final org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner commandRunner;
    private final SandboxPolicy sandboxPolicy;

    @Tool("Show git status in the project directory")
    public String gitStatus(String projectPath) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "git", "status");
            return result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Stage files for commit. Pass space-separated file paths relative to the project root")
    public String gitAdd(String projectPath, String files) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var fileList = files.trim().split("\\s+");
            var args = new String[2 + fileList.length];
            args[0] = "git";
            args[1] = "add";
            System.arraycopy(fileList, 0, args, 2, fileList.length);
            var result = commandRunner.run(new File(projectPath), args);
            if (result.exitCode() != 0) {
                return "ERROR: git add завершился с ошибкой\n" + result.output();
            }
            return "OK: файлы добавлены в индекс";
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Create a git commit with the given message")
    public String gitCommit(String projectPath, String message) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "git", "commit", "-m", message);
            if (result.exitCode() != 0) {
                return "ERROR: git commit завершился с ошибкой\n" + result.output();
            }
            return result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Push the current branch to remote origin with tracking (-u origin <branch>)")
    public String gitPush(String projectPath, String branch) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "git", "push", "-u", "origin", branch);
            if (result.exitCode() != 0) {
                return "ERROR: git push завершился с ошибкой\n" + result.output();
            }
            return "OK: ветка " + branch + " отправлена на remote";
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Create a new git branch and switch to it")
    public String createBranch(String projectPath, String branchName) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "git", "checkout", "-b", branchName);
            if (result.exitCode() != 0) {
                return "ERROR: не удалось создать ветку " + branchName + "\n" + result.output();
            }
            return "OK: создана и активна ветка " + branchName;
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Get the name of the current git branch")
    public String getCurrentBranch(String projectPath) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "git", "rev-parse", "--abbrev-ref", "HEAD");
            return result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("Show the git diff of all unstaged changes in the project")
    public String gitDiff(String projectPath) {
        try {
            sandboxPolicy.validatePath(projectPath);
            var result = commandRunner.run(new File(projectPath), "git", "diff");
            return result.output().isBlank() ? "(нет изменений)" : result.output();
        } catch (DomainException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
