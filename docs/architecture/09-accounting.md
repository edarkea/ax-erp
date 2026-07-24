# Módulo `odc-accounting`

## Responsabilidad

- plan de cuentas;
- roles contables;
- configuración de cuentas;
- periodos;
- asientos;
- líneas;
- contabilización;
- reversión.

Depende de:

- `odc-organization`
- `odc-reference-data`
- `odc-party`

## Modelos

### ChartAccount

Tabla: `odc_chart_account`

Campos:

- `company`.
- `code`.
- `name`.
- `accountType`.
- `normalBalance`.
- `isPosting`.
- `parent`.

Empresa:

- sí.

Validaciones:

- código único activo por compañía;
- padre de la misma compañía;
- sin ciclos;
- cuenta de movimiento no debe tener hijos activos;
- cuenta padre normalmente no es de movimiento;
- tipo y saldo normal mediante selecciones;
- no cambiar compañía.

Reglas:

- cuenta usada no se elimina;
- archivar impide nuevas líneas, no afecta asientos históricos.

### AccountingRoleDefinition

Tabla: `odc_accounting_role_definition`

Campos:

- `code`.
- `name`.
- `description`.
- `documentGroup`.
- `documentType`, opcional.
- `sideHint`.
- `requiresParty`.
- `requiresDueDate`.
- `allowManualSelection`.
- `systemDefined`.

Empresa:

- no tiene; es una definición global.

Validaciones:

- código global único;
- definiciones de sistema no se archivan ni cambian en campos estructurales;
- valores mediante selecciones.

### AccountingSetupEntry

Tabla: `odc_accounting_setup_entry`

Campos:

- `company`.
- `documentGroup`.
- `documentType`, opcional.
- `accountingRoleDefinition`.
- `account`.
- `branch`, opcional.
- `currency`, opcional.
- `priority`.
- `notes`.

Empresa:

- sí.

Validaciones:

- cuenta de la misma compañía;
- sucursal de la misma compañía;
- moneda activa;
- cuenta activa y de movimiento;
- combinación no duplicada;
- no generar ambigüedad con igual especificidad y prioridad.

Reglas de resolución:

1. compañía obligatoria;
2. coincidencia exacta de tipo de documento si existe;
3. sucursal específica sobre global;
4. moneda específica sobre global;
5. prioridad menor o mayor según convención única;
6. error si persiste empate.

### AccountingPeriod

Tabla: `odc_accounting_period`

Campos:

- `company`.
- `name`.
- `dateFrom`.
- `dateTo`.
- `status`.
- `closedAt`.
- `closedBy`.

Estados:

- `OPEN`
- `CLOSED`
- `LOCKED`

Empresa:

- sí.

Validaciones:

- rango válido;
- periodos no superpuestos para la misma compañía;
- cierre requiere usuario;
- periodo con asientos borrador puede cerrarse solo según política explícita.

Reglas:

- cerrado no acepta nuevas contabilizaciones;
- reapertura requiere permiso especial y auditoría;
- bloqueado no se reabre por operación normal.

### JournalEntry

Tabla: `odc_journal_entry`

Campos:

- `company`.
- `entryNumber`.
- `entryDate`.
- `period`.
- `status`.
- `description`.
- `sourceModel`.
- `sourceRecordId` Long.
- `sourceDocumentNo`.
- `totalDebit`.
- `totalCredit`.
- `reversedByEntry`.
- `reversalOfEntry`.
- `reversalReason`.
- `postedAt`.
- `postedBy`.

Estados:

- `DRAFT`
- `POSTED`
- `REVERSED`
- `CANCELLED`

Empresa:

- sí, raíz.

Validaciones:

- periodo de la misma compañía;
- fecha dentro del periodo;
- periodo abierto al contabilizar;
- totales derivados de líneas;
- débito igual a crédito;
- al menos dos líneas;
- número único cuando se asigna;
- fuente idempotente cuando corresponda.

Reglas:

- borrador editable;
- contabilizado inmutable;
- corregir mediante reversión;
- no relación directa con `SalesInvoice`;
- fuente mediante modelo e identificador Long.

### JournalLine

Tabla: `odc_journal_line`

Campos:

- `journalEntry`.
- `lineNo`.
- `account`.
- `debit`.
- `credit`.
- `description`.
- `party`, opcional.
- `dueDate`, opcional.

Empresa:

- no tiene; deriva de `journalEntry.company`.

Validaciones:

- cuenta de la misma compañía;
- tercero de la misma compañía;
- línea usa débito o crédito, no ambos;
- importes no negativos;
- al menos uno mayor que cero;
- número de línea único por asiento;
- requisitos de tercero y vencimiento según rol que originó la línea;
- cuenta activa y de movimiento.

## Servicios

- `ChartAccountService`
- `AccountingPeriodService`
- `AccountingSetupResolver`
- `JournalEntryService`
- `AccountingPostingService`
- `JournalReversalService`

## Transacciones

Contabilizar:

1. validar contexto y periodo;
2. recalcular totales;
3. comprobar balance;
4. asignar número;
5. cambiar estado;
6. sellar usuario y fecha;
7. persistir en una transacción.

Reversar:

1. bloquear asiento original;
2. crear nuevo asiento con débitos/créditos invertidos;
3. enlazar ambos;
4. contabilizar reverso;
5. marcar original como revertido.

## Permisos

- `odc.accounting.account.read`
- `odc.accounting.account.write`
- `odc.accounting.setup.manage`
- `odc.accounting.period.manage`
- `odc.accounting.entry.read`
- `odc.accounting.entry.write`
- `odc.accounting.entry.post`
- `odc.accounting.entry.reverse`

## Pruebas mínimas

- árbol sin ciclos;
- cuenta de otra compañía;
- periodos superpuestos;
- contabilización descuadrada;
- línea con débito y crédito;
- tercero de otra compañía;
- asiento contabilizado inmutable;
- reversión correcta;
- resolución contable ambigua;
- `JournalLine` sin `company`.

## Criterio de cierre

La contabilización y reversión son transaccionales, los totales se calculan en servidor y no existe dependencia hacia ventas.
