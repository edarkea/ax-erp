# Módulo `odc-reference-data`

## Responsabilidad

Datos universales reutilizables por todas las empresas:

- monedas;
- países;
- provincias, departamentos o estados;
- ciudades.

No tiene dependencia de `odc-organization`.

## Modelos

### Currency

Tabla sugerida: `odc_currency`

Campos:

- `code`: código ISO de tres caracteres.
- `name`.
- `symbol`.
- `decimalPlaces`: entero, por defecto 2.

Relaciones:

- ninguna obligatoria.

Empresa:

- no tiene.

Validaciones:

- código requerido, trim y mayúsculas;
- longitud 3;
- `decimalPlaces` entre 0 y 6;
- código globalmente único;
- no permitir duplicar una moneda archivada con el mismo código: se restaura la existente.

Reglas:

- una moneda usada no se elimina físicamente;
- archivar evita nuevas selecciones, pero conserva documentos históricos.

### Country

Tabla: `odc_country`

Campos:

- `code`: ISO 3166.
- `name`.
- `phoneCode`.
- `defaultCurrency`.

Relaciones:

- many-to-one a `Currency`;
- one-to-many a `State`.

Empresa:

- no tiene.

Validaciones:

- código global único;
- moneda activa;
- `phoneCode` no debe ser único, porque puede compartirse;
- nombre requerido.

Reglas:

- no archivar con estados activos salvo operación administrativa explícita;
- el país puede definir la moneda sugerida, no obligatoria para todos los documentos.

### State

Tabla: `odc_state`

Campos:

- `code`.
- `name`.
- `country`.

Relaciones:

- many-to-one a `Country`;
- one-to-many a `City`.

Empresa:

- no tiene.

Validaciones:

- `country` requerido;
- código único por país;
- país no archivado;
- no archivar con ciudades activas.

### City

Tabla: `odc_city`

Campos:

- `code`.
- `name`.
- `state`.

Relaciones:

- many-to-one a `State`.

Empresa:

- no tiene.

Validaciones:

- código único por estado;
- estado y país activos;
- nombre requerido.

## Servicios

- `ReferenceDataNormalizationService`
- `CurrencyService`
- `GeographyService`

Responsabilidades:

- normalizar códigos;
- validar jerarquía;
- impedir archivado inseguro;
- resolver país/estado/ciudad.

## Vistas

- moneda: grid y form;
- país: form con panel de estados;
- estado: form con panel de ciudades;
- selector de ciudad con búsqueda por ciudad, estado y país.

## Permisos

- `odc.reference.read`
- `odc.reference.write`
- `odc.reference.archive`

## Pruebas mínimas

- no duplicar código de moneda;
- permitir países con el mismo prefijo telefónico;
- no repetir código de estado dentro del mismo país;
- permitir el mismo código de estado en países diferentes;
- no crear ciudad en estado archivado;
- archivado preserva referencias históricas.

## Criterio de cierre

Los modelos son globales, no contienen `company`, sus vistas filtran archivados y la jerarquía geográfica queda validada en servicio.
