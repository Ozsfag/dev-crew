package org.blacksoil.devcrew.agent.adapter.out.llm.tools;

import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandResult;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner;
import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitToolsTest {

    @TempDir
    Path tempDir;

    @Mock
    private CommandRunner commandRunner;

    private GitTools gitTools;

    @BeforeEach
    void setUp() {
        gitTools = new GitTools(commandRunner, new SandboxPolicy(tempDir.toString()));
    }

    @Test
    void gitStatus_executes_git_status_and_returns_output() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "nothing to commit"));

        var result = gitTools.gitStatus(tempDir.toString());

        assertThat(result).contains("nothing to commit");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "status");
    }

    @Test
    void gitStatus_returns_error_for_path_outside_sandbox() {
        var result = gitTools.gitStatus("/non/existent/path");
        assertThat(result).contains("ERROR");
    }

    @Test
    void gitAdd_executes_git_add_with_files() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, ""));

        var result = gitTools.gitAdd(tempDir.toString(), "src/Main.java src/Foo.java");

        assertThat(result).contains("OK");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "add", "src/Main.java", "src/Foo.java");
    }

    @Test
    void gitAdd_returns_error_for_path_outside_sandbox() {
        var result = gitTools.gitAdd("/etc", "passwd");
        assertThat(result).contains("ERROR");
    }

    @Test
    void gitCommit_executes_git_commit_with_message() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "[main abc1234] feat: add tests"));

        var result = gitTools.gitCommit(tempDir.toString(), "feat: add tests");

        assertThat(result).contains("feat: add tests");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "commit", "-m", "feat: add tests");
    }

    @Test
    void gitCommit_returns_error_on_non_zero_exit() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(1, "nothing to commit"));

        var result = gitTools.gitCommit(tempDir.toString(), "empty commit");

        assertThat(result).contains("ERROR");
    }

    @Test
    void gitPush_executes_git_push_with_branch() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "Branch 'feat/x' set up to track..."));

        var result = gitTools.gitPush(tempDir.toString(), "feat/x");

        assertThat(result).contains("OK");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "push", "-u", "origin", "feat/x");
    }

    @Test
    void gitPush_returns_error_on_failure() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(128, "fatal: authentication failed"));

        var result = gitTools.gitPush(tempDir.toString(), "main");

        assertThat(result).contains("ERROR");
    }

    @Test
    void createBranch_executes_git_checkout_minus_b() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "Switched to a new branch 'feat/auth'"));

        var result = gitTools.createBranch(tempDir.toString(), "feat/auth");

        assertThat(result).contains("OK");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "checkout", "-b", "feat/auth");
    }

    @Test
    void getCurrentBranch_returns_branch_name() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "main\n"));

        var result = gitTools.getCurrentBranch(tempDir.toString());

        assertThat(result).contains("main");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "rev-parse", "--abbrev-ref", "HEAD");
    }

    @Test
    void gitDiff_returns_diff_output() {
        when(commandRunner.run(any(File.class), any(String[].class)))
            .thenReturn(new CommandResult(0, "diff --git a/Foo.java b/Foo.java\n+new line"));

        var result = gitTools.gitDiff(tempDir.toString());

        assertThat(result).contains("diff --git");
        var captor = ArgumentCaptor.<String[]>captor();
        verify(commandRunner).run(eq(new File(tempDir.toString())), captor.capture());
        assertThat(captor.getValue()).containsExactly("git", "diff");
    }
}
