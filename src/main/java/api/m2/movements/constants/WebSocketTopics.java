package api.m2.movements.constants;

/**
 * Constantes para los topics de WebSocket STOMP.
 * Centraliza todas las rutas de publicación para evitar strings hardcodeados.
 */
public final class WebSocketTopics {

    // Base paths
    public static final String MOVEMENTS = "/topic/movimientos";
    public static final String SERVICES = "/topic/servicios";
    public static final String WORKSPACES = "/topic/workspace";
    public static final String CATEGORIES = "/topic/categories";
    public static final String NOTIFICATIONS = "/topic/notifications";
    public static final String INVITATIONS = "/topic/invitations";

    // Suffixes
    public static final String NEW = "/new";
    public static final String UPDATE = "/update";
    public static final String DELETE = "/delete";
    public static final String REMOVE = "/remove";
    public static final String DEFAULT = "/default";

    private WebSocketTopics() {
        // Utility class
    }

    /**
     * Construye el topic para nuevos movimientos de un workspace.
     */
    public static String movementsNew(Long workspaceId) {
        return MOVEMENTS + "/" + workspaceId + NEW;
    }

    /**
     * Construye el topic para movimientos eliminados de un workspace.
     */
    public static String movementsDelete(Long workspaceId) {
        return MOVEMENTS + "/" + workspaceId + DELETE;
    }

    /**
     * Construye el topic para nuevos servicios de un workspace.
     */
    public static String servicesNew(Long workspaceId) {
        return SERVICES + "/" + workspaceId + NEW;
    }

    /**
     * Construye el topic para servicios actualizados de un workspace.
     */
    public static String servicesUpdate(Long workspaceId) {
        return SERVICES + "/" + workspaceId + UPDATE;
    }

    /**
     * Construye el topic para servicios eliminados de un workspace.
     */
    public static String servicesRemove(Long workspaceId) {
        return SERVICES + "/" + workspaceId + REMOVE;
    }

    /**
     * Construye el topic para cambio de workspace por defecto.
     * Usa el Keycloak subject (UUID string), no el userId.
     */
    public static String workspacesDefault(String keycloakSubject) {
        return WORKSPACES + DEFAULT + "/" + keycloakSubject;
    }

    /**
     * Construye el topic para categorías actualizadas de un workspace.
     */
    public static String categoriesUpdate(Long workspaceId) {
        return CATEGORIES + "/" + workspaceId + UPDATE;
    }

    /**
     * Construye el topic para nuevas notificaciones de un workspace.
     */
    public static String notificationsNew(Long workspaceId) {
        return NOTIFICATIONS + "/" + workspaceId + NEW;
    }

    /**
     * Construye el topic para invitaciones recibidas por un usuario, direccionado por email
     * (el "name" del principal autenticado, igual que en el resto del backend) ya que el
     * evento que llega desde api-identity no incluye el subject de Keycloak del invitado.
     */
    public static String invitationsNew(String invitedUserEmail) {
        return INVITATIONS + "/" + invitedUserEmail + NEW;
    }
}
