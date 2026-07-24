# Hoja de ruta ODC sobre Axelor

## Objetivo

Migrar el dominio Prisma de ODC a módulos Axelor fijos, organizados por negocio y sin duplicar infraestructura nativa.

## Principios

- `Long id`, `version` y `archived` nativos.
- Auditoría nativa de Axelor.
- PostgreSQL.
- Empresa únicamente en raíces de agregado.
- Datos universales sin empresa.
- Servicios como frontera de negocio.
- Vistas XML como interfaz, no como única validación.
- Índices únicos parciales para unicidad activa.
- Módulos inferiores no conocen módulos superiores.

## Módulos definitivos

1. `odc-reference-data`
2. `odc-organization`
3. `odc-operations`
4. `odc-tax`
5. `odc-party`
6. `odc-catalog`
7. `odc-pricing`
8. `odc-document-control`
9. `odc-accounting`
10. `odc-sales`
11. `odc-sales-accounting`

## Fases

### Fase 0 — Esqueleto y estándares

Entregables:

- todos los subproyectos Gradle creados;
- dependencias sin ciclos;
- paquetes y recursos base;
- convenciones;
- perfiles de configuración;
- conexión PostgreSQL;
- pruebas de arranque;
- permisos base;
- estrategia de migraciones.

Criterio de cierre:

- la aplicación inicia;
- todos los módulos aparecen instalables;
- un build limpio completa;
- ninguna entidad de negocio todavía es necesaria.

### Fase 1 — Referenciales y organización

Módulos:

- `odc-reference-data`
- `odc-organization`

Entregables:

- moneda y geografía;
- compañía y sucursal;
- acceso de usuario a compañías y sucursales;
- resolución de empresa activa;
- filtros de seguridad organizacional.

### Fase 2 — Operaciones y tributación

Módulos:

- `odc-operations`
- `odc-tax`

Entregables:

- auditoría API propia;
- trabajos masivos y ejecuciones idempotentes;
- categorías y tarifas tributarias con vigencia;
- resolución de tarifa por fecha.

### Fase 3 — Terceros y catálogo

Módulos:

- `odc-party`
- `odc-catalog`

Entregables:

- terceros, roles, contactos, direcciones y etiquetas;
- unidades, categorías y artículos;
- validaciones multicompañía;
- índices únicos activos.

### Fase 4 — Precios y control documental

Módulos:

- `odc-pricing`
- `odc-document-control`

Entregables:

- listas y líneas de precios;
- establecimientos, puntos de emisión y series;
- asignación de usuarios;
- reserva concurrente y no reutilizable de secuencias.

### Fase 5 — Contabilidad

Módulo:

- `odc-accounting`

Entregables:

- plan de cuentas;
- roles y configuración contable;
- periodos;
- asientos y líneas;
- contabilización, cierre y reversión.

### Fase 6 — Ventas

Módulo:

- `odc-sales`

Entregables:

- factura y líneas;
- cálculo de precios, descuentos e impuestos;
- numeración;
- confirmación y anulación;
- snapshots históricos.

### Fase 7 — Integración ventas-contabilidad

Módulo:

- `odc-sales-accounting`

Entregables:

- traducción de factura a asiento;
- idempotencia;
- actualización de estado contable;
- reversión coordinada.

### Fase 8 — Migración y endurecimiento

Entregables:

- importación desde Prisma;
- reconciliación;
- rendimiento;
- permisos finales;
- API y OpenAPI;
- pruebas integrales;
- respaldo y recuperación.

## Política de implementación por módulo

Cada módulo se implementa en cuatro cortes:

1. **Dominio**: XML, selecciones, generación y migraciones.
2. **Servicios**: reglas, transacciones y consultas.
3. **Interfaz y seguridad**: vistas, acciones, menús y permisos.
4. **Pruebas**: unitarias, integración y aceptación.

No avances al siguiente módulo de la fase si el anterior no cumple sus criterios de cierre.
