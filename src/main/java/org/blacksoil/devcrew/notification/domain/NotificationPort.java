package org.blacksoil.devcrew.notification.domain;

/**
 * Port для отправки уведомлений архитектору.
 * Реализуется в adapter/out (Telegram, email и т.д.).
 */
public interface NotificationPort {

    /**
     * Отправляет произвольное сообщение.
     */
    void send(String message);
}
