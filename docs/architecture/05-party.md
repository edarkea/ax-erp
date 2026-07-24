# Módulo `odc-party`

## Responsabilidad

Terceros de una compañía:

- personas y organizaciones;
- identificación tributaria;
- roles;
- contactos;
- direcciones;
- etiquetas.

Depende de:

- `odc-organization`
- `odc-reference-data`

## Modelos

### Party

Tabla: `odc_party`

Campos:

- `company`.
- `partyType`.
- `displayName`.
- `legalName`, opcional.
- `taxType`.
- `taxId`.
- `notes`, opcional.

Tipos:

- `PERSON`
- `ORGANIZATION`

Relaciones:

- one-to-many a roles;
- one-to-many a contactos;
- one-to-many a direcciones;
- one-to-many a enlaces de etiquetas.

Empresa:

- sí, raíz del agregado.

Validaciones:

- identificación única activa por compañía, tipo tributario e identificación;
- nombres requeridos según tipo;
- trim y normalización de identificación;
- usuario con acceso a la empresa;
- no guardar relaciones inversas a ventas o contabilidad.

Reglas:

- un tercero puede ser cliente y proveedor mediante roles;
- archivar no borra sus datos ni documentos;
- módulos superiores deciden si existen operaciones que bloquean el archivado.

### PartyTag

Tabla: `odc_party_tag`

Campos:

- `company`.
- `name`.

Empresa:

- sí, porque las etiquetas son vocabulario interno de cada compañía.

Validaciones:

- nombre único activo por compañía;
- no enlazar a terceros de otra compañía.

### PartyTagLink

Tabla: `odc_party_tag_link`

Campos:

- `party`.
- `tag`.

Empresa:

- no tiene; deriva de `party.company`.

Validaciones:

- combinación única;
- `party.company == tag.company`;
- ambos activos.

### PartyRole

Tabla: `odc_party_role`

Campos:

- `party`.
- `roleType`.
- `isDefault`.

Roles iniciales:

- `CUSTOMER`
- `SUPPLIER`
- `EMPLOYEE`
- `OTHER`

Empresa:

- no tiene; deriva de `party`.

Validaciones:

- un rol de cada tipo por tercero;
- predeterminado coherente con el tipo;
- no crear en tercero archivado.

### PartyContactPoint

Tabla: `odc_party_contact_point`

Campos:

- `party`.
- `type`.
- `value`.
- `label`.
- `isPrimary`.

Tipos:

- `EMAIL`
- `PHONE`
- `MOBILE`
- `WEB`
- `OTHER`

Empresa:

- no tiene.

Validaciones:

- formato según tipo;
- un contacto principal por tercero y tipo;
- valor requerido;
- no imponer unicidad global de teléfono o correo.

### PartyAddress

Tabla: `odc_party_address`

Campos:

- `party`.
- `city`.
- `line1`.
- `line2`.
- `postalCode`.
- `isBillingDefault`.
- `isShippingDefault`.

Empresa:

- no tiene.

Validaciones:

- ciudad activa;
- una dirección predeterminada de facturación;
- una dirección predeterminada de envío;
- línea principal requerida.

## Servicios

- `PartyService`
- `PartyRoleService`
- `PartyContactService`
- `PartyAddressService`
- `PartyTagService`

El servicio raíz coordina archivado y valores predeterminados.

## Vistas

- grid de terceros;
- form con pestañas: general, roles, contactos, direcciones, etiquetas;
- dominios por empresa activa;
- selector de ciudad con contexto geográfico.

## Permisos

- `odc.party.read`
- `odc.party.write`
- `odc.party.archive`
- `odc.party.tag.manage`

## Pruebas mínimas

- identificación única por compañía;
- mismo tercero permitido en compañías distintas;
- no enlazar etiqueta de otra compañía;
- un contacto principal por tipo;
- una dirección predeterminada por finalidad;
- children no contienen campo `company`;
- archivado no elimina hijos físicamente.

## Criterio de cierre

`Party` es la única raíz empresarial del agregado y los módulos de ventas/contabilidad solo apuntan hacia ella.
