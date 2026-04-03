package org.blacksoil.devcrew.common;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("tc")
@Transactional
@Tag("integration")
public abstract class IntegrationTestBase {}
