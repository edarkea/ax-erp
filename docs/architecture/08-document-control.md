# Módulo `odc-document-control`

## Responsabilidad

- establecimientos de emisión;
- puntos de emisión o venta;
- asignación de usuarios;
- series documentales;
- reserva, consumo y anulación de secuencias.

Depende de `odc-organization`.

## Modelos

### EmissionEstablishment

Tabla: `odc_emission_establishment`

Campos:

- `branch`.
- `code`.
- `name`.
- `address`.
- `isDefault`.

Empresa:

- no tiene; deriva de `branch.company`.

Validaciones:

- código único activo por sucursal o por compañía según norma definida;
- una opción predeterminada por sucursal;
- sucursal activa;
- no cambiar sucursal después de emitir.

### PointOfSale

Tabla: `odc_point_of_sale`

Campos:

- `emissionEstablishment`.
- `code`.
- `name`.
- `type`.
- `isDefault`.

Empresa:

- no tiene.

Validaciones:

- código único activo por establecimiento;
- tipo mediante selección;
- un predeterminado por establecimiento y tipo;
- establecimiento activo.

### UserPointAssignment

Tabla: `odc_user_point_assignment`

Campos:

- `user`.
- `pointOfSale`.
- `isDefault`.

Empresa:

- no tiene.

Validaciones:

- combinación usuario-punto única activa;
- usuario con acceso a la compañía y sucursal derivadas;
- un punto predeterminado por usuario, compañía y tipo;
- punto activo.

### DocumentSeries

Tabla: `odc_document_series`

Campos:

- `emissionEstablishment`.
- `pointOfSale`.
- `documentType`.
- `currentSequence`.
- `paddingLength`.
- `isAutomatic`.
- `displayPattern`.

Empresa:

- no tiene.

Validaciones:

- punto pertenece al establecimiento;
- unicidad activa por establecimiento, punto y tipo;
- `currentSequence >= 0`;
- `paddingLength` entre 1 y 20;
- patrón limitado a tokens autorizados;
- serie usada no cambia de tipo ni contexto.

Reglas:

- el contador solo cambia dentro del servicio transaccional;
- no editar manualmente si es automática.

### DocumentSequenceReservation

Tabla: `odc_document_sequence_reservation`

Campos:

- `documentSeries`.
- `documentModel`.
- `documentId` Long, opcional.
- `correlationKey`, opcional.
- `sequenceNumber`.
- `documentNo`.
- `status`.
- `reservedBy`.
- `reservedAt`.
- `consumedAt`.
- `voidedAt`.
- `voidReason`.

Estados:

- `RESERVED`
- `CONSUMED`
- `VOID`

Empresa:

- no tiene.

Validaciones:

- secuencia única por serie;
- número único por serie;
- referencia por `documentId` o `correlationKey`;
- transiciones irreversibles;
- motivo requerido para anulación;
- usuario con acceso al contexto derivado.

Reglas:

- un número reservado nunca se reutiliza;
- `VOID` no vuelve a `RESERVED`;
- consumo vincula definitivamente el documento;
- reserva y actualización de contador ocurren en una sola transacción;
- usar bloqueo pesimista o mecanismo PostgreSQL seguro;
- una reejecución con la misma correlación devuelve la misma reserva.

## Servicios

- `EmissionConfigurationService`
- `UserPointAssignmentService`
- `DocumentSeriesService`
- `DocumentSequenceService`

Métodos conceptuales:

- `reserve(series, model, idOrCorrelation)`
- `consume(reservation, documentId)`
- `voidReservation(reservation, reason)`
- `formatDocumentNo(series, sequence)`

## Vistas

- establecimiento con puntos y series;
- asignación por usuario;
- monitor de reservas de solo lectura;
- acciones explícitas para anular.

## Permisos

- `odc.document.configuration.read`
- `odc.document.configuration.write`
- `odc.document.sequence.reserve`
- `odc.document.sequence.void`
- `odc.document.assignment.manage`

## Pruebas mínimas

- concurrencia genera números distintos;
- idempotencia por correlación;
- punto de otro establecimiento;
- usuario sin acceso;
- no reutilizar número anulado;
- transición consumido a anulado bloqueada salvo flujo formal;
- ningún hijo contiene `company`.

## Criterio de cierre

La numeración es transaccional, idempotente y no reutilizable. El módulo no depende de ventas.
