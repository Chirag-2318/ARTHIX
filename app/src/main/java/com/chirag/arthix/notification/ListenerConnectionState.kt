package com.chirag.arthix.notification

/**
 * Observable lifecycle state of the NotificationListenerService binding (PRD §2.3).
 *
 * Exposed via StateFlow by this phase; consumed by Phase 3 for a
 * "reconnecting…" indicator. Also gates health-gap reasoning with Phase 1's
 * service-restart signal.
 *
 * - DISCONNECTED: listener is not bound (permission not granted, or OS unbound it)
 * - CONNECTING: process/service has started, but onListenerConnected() hasn't fired yet
 * - CONNECTED: onListenerConnected() has fired — notifications are being received
 */
enum class ListenerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}
