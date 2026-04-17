package org.blacksoil.devcrew.agent.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.MeteredCommandRunner;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.ProcessBuilderCommandRunner;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ToolMetricsConfigTest {

  @Test
  void meteredCommandRunner_returns_metered_decorator() {
    var config = new ToolMetricsConfig();
    var agentProperties = Mockito.mock(AgentProperties.class);
    var delegate = new ProcessBuilderCommandRunner(agentProperties);
    var meterRegistry = new SimpleMeterRegistry();

    var result = config.meteredCommandRunner(delegate, meterRegistry);

    assertThat(result).isInstanceOf(MeteredCommandRunner.class);
  }
}
