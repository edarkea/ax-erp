# Módulo `odc-sales-accounting`

## Responsabilidad

Integrar facturas de venta con contabilidad sin crear dependencia circular.

Depende de:

- `odc-sales`
- `odc-accounting`

## Modelos

Primera versión:

- no requiere modelos propios.

La trazabilidad se mantiene con:

- `JournalEntry.sourceModel`;
- `JournalEntry.sourceRecordId`;
- `JournalEntry.sourceDocumentNo`;
- `SalesInvoice.accountingStatus`.

Solo crear un modelo `SalesAccountingLink` si aparece una necesidad real que no pueda resolverse con la referencia de origen.

## Servicios

### SalesInvoiceAccountingService

Métodos conceptuales:

- `postInvoice(SalesInvoice invoice)`
- `findJournalEntry(SalesInvoice invoice)`
- `reverseInvoicePosting(SalesInvoice invoice, String reason)`

### SalesInvoicePostingMapper

Transforma:

- cliente;
- ingresos;
- impuestos;
- descuentos;
- cuentas por cobrar o efectivo.

### SalesInvoicePostingValidator

Valida:

- factura confirmada;
- no cancelada;
- no contabilizada;
- periodo abierto;
- configuración de todas las funciones contables;
- tercero y vencimiento cuando corresponden;
- resultado balanceado.

## Regla de idempotencia

Clave lógica:

```text
sourceModel + sourceRecordId + tipo de contabilización
```

Si existe un asiento contabilizado válido:

- no crear otro;
- sincronizar estado si fuera necesario;
- devolver el existente.

## Flujo de contabilización

1. bloquear factura;
2. comprobar `CONFIRMED`;
3. establecer `POSTING`;
4. resolver periodo;
5. resolver cuentas;
6. construir asiento borrador;
7. validar balance;
8. contabilizar asiento;
9. marcar factura `POSTED`;
10. transacción única o estrategia compensable explícita.

## Flujo de reversión

1. localizar asiento fuente;
2. validar que no esté ya revertido;
3. crear reversión contable;
4. marcar factura `REVERSED`;
5. conservar trazabilidad.

## Errores

Si falla:

- no dejar asiento parcial;
- factura pasa a `ERROR` solo cuando la transacción de negocio lo permite;
- guardar mensaje funcional no sensible;
- permitir reintento idempotente.

## Permisos

- `odc.sales.accounting.post`
- `odc.sales.accounting.reverse`
- `odc.sales.accounting.retry`

## Pruebas mínimas

- contabilización correcta;
- segundo intento no duplica;
- configuración faltante;
- periodo cerrado;
- asiento descuadrado;
- reversión;
- factura de otra compañía;
- estado sincronizado tras reintento.

## Criterio de cierre

Ventas y contabilidad continúan instalables de forma independiente; únicamente este módulo conoce ambos dominios.
