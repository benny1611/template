package com.benny1611.template.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocaleProviderTest {

    @Mock
    private ResourcePatternResolver resolver;

    @Mock
    private Resource resourceFr;

    @Mock
    private Resource resourceDe;

    private LocaleProvider localeProvider;

    @BeforeEach
    void setUp() throws IOException {
        // Mock the filenames found in the "classpath"
        when(resourceFr.getFilename()).thenReturn("mail_fr.properties");
        when(resourceDe.getFilename()).thenReturn("mail_de_DE.properties");

        // Mock the resolver to return our fake resources
        when(resolver.getResources("classpath:i18n/mail_*.properties"))
                .thenReturn(new Resource[]{resourceFr, resourceDe});

        localeProvider = new LocaleProvider(resolver);
    }

    @Test
    @DisplayName("Should support locales found in the classpath")
    void shouldSupportLocalesFromClasspath() {
        assertThat(localeProvider.supports(Locale.FRENCH)).isTrue();
        assertThat(localeProvider.supports(Locale.GERMANY)).isTrue();
    }

    @Test
    @DisplayName("Should always support English even if file is missing")
    void shouldAlwaysSupportEnglish() {
        assertThat(localeProvider.supports(Locale.ENGLISH)).isTrue();
    }

    @Test
    @DisplayName("Should not support locales that were not found")
    void shouldNotSupportUnknownLocales() {
        assertThat(localeProvider.supports(Locale.CHINESE)).isFalse();
        assertThat(localeProvider.supports(Locale.ITALIAN)).isFalse();
    }
}
