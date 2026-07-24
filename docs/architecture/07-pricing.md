# Módulo `odc-pricing`

## Responsabilidad

- listas de precios;
- precios por artículo;
- moneda;
- vigencia;
- resolución de precio.

Depende de:

- `odc-organization`
- `odc-reference-data`
- `odc-catalog`

## Modelos

### PriceList

Tabla: `odc_price_list`

Campos:

- `company`.
- `name`.
- `currency`.
- `validFrom`, opcional.
- `validUntil`, opcional.
- `priority`, por defecto 100.
- `pricesIncludeTax`, por defecto false.

Relaciones:

- many-to-one a `Currency`;
- one-to-many a `PriceListItem`.

Empresa:

- sí, raíz.

Validaciones:

- nombre único activo por compañía;
- moneda activa;
- fecha final no anterior a inicial;
- prioridad no negativa;
- usuario con acceso.

### PriceListItem

Tabla: `odc_price_list_item`

Campos mínimos:

- `priceList`.
- `item`.
- `price`.

Campos extensibles desde el inicio si el alcance los acepta:

- `minimumQuantity`;
- `maximumQuantity`;
- `validFrom`;
- `validUntil`.

Empresa:

- no tiene; deriva de `priceList.company`.

Validaciones:

- artículo de la misma compañía de la lista;
- precio no negativo;
- artículo activo al crear;
- rango de cantidades válido;
- rango de fechas contenido o compatible con la lista;
- no duplicar regla activa equivalente.

Reglas:

- modificar una lista no altera facturas existentes;
- el resultado de precio se copia a la línea de venta;
- la lista archivada no se usa en documentos nuevos;
- una línea no se elimina físicamente si se necesita trazabilidad; se archiva.

## Resolución de precio

Entrada:

- compañía;
- artículo;
- moneda;
- fecha;
- cantidad;
- lista explícita opcional.

Orden:

1. validar compañía y artículo;
2. usar lista explícita válida;
3. filtrar líneas vigentes;
4. elegir el rango de cantidad más específico;
5. resolver empate por prioridad;
6. error si continúa ambiguo.

Nunca seleccionar silenciosamente una regla ambigua.

## Servicios

- `PriceListService`
- `PriceListItemService`
- `PriceResolverService`
- `PriceValidationService`

## Vistas

- lista con líneas embebidas;
- dominios por empresa;
- selector de artículos de la misma empresa;
- moneda global.

## Permisos

- `odc.pricing.read`
- `odc.pricing.write`
- `odc.pricing.archive`
- `odc.pricing.resolve`

## Pruebas mínimas

- artículo de otra compañía;
- precio negativo;
- lista fuera de vigencia;
- resolución por cantidad;
- empate ambiguo;
- no existe `company` en `PriceListItem`;
- factura conserva precio aunque cambie la lista.

## Criterio de cierre

Existe un único `PriceResolverService` y ventas no consulta directamente repositorios de líneas de precio.
