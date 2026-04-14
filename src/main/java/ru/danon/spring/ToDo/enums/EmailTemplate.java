package ru.danon.spring.ToDo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {

    PASSWORD_RESET(
            "Восстановление пароля",
            """
            Здравствуйте, %s!
            
            Для восстановления пароля используйте следующий код подтверждения:
            
            %s
            
            Код действителен в течение %d минут.
            
            Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.
            
            С уважением,
            Команда поддержки
            """
    ),

    PASSWORD_RESET_SUCCESS(
            "Пароль успешно изменен",
            """
            Здравствуйте, %s!
            
            Ваш пароль был успешно изменен.
            
            Если это были не вы, пожалуйста, немедленно свяжитесь со службой поддержки.
            
            С уважением,
            Команда поддержки
            """
    ),

    WELCOME(
            "Добро пожаловать!",
            """
            Здравствуйте, %s!
            
            Спасибо за регистрацию в нашей системе ToDo.
            
            Теперь вы можете:
            • Просматривать назначенные задачи
            • Загружать решения
            • Отслеживать дедлайны
            • Общаться с преподавателями в комментариях
            
            С уважением,
            Команда поддержки
            """
    );

    private final String subject;
    private final String bodyTemplate;

    public String formatBody(Object... args) {
        return String.format(bodyTemplate, args);
    }
}