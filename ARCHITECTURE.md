# Arquitectura del backend

El backend aplica arquitectura hexagonal por contexto funcional. Las dependencias
siempre apuntan hacia el caso de uso; Spring, HTTP, JPA y MySQL permanecen en los
adaptadores externos.

## Catálogo

```
HTTP -> adapter.in.web -> application -> port.out <- adapter.out.persistence -> JPA/MySQL
```

- `catalog/application/` contiene los casos de uso sin dependencias de Spring.
- `catalog/application/port/out/` declara los contratos que la aplicación necesita.
- `catalog/adapter/in/` contiene los adaptadores de entrada, DTOs HTTP y el
  ensamblaje de dependencias de Spring.
- `catalog/adapter/out/persistence/` contiene el repositorio Spring Data y su
  adaptador. Ningún controlador ni caso de uso lo importa directamente.
- `catalog/Product` conserva las reglas del producto: actualización, disponibilidad
  y control de inventario.

Los pedidos y el flujo QR consumen `ProductCatalogPort`; por tanto, no dependen de
Spring Data ni de MySQL para consultar productos. Esto permite sustituir la
persistencia o probar esos casos de uso con dobles de prueba.

## Reglas para nuevos cambios

1. Un controlador HTTP solo convierte DTOs de solicitud/respuesta y llama a un caso de uso.
2. Un caso de uso solo puede depender del dominio y de puertos definidos por la
   aplicación; no debe importar Spring Data, HTTP ni SDKs externos.
3. Cada integración externa implementa un puerto en `adapter/out`.
4. La configuración de beans se ubica en `adapter/in/config`, nunca en el dominio o
   en los casos de uso.
5. Las transacciones se abren en el adaptador de entrada, delimitando una operación
   completa sin filtrar detalles de infraestructura hacia la aplicación.

La auditoría ya sigue el mismo patrón mediante `audit/port` y `audit/adapter`. Los
siguientes contextos deben migrarse con estas mismas reglas, preservando sus
contratos públicos antes de modificar funcionalidades.
