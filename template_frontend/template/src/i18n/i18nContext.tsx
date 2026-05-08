import { createContext, useContext, useState } from "react";
import en from "./en";

type Language = "en";

const translations = { en };

const I18nContext = createContext({
  translation: translations.en,
  setLanguage: (_: Language) => {},
});

export const I18nProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [language, setLanguage] = useState<Language>("en");

  return (
    <I18nContext.Provider
      value={{
        translation: translations[language],
        setLanguage,
      }}
    >
      {children}
    </I18nContext.Provider>
  );
};

export const useI18n = () => useContext(I18nContext);
