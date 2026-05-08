package com.benny1611.template.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class LocaleProvider {


    private final Set<Locale> availableLocales;

    public LocaleProvider() throws IOException {
        this.availableLocales = loadLocales();
    }

    private Set<Locale> loadLocales() throws IOException {
        Set<Locale> locales = new HashSet<>();

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();

        Resource[] resources =
                resolver.getResources("classpath:i18n/mail_*.properties");

        for (Resource resource : resources) {
            String filename = resource.getFilename(); // mail_en.properties
            if (filename == null) continue;

            String tag = filename
                    .replace("mail_", "")
                    .replace(".properties", "")
                    .replace('_', '-');

            locales.add(Locale.forLanguageTag(tag));
        }

        locales.add(Locale.ENGLISH);

        return Collections.unmodifiableSet(locales);
    }

    public boolean supports(Locale locale) {
        return availableLocales.contains(locale);
    }
}
