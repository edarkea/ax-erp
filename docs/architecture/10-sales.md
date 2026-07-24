# Módulo `odc-sales`

## Responsabilidad

- facturas de venta;
- líneas;
- precios;
- descuentos;
- impuestos;
- numeración;
- confirmación;
- anulación;
- estado contable desacoplado.

Depende de:

- `odc-organization`
- `odc-reference-data`
- `odc-party`
- `odc-tax`
- `odc-catalog`
- `odc-pricing`
- `odc-document-control`

No depende de `odc-accounting`.

## Modelos

### SalesInvoice

Tabla: `odc_sales_invoice`

Campos:

- `company`.
- `customerParty`.
- `documentType`.
- `documentNo`.
- `documentDate`.
- `dueDate`.
- `branch`, opcional.
- `currency`.
- `priceList`, opcional.
- `paymentMode`.
- `status`.
- `accountingStatus`.
- `description`.
- `subtotal`.
- `discountTotal`.
- `taxTotal`.
- `grandTotal`.
- `emissionEstablishment`, opcional.
- `pointOfSale`, opcional.
- `documentSeries`, opcional.
- `documentSequenceReservation`, opcional.
- `sequenceNumber`.
- `confirmedAt`, `confirmedBy`.
- `cancelledAt`, `cancelledBy`.
- `cancellationReason`.

Estados comerciales:

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`

Estados contables:

- `NOT_POSTED`
- `POSTING`
- `POSTED`
- `REVERSED`
- `ERROR`

Empresa:

- sí, raíz.

Validaciones:

- cliente de la misma compañía y con rol de cliente;
- sucursal de la misma compañía;
- moneda activa;
- lista de precios de la misma compañía y moneda;
- vencimiento no anterior a fecha;
- configuración documental coherente;
- establecimiento pertenece a la sucursal;
- punto pertenece al establecimiento;
- serie pertenece al establecimiento y punto;
- documento confirmado requiere número y reserva consumida;
- usuario con acceso a compañía y sucursal.

Reglas:

- solo borrador es editable;
- totales siempre recalculados en servidor;
- confirmación reserva/consume número de forma transaccional;
- cancelación no archiva ni elimina;
- no contiene FK a `JournalEntry`;
- la contabilidad se refleja por `accountingStatus`;
- una factura confirmada conserva snapshots.

### SalesInvoiceLine

Tabla: `odc_sales_invoice_line`

Campos:

- `salesInvoice`.
- `lineNo`.
- `item`, opcional.
- `itemSkuSnapshot`, opcional.
- `description`.
- `uomCodeSnapshot`, opcional.
- `quantity`.
- `unitPrice`.
- `discountAmount`.
- `taxCategory`, opcional.
- `taxCodeSnapshot`.
- `taxRate`.
- `lineSubtotal`.
- `lineTax`.
- `lineTotal`.

Empresa:

- no tiene; deriva de factura.

Validaciones:

- artículo de la misma compañía;
- artículo activo al agregar;
- cantidad mayor que cero;
- precio no negativo;
- descuento entre cero y subtotal bruto;
- impuesto resuelto para la fecha;
- número de línea único;
- no editar si factura no está en borrador.

Reglas de cálculo:

```text
gross = quantity × unitPrice
lineSubtotal = gross - discountAmount
lineTax = round(lineSubtotal × taxRate / 100)
lineTotal = lineSubtotal + lineTax
```

La estrategia de redondeo debe centralizarse y usar la precisión de la moneda.

## Servicios

### SalesInvoiceCalculationService

- recalcula líneas;
- recalcula cabecera;
- aplica redondeo;
- ignora totales enviados por cliente.

### SalesInvoiceService

- crear;
- actualizar borrador;
- agregar/quitar líneas;
- confirmar;
- cancelar.

### SalesInvoiceValidationService

- consistencia multicompañía;
- fechas;
- estados;
- referencias documentales.

### SalesInvoiceNumberingService

- selecciona serie;
- reserva;
- consume;
- enlaza documento.

## Flujo de confirmación

1. bloquear factura;
2. verificar estado `DRAFT`;
3. validar cabecera y líneas;
4. resolver precios e impuestos faltantes;
5. recalcular totales;
6. guardar factura para obtener id si hace falta;
7. reservar o recuperar secuencia idempotente;
8. consumir reserva;
9. copiar número y secuencia;
10. marcar `CONFIRMED`;
11. sellar usuario y fecha;
12. commit único.

## Flujo de cancelación

1. factura confirmada;
2. validar permiso y motivo;
3. impedir si la política contable exige reversión previa;
4. marcar cancelada;
5. no liberar ni reutilizar número;
6. conservar líneas y snapshots.

## Permisos

- `odc.sales.invoice.read`
- `odc.sales.invoice.create`
- `odc.sales.invoice.write`
- `odc.sales.invoice.confirm`
- `odc.sales.invoice.cancel`
- `odc.sales.invoice.view-cost`, futuro si aplica.

## Pruebas mínimas

- cliente de otra compañía;
- artículo de otra compañía;
- lista en moneda diferente;
- cálculo y redondeo;
- no confiar en totales recibidos;
- confirmación concurrente idempotente;
- documento confirmado inmutable;
- cancelación conserva número;
- línea sin `company`;
- ausencia de FK a contabilidad.

## Criterio de cierre

Ventas opera completamente sin instalar contabilidad y produce documentos históricos consistentes.
