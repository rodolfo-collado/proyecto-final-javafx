# Aula Equilibrio — docentes y disponibilidad

Implementación en `rodolfo-ramirez`, sobre `develop`. Reutiliza `Teacher` sin modificarlo y la configuración PostgreSQL de `HibernateUtil`. El ZIP se usó como referencia; no se incorporaron sus otros módulos ni su historial Git.

## Ejecutar

Se requiere JDK 21 y PostgreSQL. Copia `.env.example` a `.env` y configura `DB_URL`, `DB_USER` y `DB_PASSWORD` para una base existente. El usuario de la base debe poder crear/actualizar tablas, como requiere la configuración Hibernate existente.

```bash
./mvnw javafx:run
```

La pestaña **Docentes y disponibilidad** contiene dos catálogos. La navegación también conserva Inicio, Grupos y Asignaturas, y el acceso a Horarios desde Inicio. Usa **Nuevo** para limpiar la selección, **Guardar** para crear o actualizar y selecciona una fila para editarla. La eliminación requiere confirmación.

- El nombre es obligatorio y admite hasta 120 caracteres. El correo es opcional y único.
- Cada bloque exige un docente existente, un día de lunes a domingo y un número entero mayor que cero. No se impone un máximo de bloques porque el requisito no lo define.
- **Consultar** lista los bloques del docente elegido; **Ver todos** restablece la lista completa.
- Un docente con bloques asociados no se puede eliminar. La pantalla explica que primero deben eliminarse o reasignarse sus bloques. La clave foránea también protege la relación en la base.

La consulta está en `CatalogDAO.listAvailability`:

```hql
select a from Availability a join fetch a.teacher t
where t.id = :teacherId
order by a.day, a.blockNumber, a.id
```

Cada operación abre una sesión, confirma su transacción y revierte los cambios ante un error. Las consultas se ejecutan en segundo plano para mantener la ventana operativa; los controles se habilitan al terminar.

## Revisión con dos docentes

Puedes crearlos desde la pantalla o ejecutar `sql/rodolfo_demo.sql` en tu base PostgreSQL después del primer inicio. El script agrega a Ana Pérez con lunes/bloque 2 y miércoles/bloque 4, y a Luis López con martes/bloque 6; puede repetirse sin duplicar esos ejemplos.

1. Pulsa **Actualizar** y consulta a Ana: deben aparecer únicamente sus dos bloques.
2. Selecciona su bloque 2, cambia el día a viernes y el número a 8; guarda y comprueba el resultado.
3. Selecciona a Ana e intenta eliminarla: debe aparecer el mensaje de bloques asociados.
4. Elimina uno de sus bloques y comprueba que desaparezca. Consulta a Luis y verifica que su bloque siga intacto.
5. Cierra y abre la aplicación para comprobar la persistencia en PostgreSQL.

## Pruebas

```bash
./mvnw test
```

Cinco pruebas de integración Hibernate usan una base H2 temporal en modo PostgreSQL: CRUD, filtrado entre dos docentes, edición y eliminación de bloques, validaciones, reasignación y protección de claves foráneas. No necesitan tu `.env` ni alteran tu base de trabajo.

Una sexta prueba carga el FXML y opera los controles JavaFX para comprobar el recorrido de revisión y el mensaje al borrar un docente referenciado. Requiere un servidor gráfico (`DISPLAY`); sin él se omite esa prueba. Las pruebas H2 no sustituyen la comprobación final con tu instalación PostgreSQL.

También se corrigieron el nombre de clase y el paquete de `Classroom.java`, que en la base original impedían compilar.
