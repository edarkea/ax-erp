# Módulo `odc-tax`

## Responsabilidad

- catálogo tributario;
- tipos de impuesto;
- tarifas históricas;
- vigencias;
- resolución de tarifa aplicable.

Depende de `odc-reference-data`.

Los impuestos son globales y se delimitan por país, no por compañía.

## Modelos

### TaxCategory

Tabla: `odc_tax_category`

Campos:

- `country`.
- `code`.
- `name`.
- `type`.
- `calculationMethod`, opcional.
- `description`, opcional.

Tipos iniciales:

- `VAT`
- `WITHHOLDING`
- `EXCISE`
- `OTHER`

Relaciones:

- many-to-one a `Country`;
- one-to-many a `TaxRate`.

Empresa:

- no tiene.

Validaciones:

- código único activo por país y tipo;
- país activo;
- tipo mediante selección;
- no modificar país o tipo si existen usos.

### TaxRate

Tabla: `odc_tax_rate`

Campos:

- `taxCategory`.
- `rate`.
- `validFrom`.
- `validUntil`, opcional.

Empresa:

- no tiene; deriva de la categoría, que es global.

Validaciones:

- tarifa no negativa;
- `validUntil >= validFrom`;
- periodos no superpuestos para la misma categoría;
- como máximo una tarifa aplicable por fecha;
- precisión decimal definida por dominio.

Reglas:

- nunca se edita una tarifa histórica usada; se cierra su vigencia y se crea otra;
- una fecha fuera de vigencia devuelve error funcional, no cero silencioso;
- documentos guardan snapshot de código y porcentaje;
- archivar una tarifa no altera documentos anteriores.

## Servicios

### TaxRateService

- crear tarifa;
- cerrar vigencia;
- validar solapamientos;
- resolver por categoría y fecha.

### TaxCalculationService

- calcular base e importe;
- normalizar escala y redondeo;
- no depende de ventas.

## Vistas

- categoría con panel de tarifas;
- calendario o grid de vigencias;
- filtros por país y tipo.

## Permisos

- `odc.tax.read`
- `odc.tax.write`
- `odc.tax.archive`
- `odc.tax.rate.manage`

## Pruebas mínimas

- resolver tarifa exacta;
- fecha límite inclusiva;
- impedir superposición;
- permitir periodos consecutivos;
- no devolver tarifa archivada para documentos nuevos;
- mismo código permitido en países distintos.

## Criterio de cierre

Existe una única función de resolución de tarifa por fecha y ningún módulo duplica esa lógica.
