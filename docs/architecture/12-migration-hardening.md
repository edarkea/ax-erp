# Fase de migración y endurecimiento

## 1. Mapeo de identidad

Prisma usa CUID; Axelor usa `Long`.

Crear tablas temporales o archivos de mapeo:

```text
legacy_model
legacy_id
axelor_id
```

No conservar CUID como clave primaria.

`legacyId` puede mantenerse temporalmente durante la migración y retirarse después de reconciliar, o conservarse oculto si existe integración externa.

## 2. Orden de importación

1. usuarios Axelor y equivalencias;
2. moneda y geografía;
3. compañías y sucursales;
4. accesos de usuario;
5. tributación;
6. terceros;
7. catálogo;
8. precios;
9. control documental;
10. contabilidad;
11. ventas;
12. integración ventas-contabilidad;
13. auditoría y operaciones, solo si se decide migrarlas.

## 3. Auditoría

Mapeo:

- `createdAt` → `createdOn`;
- `updatedAt` → `updatedOn`;
- `createdBy String` → usuario Axelor cuando exista;
- `updatedBy String` → usuario Axelor;
- `deletedAt != null` → `archived = true`.

Los campos `deletedBy` y `deletedAt` históricos pueden guardarse en columnas de migración solo si tienen valor legal u operativo. Para nuevos registros se usa la forma nativa de Axelor.

## 4. Tablas técnicas

No migrar por defecto:

- sesiones refresh;
- roles y permisos propios;
- relaciones propias de seguridad.

Evaluar retención de:

- auditoría API;
- trabajos masivos;
- ejecuciones.

## 5. Relaciones eliminadas

No recrear colecciones inversas que generan ciclos:

- `Party.salesInvoices`;
- `Party.journalLines`;
- `Item.priceListItems`;
- `Item.salesInvoiceLines`;
- `JournalEntry.postedSalesInvoices`;
- relaciones desde control documental hacia facturas.

Las relaciones many-to-one desde módulos superiores son suficientes.

## 6. Índices únicos activos

Crear migraciones PostgreSQL para:

- compañía por código;
- sucursal por compañía y código;
- tercero por compañía e identificación;
- etiqueta por compañía y nombre;
- categoría por compañía y código;
- artículo por compañía y SKU;
- artículo por compañía y barcode no nulo;
- lista por compañía y nombre;
- cuenta por compañía y código;
- línea por padre y número;
- factura por compañía y número.

Ejemplo:

```sql
CREATE UNIQUE INDEX ux_odc_item_company_sku_active
ON odc_item(company_id, sku)
WHERE archived = FALSE;
```

## 7. Reconciliación

Comparar:

- conteos por modelo;
- registros archivados;
- importes por factura;
- débitos y créditos;
- números documentales;
- reservas;
- relaciones huérfanas;
- identificaciones duplicadas;
- códigos duplicados antes de crear índices.

## 8. Seguridad

- permisos por módulo;
- acceso por compañía;
- acceso por sucursal;
- pruebas de aislamiento;
- API con filtros de servidor;
- ningún dominio de vista sustituye autorización.

## 9. Rendimiento

Medir antes de optimizar.

Índices esperados:

- FKs;
- estado y fecha en documentos;
- compañía + código;
- fuente contable;
- serie + secuencia;
- trabajos por estado y fecha.

Evitar:

- company duplicada en líneas;
- colecciones one-to-many gigantes en forms;
- eager loading;
- consultas N+1;
- dashboards antes de estabilizar transacciones.

## 10. Respaldo y recuperación

Antes de migrar:

- respaldo completo;
- ensayo en copia;
- script reversible cuando sea posible;
- bitácora de errores;
- reejecución idempotente.

## 11. Criterios finales

- build limpio;
- migración repetible;
- cero relaciones huérfanas;
- asientos balanceados;
- facturas reconciliadas;
- secuencias sin duplicados;
- aislamiento multicompañía probado;
- permisos probados;
- documentación actualizada.
