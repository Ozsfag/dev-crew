package org.blacksoil.devcrew.agent.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.MeteredCommandRunner;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.ProcessBuilderCommandRunner;
import org.junit.jupiter.api.Test;

class ToolMetricsConfigTest {

  @Test
  void meteredCommandRunner_returns_metered_decorator() {
    var config = new ToolMetricsConfig();
    var delegate = new ProcessBuilderCommandRunner();
    var meterRegistry = new SimpleMeterRegistry();

    var result = config.meteredCommandRunner(delegate, meterRegistry);

    assertThat(result).isInstanceOf(MeteredCommandRunner.class);
  }
}
