# Módulo `odc-operations`

## Responsabilidad

Infraestructura operativa propia de ODC:

- auditoría de solicitudes e integraciones;
- operaciones masivas;
- trazabilidad de trabajos;
- idempotencia;
- progreso y errores.

Depende de `odc-organization`.

No reemplaza la auditoría nativa de entidades de Axelor.

## Modelos

### ApiAuditLog

Tabla: `odc_api_audit_log`

Campos:

- `user`, opcional;
- `company`, opcional;
- `action`;
- `entityModel`, opcional;
- `entityId` Long, opcional;
- `httpMethod`;
- `path`;
- `statusCode`;
- `metadata` JSON;
- `occurredOn`.

Empresa:

- opcional, porque una solicitud técnica puede no tener contexto.

Reglas:

- inmutable después de persistir;
- sin `archived`;
- no guardar contraseñas, tokens, certificados ni payloads sensibles;
- metadatos con tamaño máximo;
- eliminación solo por política de retención.

### BulkOperationJob

Tabla: `odc_bulk_operation_job`

Campos:

- `operationType`;
- `moduleKey`;
- `entityModel`;
- `company`, opcional;
- `requestedBy`;
- `status`;
- contadores;
- `query`, `selection`, `candidateIds`, `results`;
- error;
- fechas;
- worker y lease.

Estados:

- `QUEUED`
- `RUNNING`
- `DONE`
- `FAILED`
- `CANCELLED`

Empresa:

- opcional, obligatoria cuando el modelo objetivo es empresarial.

Validaciones:

- transición de estado válida;
- contadores no negativos;
- procesados no superan total;
- modelos permitidos mediante lista blanca;
- usuario con acceso a la compañía;
- operación permitida para el estado de la entidad.

Reglas:

- archivar es la operación masiva predeterminada;
- eliminación física requiere permiso administrativo especial;
- trabajo reanudable e idempotente cuando sea posible;
- los resultados parciales se registran por identificador.

### JobExecution

Tabla: `odc_job_execution`

Campos:

- `queueName`;
- `jobName`;
- `correlationId`;
- `idempotencyKey`;
- `status`;
- `payload`;
- `result`;
- `errorMessage`;
- `startedAt`;
- `finishedAt`.

Empresa:

- no tiene; puede incluir contexto en payload normalizado si es necesario.

Validaciones:

- `idempotencyKey` global única;
- estados válidos;
- finalizado requiere `finishedAt`;
- payload sin secretos.

Reglas:

- una clave ya completada devuelve el resultado anterior;
- una ejecución activa con la misma clave no inicia otra;
- retención configurable.

## Servicios

- `ApiAuditService`
- `BulkOperationService`
- `JobExecutionService`
- `IdempotencyService`

## Permisos

- `odc.operations.audit.read`
- `odc.operations.job.read`
- `odc.operations.job.run`
- `odc.operations.job.cancel`
- `odc.operations.physical-delete`

## Pruebas mínimas

- auditoría elimina secretos;
- no duplicar ejecución por idempotencia;
- transiciones inválidas fallan;
- usuario sin empresa no lanza operación empresarial;
- cancelación no cambia trabajo terminado;
- progreso coherente.

## Criterio de cierre

La infraestructura es genérica, pero no conoce reglas internas de ventas, catálogo o contabilidad; invoca capacidades públicas de esos módulos.
