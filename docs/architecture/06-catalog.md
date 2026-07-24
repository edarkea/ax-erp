# Módulo `odc-catalog`

## Responsabilidad

- unidades de medida;
- categorías de artículos;
- productos y servicios;
- datos tributarios predeterminados del artículo.

Depende de:

- `odc-organization`
- `odc-tax`

## Modelos

### UnitOfMeasure

Tabla: `odc_unit_of_measure`

Campos:

- `code`.
- `name`.
- `symbol`, opcional.
- `decimalPrecision`, por defecto 4.

Empresa:

- no tiene; es un referencial global.

Validaciones:

- código global único;
- precisión entre 0 y 8;
- no archivar si existen artículos activos que la usan;
- no duplicar una unidad archivada: restaurar.

### ItemCategory

Tabla: `odc_item_category`

Campos:

- `company`.
- `code`.
- `name`.
- `parent`, opcional.
- `sequence`, opcional.

Relaciones:

- autorreferencia padre-hijos;
- one-to-many conceptual a artículos, sin necesidad de colección si genera acoplamiento innecesario.

Empresa:

- sí.

Validaciones:

- código único activo por compañía;
- padre de la misma compañía;
- no ser su propio padre;
- no crear ciclos;
- no archivar con hijos o artículos activos;
- usuario con acceso a la compañía.

### Item

Tabla: `odc_item`

Campos:

- `company`.
- `itemType`.
- `sku`.
- `name`.
- `description`.
- `barcode`.
- `uom`.
- `taxCategory`.
- `category`.

Tipos:

- `PRODUCT`
- `SERVICE`

Empresa:

- sí, raíz.

Validaciones:

- SKU único activo por compañía;
- código de barras único activo por compañía cuando no es nulo;
- categoría de la misma compañía;
- unidad activa;
- impuesto activo;
- país del impuesto compatible con país de la compañía;
- producto requiere unidad;
- servicio puede permitir unidad opcional;
- nombre y SKU normalizados.

Reglas:

- no contiene colecciones inversas de precios ni facturas;
- archivar impide nuevas ventas, pero preserva líneas históricas;
- no cambiar compañía después de usar el artículo;
- impuestos son valores predeterminados, no sustituyen snapshots documentales.

## Índices especiales

PostgreSQL:

```sql
CREATE UNIQUE INDEX ux_odc_item_company_sku_active
ON odc_item(company_id, sku)
WHERE archived = FALSE;

CREATE UNIQUE INDEX ux_odc_item_company_barcode_active
ON odc_item(company_id, barcode)
WHERE archived = FALSE AND barcode IS NOT NULL;
```

Aplicar estrategia equivalente a categoría.

## Servicios

- `UnitOfMeasureService`
- `ItemCategoryService`
- `ItemService`
- `CatalogValidationService`

## Vistas

- unidad: grid/form global;
- categoría: tree/grid/form filtrado por empresa;
- artículo: grid/form con dominios de categoría e impuesto;
- campos condicionales según tipo.

## Permisos

- `odc.catalog.uom.read`
- `odc.catalog.uom.write`
- `odc.catalog.category.manage`
- `odc.catalog.item.read`
- `odc.catalog.item.write`
- `odc.catalog.item.archive`

## Pruebas mínimas

- ciclos de categoría;
- padre de otra empresa;
- SKU repetido activo;
- reutilización de SKU tras múltiples archivados permitida por índice parcial;
- barcode nulo múltiple;
- producto sin unidad;
- impuesto de país incompatible;
- artículo archivado no disponible en selector de ventas.

## Criterio de cierre

El catálogo funciona de forma independiente de precios y ventas, y solo `Item` e `ItemCategory` tienen empresa.
