# Agente Codex — ODC sobre Axelor Open Platform

## 1. Misión

Desarrollar ODC como una aplicación modular sobre **Axelor Open Platform**, sin depender funcionalmente de Axelor Open Suite.

El código debe ser:

- modular por capacidad de negocio;
- sencillo de mantener;
- compatible con la versión de Axelor declarada en el proyecto;
- seguro para un entorno multicompañía;
- eficiente en consumo de contexto y ejecución;
- validado en servidor, no solamente en vistas XML.

Antes de modificar código, lee este archivo y **únicamente** la especificación del módulo y fase afectados.

---

## 2. Presupuesto de contexto

Para evitar consumo innecesario:

1. No leas todo el repositorio.
2. Usa `rg --files`, `rg` y rutas concretas.
3. Lee, en este orden:
   - `AGENTS.md`;
   - `docs/architecture/00-roadmap.md`;
   - el archivo del módulo solicitado;
   - `build.gradle` del módulo;
   - archivos directamente afectados.
4. No inspecciones:
   - `build/`;
   - `.gradle/`;
   - código generado;
   - dependencias descargadas;
   - módulos no relacionados con la tarea.
5. No ejecutes un build global por defecto.
6. Ejecuta primero generación, compilación y pruebas del módulo afectado.
7. Ejecuta la validación global solo al cerrar una fase o cuando una dependencia compartida cambie.
8. No repitas en la respuesta final el contenido completo de los archivos modificados.
9. Resume:
   - qué cambió;
   - archivos tocados;
   - validaciones realizadas;
   - riesgos o trabajo pendiente.

---

## 3. Flujo obligatorio por tarea

### Paso 1: delimitar

Antes de programar, identifica internamente:

- módulo;
- fase;
- modelos afectados;
- dependencias directas;
- reglas de negocio aplicables;
- pruebas mínimas.

No amplíes el alcance sin necesidad.

### Paso 2: inspeccionar

Lee solamente:

- especificación del módulo;
- dominios XML afectados;
- vistas y acciones relacionadas;
- servicios/repositorios relacionados;
- pruebas relacionadas.

### Paso 3: implementar una rebanada vertical

Cuando sea razonable, una tarea debe incluir:

1. modelo XML;
2. selección XML;
3. repositorio personalizado solo si hace falta;
4. servicio;
5. controlador delgado;
6. vistas;
7. permisos;
8. prueba;
9. migración o índice especial.

No dejes la regla principal únicamente en la interfaz.

### Paso 4: validar

Usa los comandos reales del proyecto. Descubre el nombre de la tarea Gradle si es necesario, pero no ejecutes búsquedas repetidas.

Orden recomendado:

1. generación de código del módulo, si cambiaron dominios;
2. compilación del módulo;
3. pruebas específicas;
4. pruebas del módulo;
5. build global únicamente al cerrar la fase.

### Paso 5: informar

Respuesta final breve:

- resultado;
- archivos principales;
- comandos ejecutados;
- errores pendientes;
- siguiente tarea lógica, solo una.

---

## 4. Reglas Axelor

1. Los modelos se definen en XML.
2. No edites clases Java generadas.
3. Usa el XSD correspondiente a la versión Axelor declarada en el proyecto.
4. Una entidad por archivo XML, salvo selecciones pequeñas relacionadas.
5. Usa el `Long id`, `version` y `archived` nativos.
6. Para entidades empresariales auditables, extiende `com.axelor.auth.db.AuditableModel`.
7. Usa `createdBy`, `createdOn`, `updatedBy` y `updatedOn` nativos.
8. No recrees `User`, `Role`, `Group`, `Permission` ni sesiones.
9. Las referencias a usuarios apuntan a `com.axelor.auth.db.User`.
10. Los controladores no contienen reglas de negocio; delegan a servicios.
11. Los servicios públicos usan interfaces cuando representan una capacidad reutilizable.
12. Las operaciones con cambios múltiples deben ser transaccionales.
13. Los repositorios personalizados contienen consultas reutilizables, no reglas de negocio.
14. Las vistas aplican dominios para mejorar la experiencia, pero el servicio repite toda validación crítica.
15. No uses eliminación física para documentos o catálogos usados históricamente.
16. Para unicidad solo entre registros activos, usa migración PostgreSQL con índice único parcial.
17. No simules unicidad activa con `(campo, archived)`.
18. No agregues relaciones inversas desde módulos inferiores hacia módulos superiores.
19. No introduzcas dependencias circulares.
20. No uses Open Suite salvo solicitud explícita.

---

## 5. Política de empresa

Agregar `company` solo cuando el modelo sea una **raíz de agregado cuyo contenido pertenezca realmente a una empresa**.

### Tiene empresa directa

- `Branch`
- `UserCompanyAccess`
- `ApiAuditLog`, opcional
- `BulkOperationJob`, opcional
- `Party`
- `PartyTag`
- `ItemCategory`
- `Item`
- `PriceList`
- `ChartAccount`
- `AccountingSetupEntry`
- `AccountingPeriod`
- `JournalEntry`
- `SalesInvoice`

### Hereda empresa mediante su padre

- `UserBranchAccess` → `branch.company`
- `PartyRole` → `party.company`
- `PartyContactPoint` → `party.company`
- `PartyAddress` → `party.company`
- `PartyTagLink` → `party.company`
- `PriceListItem` → `priceList.company`
- `EmissionEstablishment` → `branch.company`
- `PointOfSale` → `emissionEstablishment.branch.company`
- `UserPointAssignment` → `pointOfSale...company`
- `DocumentSeries` → `emissionEstablishment.branch.company`
- `DocumentSequenceReservation` → `documentSeries...company`
- `JournalLine` → `journalEntry.company`
- `SalesInvoiceLine` → `salesInvoice.company`

### Global, sin empresa

- `Currency`
- `Country`
- `State`
- `City`
- `UnitOfMeasure`
- `TaxCategory`
- `TaxRate`
- `AccountingRoleDefinition`
- `JobExecution`

Nunca agregues `company` a una entidad hija solo para simplificar una consulta. Usa joins, consultas específicas o campos derivados.

---

## 6. Dependencias permitidas

```text
odc-reference-data
        ↓
odc-organization
        ├──────────────┬──────────────┬──────────────┐
        ↓              ↓              ↓              ↓
odc-operations      odc-party   odc-document-control odc-accounting
        │              │                              ↑
        │              └──────────────┐               │
        ↓                             ↓               │
odc-tax ───────────────────────→ odc-catalog          │
        │                             ↓               │
        └──────────────────────→ odc-pricing          │
                                      ↓               │
                               odc-sales ──────────────┘
                                      ↓
                           odc-sales-accounting
```

Dependencias concretas:

- `odc-reference-data`: plataforma.
- `odc-organization`: `odc-reference-data`.
- `odc-operations`: `odc-organization`.
- `odc-tax`: `odc-reference-data`.
- `odc-party`: `odc-organization`, `odc-reference-data`.
- `odc-catalog`: `odc-organization`, `odc-tax`.
- `odc-pricing`: `odc-organization`, `odc-reference-data`, `odc-catalog`.
- `odc-document-control`: `odc-organization`.
- `odc-accounting`: `odc-organization`, `odc-reference-data`, `odc-party`.
- `odc-sales`: `odc-organization`, `odc-reference-data`, `odc-party`, `odc-tax`, `odc-catalog`, `odc-pricing`, `odc-document-control`.
- `odc-sales-accounting`: `odc-sales`, `odc-accounting`.

Si una modificación requiere invertir una dependencia, detente y usa:

- consulta desde el módulo superior;
- referencia polimórfica;
- servicio de integración;
- evento;
- módulo puente.

---

## 7. Convenciones

### Paquetes

```text
com.odc.reference
com.odc.organization
com.odc.operations
com.odc.tax
com.odc.party
com.odc.catalog
com.odc.pricing
com.odc.document
com.odc.accounting
com.odc.sales
com.odc.sales.accounting
```

### Tablas

Conservar nombres existentes cuando sea posible:

```text
odc_company
odc_branch
odc_party
odc_item
odc_price_list
odc_sales_invoice
```

### Selecciones

Usa prefijo:

```text
odc.organization.*
odc.tax.*
odc.party.*
odc.catalog.*
odc.document.*
odc.accounting.*
odc.sales.*
```

No guardes estados o tipos como strings libres.

### Permisos

```text
odc.<module>.read
odc.<module>.write
odc.<module>.archive
odc.<module>.<business-action>
```

### Excepciones

Los mensajes deben:

- ser comprensibles para usuario;
- indicar el campo o regla;
- usar i18n;
- no exponer SQL ni detalles internos.

---

## 8. Reglas de calidad

Una tarea no está terminada cuando solo compila.

Debe cumplir, según alcance:

- generación de código correcta;
- compilación;
- prueba de caso válido;
- prueba de al menos un caso inválido;
- validación multicompañía;
- validación de archivado;
- permisos;
- vista con dominio adecuado;
- ausencia de dependencia circular;
- ausencia de cambios en código generado;
- ausencia de eliminación física accidental.

---

## 9. Prohibiciones

No:

- crear un módulo por tabla;
- duplicar la seguridad de Axelor;
- añadir `company` a todas las tablas;
- editar documentos confirmados o asientos contabilizados;
- confiar en totales enviados por el cliente;
- reutilizar números documentales anulados;
- borrar físicamente facturas, asientos o reservas;
- consultar módulos superiores desde inferiores;
- introducir una “clase utilitaria global” como contenedor de reglas;
- hacer refactorizaciones generales durante una tarea puntual;
- cambiar nombres de tablas existentes sin plan de migración;
- añadir dependencias de Open Suite por comodidad;
- ejecutar formateo masivo del repositorio.

---

## 10. Documentación de trabajo

Para cada tarea, lee solo el archivo necesario:

- `docs/architecture/00-roadmap.md`
- `docs/architecture/01-reference-data.md`
- `docs/architecture/02-organization.md`
- `docs/architecture/03-operations.md`
- `docs/architecture/04-tax.md`
- `docs/architecture/05-party.md`
- `docs/architecture/06-catalog.md`
- `docs/architecture/07-pricing.md`
- `docs/architecture/08-document-control.md`
- `docs/architecture/09-accounting.md`
- `docs/architecture/10-sales.md`
- `docs/architecture/11-sales-accounting.md`
- `docs/architecture/12-migration-hardening.md`

Usa `tasks/PHASE_TASK_TEMPLATE.md` para preparar cada instrucción a Codex.
