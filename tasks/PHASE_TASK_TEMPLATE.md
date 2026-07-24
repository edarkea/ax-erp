# Plantilla de tarea para Codex

Copia y completa únicamente lo necesario.

```text
Trabaja en la Fase <N>, módulo <odc-module>, capacidad <nombre>.

Lee:
- AGENTS.md
- docs/architecture/00-roadmap.md
- docs/architecture/<archivo-del-modulo>.md

Objetivo:
<resultado funcional concreto>

Alcance:
- <modelo/servicio/vista/prueba 1>
- <modelo/servicio/vista/prueba 2>

Fuera de alcance:
- no modificar otros módulos salvo la dependencia estrictamente necesaria;
- no hacer refactor general;
- no editar código generado;
- no ejecutar build global hasta validar el módulo.

Criterios de aceptación:
- <caso válido>
- <caso inválido>
- <regla multicompañía o herencia de empresa>
- <prueba y comando esperado>

Entrega:
1. implementa;
2. ejecuta generación/compilación/pruebas específicas;
3. corrige errores del alcance;
4. resume cambios, archivos y comandos;
5. detente al cumplir los criterios.
```

## Ejemplo

```text
Trabaja en la Fase 3, módulo odc-catalog, capacidad ItemCategory.

Lee:
- AGENTS.md
- docs/architecture/00-roadmap.md
- docs/architecture/06-catalog.md

Objetivo:
Implementar el árbol de categorías por compañía con validación de ciclos.

Alcance:
- dominio ItemCategory;
- selección o constantes necesarias;
- servicio de validación;
- grid, form y tree;
- permisos;
- pruebas de misma compañía, autorreferencia y ciclo.

Fuera de alcance:
- Item;
- precios;
- ventas;
- migración de datos.

Criterios de aceptación:
- código único activo por compañía;
- padre obligatorio de la misma compañía cuando exista;
- no se permite A → B → A;
- no se permite archivar una categoría con hijos activos;
- pruebas del módulo exitosas.

Entrega:
implementa y detente al cumplir estos criterios.
```
