# Módulo `odc-organization`

## Responsabilidad

- compañías;
- sucursales;
- acceso de usuarios;
- empresa y sucursal predeterminadas;
- contexto organizacional activo;
- preferencias ODC del usuario.

Depende de `odc-reference-data`.

## Modelos

### Company

Tabla: `odc_company`

Campos:

- `code`.
- `name`.
- `legalName`, opcional.
- `taxId`, opcional inicialmente.
- `country`.
- `defaultCurrency`.
- `timezone`.
- `locale`.
- `active`, si se desea separar habilitación de archivado.

Relaciones:

- many-to-one a `Country`;
- many-to-one a `Currency`;
- one-to-many a `Branch`;
- one-to-many a `UserCompanyAccess`.

Empresa:

- no tiene; es la raíz organizacional.

Validaciones:

- código único activo;
- país y moneda activos;
- zona horaria válida;
- no archivar si existen documentos operativos abiertos;
- `taxId` normalizado según estrategia de país.

Reglas:

- una compañía archivada no puede ser seleccionada como contexto;
- no se eliminan compañías físicamente;
- la moneda predeterminada solo sugiere valores.

### Branch

Tabla: `odc_branch`

Campos:

- `company`.
- `code`.
- `name`.
- `city`, opcional.
- `address`, opcional.
- `isDefault`.

Relaciones:

- many-to-one a `Company`;
- many-to-one a `City`;
- one-to-many a `UserBranchAccess`;
- será referenciada por emisión, contabilidad y ventas.

Empresa:

- sí, directa y obligatoria.

Validaciones:

- código único activo por compañía;
- ciudad activa;
- solo una sucursal predeterminada por compañía;
- no cambiar compañía cuando ya tenga operaciones;
- no archivar la última sucursal activa si la empresa exige sucursal.

### UserCompanyAccess

Tabla: `odc_user_company_access`

Campos:

- `user`.
- `company`.
- `isDefault`.
- `active`.

Relaciones:

- many-to-one a `com.axelor.auth.db.User`;
- many-to-one a `Company`.

Empresa:

- sí, mediante el campo `company`.

Validaciones:

- combinación usuario-compañía única activa;
- solo una compañía predeterminada por usuario;
- usuario y compañía activos.

Reglas:

- retirar acceso no elimina datos;
- un usuario sin acceso no puede consultar ni operar registros de la empresa.

### UserBranchAccess

Tabla: `odc_user_branch_access`

Campos:

- `user`.
- `branch`.
- `isDefault`.
- `active`.

Relaciones:

- many-to-one a usuario Axelor;
- many-to-one a `Branch`.

Empresa:

- no tiene; deriva de `branch.company`.

Validaciones:

- combinación usuario-sucursal única;
- debe existir acceso del usuario a la compañía de la sucursal;
- solo una sucursal predeterminada por usuario y compañía;
- no asignar sucursal archivada.

### OdcUserPreference

Tabla: `odc_user_preference`

Campos:

- `user`.
- `theme`.
- `locale`.
- `navigationMode`.
- `profileImage`, opcional mediante `MetaFile`.

Empresa:

- no tiene.

Validaciones:

- un registro por usuario;
- valores mediante selecciones.

## Servicios

### ActiveOrganizationService

Métodos conceptuales:

- `getActiveCompany()`
- `requireActiveCompany()`
- `getActiveBranch()`
- `requireCompanyAccess(User, Company)`
- `requireBranchAccess(User, Branch)`

Orden de resolución de empresa:

1. contexto explícito válido;
2. preferencia o acceso predeterminado;
3. única compañía accesible;
4. error funcional.

### OrganizationAccessService

- conceder y retirar acceso;
- validar predeterminados;
- devolver compañías y sucursales disponibles.

### CompanyService y BranchService

- normalización;
- archivo seguro;
- consistencia geográfica.

## Seguridad

Toda consulta de un modelo con empresa debe filtrar por compañías accesibles.

La vista ayuda con dominios, pero cada servicio vuelve a verificar acceso.

## Permisos

- `odc.organization.company.read`
- `odc.organization.company.write`
- `odc.organization.branch.read`
- `odc.organization.branch.write`
- `odc.organization.access.manage`

## Pruebas mínimas

- usuario con una compañía la recibe automáticamente;
- usuario con varias compañías y ninguna predeterminada recibe error;
- no asignar sucursal sin acceso a su compañía;
- solo una compañía predeterminada;
- solo una sucursal predeterminada por compañía;
- impedir operación sobre compañía no accesible;
- no cambiar la empresa de una sucursal usada.

## Criterio de cierre

Existe un servicio único y probado para resolver y validar contexto organizacional. Ningún módulo inventa su propia manera de obtener la empresa activa.
