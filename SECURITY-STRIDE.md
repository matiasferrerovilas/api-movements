# Reporte de Seguridad STRIDE — api-movements

> Análisis basado en revisión directa del código fuente. Solo se reportan vulnerabilidades con path de explotación concreto y verificado (confianza ≥ 0.85).
>
> **Estado:** Pendiente de corrección — generado el 2026-05-24

---

## VULN 1 — Elevation of Privilege: Inyección de datos en workspaces ajenos via DEFAULT_WORKSPACE

**Archivos:** `UserSettingService.java:47` · `WorkspaceContextService.java:34` · `MovementAddService.java:32`
**Severidad:** ALTA
**Categoría STRIDE:** Elevation of Privilege / Tampering

**Descripción:**
`PUT /v1/settings/defaults/DEFAULT_WORKSPACE` acepta cualquier `Long` como valor y lo guarda sin verificar que el usuario sea miembro del workspace indicado. `WorkspaceContextService.getActiveWorkspace()` luego resuelve ese workspace ID directamente desde el repositorio **sin ningún membership check**. `MovementAddService.saveMovement()` no tiene `@RequiresMembership`.

```java
// UserSettingService.java:47 — sin validación de pertenencia
public UserSettingResponse upsert(UserSettingKey key, Long value) {
    User user = userService.getAuthenticatedUser();
    // ← Acepta value=<cualquier_workspace_id> sin verificar membresía
    setting.setSettingValue(value);
    ...
}

// WorkspaceContextService.java:34 — sin membership check
public Workspace getActiveWorkspace() {
    Long workspaceId = userSettingService.getDefaultWorkspaceId(user)...;
    return workspaceRepository.findById(workspaceId)...;  // ← fetch directo, sin verificar
}

// MovementAddService.java:32 — sin @RequiresMembership
public MovementRecord saveMovement(@Valid MovementToAdd dto) {
    var movement = movementFactory.create(dto);  // ← usa getActiveWorkspace()
    ...
}
```

**Escenario de explotación:**
1. Usuario autenticado A ejecuta: `PUT /v1/settings/defaults/DEFAULT_WORKSPACE` con `{"value": 42}` (workspace de la víctima B)
2. Ejecuta: `POST /v1/expenses` — el movimiento se crea en el workspace 42, propiedad de B
3. También aplica a `POST /v1/expenses/import-file` (usa el mismo `workspaceContextService.getActiveWorkspaceId()`)
4. El atacante puede corromper el balance financiero de cualquier workspace conociendo solo su ID (enteros secuenciales)

**Corrección:**
En `UserSettingService.upsert()`, cuando la clave es `DEFAULT_WORKSPACE`, verificar que el usuario sea miembro del workspace antes de guardar:
```java
if (key == UserSettingKey.DEFAULT_WORKSPACE) {
    workspaceQueryService.verifyUserIsMemberOfWorkspace(value, user.getId());
}
```
Alternativamente, agregar `@RequiresMembership` a `saveMovement()` o verificar membresía en `WorkspaceContextService.getActiveWorkspace()`.

---

## VULN 2 — Information Disclosure: WebSocket completamente sin autenticación

**Archivos:** `SecurityConfiguration.java:44` · `WebSocketConfig.java`
**Severidad:** ALTA
**Categoría STRIDE:** Information Disclosure / Spoofing

**Descripción:**
El endpoint `/ws/**` está explícitamente en `permitAll()`. No existe ningún `ChannelInterceptor`, `HandshakeInterceptor` ni configuración de seguridad STOMP. Cualquier cliente, sin token JWT, puede conectarse y suscribirse a cualquier topic.

```java
// SecurityConfiguration.java:38-45
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers(
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**",
        "/ws/**"          // ← sin autenticación
    ).permitAll()
```

Los topics exponen datos financieros en tiempo real:
- `/topic/movimientos/{workspaceId}/new` → `MovementRecord` (monto, fecha, descripción, categoría)
- `/topic/servicios/{workspaceId}/new` → datos de suscripciones
- `/topic/invitation/{userId}/new` → invitaciones con emails de usuarios

Los `workspaceId` son `Long` enteros secuenciales — trivialmente enumerables.

**Escenario de explotación:**
1. Atacante conecta a `wss://movement.eva-core.com/ws` sin token
2. Suscribe a `/topic/movimientos/1/new`, `/topic/movimientos/2/new`, etc.
3. Recibe en tiempo real cada movimiento de todos los workspaces activos de la plataforma, incluyendo montos y descripciones de gastos

**Corrección:**
Agregar un `ChannelInterceptor` que valide el JWT en el header CONNECT:

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            // validar JWT y setear authentication en el accessor
        }
        return message;
    }
}
```

Y registrarlo en `WebSocketConfig.configureClientInboundChannel()`.

---

## VULN 3 — Elevation of Privilege: Cualquier miembro puede invitar usuarios (sin importar rol)

**Archivo:** `InvitationAddService.java:35-39`
**Severidad:** MEDIA
**Categoría STRIDE:** Elevation of Privilege

**Descripción:**
`inviteToWorkspace()` solo verifica que el invitante sea **miembro** del workspace, pero no verifica su **rol**. Un miembro con rol `GUEST` o `COLLABORATOR` puede invitar personas externas al workspace.

```java
// InvitationAddService.java:38-39
var loggedInUser = userService.getAuthenticatedUser();
workspaceQueryService.verifyUserIsMemberOfWorkspace(
    workspaceToInvite.getId(),
    loggedInUser.getId()
);
// ← Falta: verificar que loggedInUser tenga rol OWNER
```

**Escenario de explotación:**
Un usuario con rol `GUEST` en un workspace compartido de familia puede invitar a un tercero sin consentimiento del dueño, expandiendo el acceso a datos financieros compartidos.

**Corrección:**
Verificar que el invitante tenga rol `OWNER` antes de proceder:

```java
var membership = membershipRepository.findMember(workspaceToInvite.getId(), loggedInUser.getId())
    .orElseThrow(() -> new PermissionDeniedException("No eres miembro de este workspace"));

if (membership.getRole() != WorkspaceRole.OWNER) {
    throw new PermissionDeniedException("Solo el owner puede invitar usuarios");
}
```

---

## VULN 4 — Tampering: Sin validación de tipo de archivo en importación de PDF

**Archivo:** `MovementImportFileService.java:25-33` · `MovementController.java:96-104`
**Severidad:** MEDIA
**Categoría STRIDE:** Tampering

**Descripción:**
El endpoint `POST /v1/expenses/import-file` no valida que el archivo subido sea efectivamente un PDF. No se verifica `Content-Type`, ni magic bytes del archivo. Cualquier contenido binario se escribe en un archivo temporal y se pasa a PDFBox para parseo.

```java
// MovementImportFileService.java:28-30
pdfFile = Files.createTempFile("expense-", ".pdf");
file.transferTo(pdfFile);                              // ← sin validar tipo
String text = pdfReaderService.extractTextFromPdf(pdfFile);
```

Aunque PDFBox no ejecuta código del PDF (no hay RCE conocido en 3.0.6), se pueden enviar archivos malformados diseñados para explotar bugs de parsing en la librería.

**Corrección:**

```java
// Verificar magic bytes del PDF antes de transferir
byte[] header = file.getBytes();
if (header.length < 4 || !new String(header, 0, 4).equals("%PDF")) {
    throw new BusinessException("El archivo no es un PDF válido");
}
// Validar tamaño máximo
if (file.getSize() > MAX_FILE_SIZE_BYTES) {
    throw new BusinessException("El archivo supera el tamaño máximo permitido");
}
```

---

## VULN 5 — Information Disclosure: Credenciales de RabbitMQ hardcodeadas en configuración de producción

**Archivo:** `src/main/resources/application-prod.yaml:10-11`
**Severidad:** ALTA
**Categoría STRIDE:** Information Disclosure

**Descripción:**
Las credenciales de RabbitMQ están hardcodeadas en texto plano en el archivo de configuración de producción, a diferencia de las credenciales de base de datos que sí usan variables de entorno.

```yaml
# application-prod.yaml
spring:
  datasource:
    url: ${DB_URL}           # ✅ env var
    username: ${DB_USERNAME} # ✅ env var
    password: ${DB_PASSWORD} # ✅ env var
  rabbitmq:
    host: ${RABBIT_URL}
    username: api-movements  # ❌ hardcoded
    password: api-movements  # ❌ hardcoded
```

Cualquier persona con acceso al repositorio tiene las credenciales de producción de RabbitMQ.

**Corrección:**
```yaml
rabbitmq:
  host: ${RABBIT_URL}
  username: ${RABBIT_USERNAME}
  password: ${RABBIT_PASSWORD}
```

---

## Resumen STRIDE

| # | Categoría STRIDE | Componente | Severidad | Confianza |
|---|---|---|---|---|
| 1 | **E**levation of Privilege | `DEFAULT_WORKSPACE` + `WorkspaceContextService` | 🔴 Alta | 0.95 |
| 2 | **I**nformation Disclosure | WebSocket `permitAll` sin auth | 🔴 Alta | 0.98 |
| 3 | **E**levation of Privilege | `InvitationAddService` sin rol check | 🟡 Media | 0.90 |
| 4 | **T**ampering | PDF upload sin validación de tipo | 🟡 Media | 0.85 |
| 5 | **I**nformation Disclosure | RabbitMQ creds hardcodeadas en prod | 🔴 Alta | 1.00 |

**Prioridad de remediación:** Vuln 1 y Vuln 2 deben tratarse como urgentes — ambas son explotables por cualquier usuario autenticado (Vuln 1) o cualquier persona sin autenticación (Vuln 2) contra datos de producción en `movement.eva-core.com`.
