# Rolando Mayorga — aulas y tipos de aula

Este cambio implementa exclusivamente la asignación de `Rolando-Mayorga.md`: entidades, CRUD Hibernate y formularios JavaFX para aulas y tipos de aula. Se conserva el proyecto del equipo como base para que el módulo compile y se integre; no se agregan los módulos asignados a los compañeros.

## Rama y base

Rama de trabajo: `rolando-mayorga`. Se incorporó por avance rápido el `develop` consultado, que ya contiene la corrección de nombre y paquete de `Classroom.java`, hecha por Bismarck (`443f901`). Esa corrección conserva su autoría original. Los commits nuevos del módulo se agregan después de ese punto y están identificados como código generado con asistencia. No se atribuyeron aportes a compañeros ni se modificaron fechas del historial.

## Archivos del módulo

| Archivo | Responsabilidad |
| --- | --- |
| `model/Classroom.java` | Entidad de aula, código único, capacidad y relación obligatoria con tipo |
| `model/ClassroomType.java` | Entidad de tipo con nombre único y colección inversa de aulas |
| `dao/ClassroomDAO.java` | Alta, listado, filtro por tipo, actualización y eliminación con Hibernate |
| `dao/ClassroomTypeDAO.java` | CRUD de tipos y protección de los tipos que tienen aulas |
| `controller/ClassroomController.java` | Tablas y formularios JavaFX, validaciones y manejo de errores |
| `views/classroom-view.fxml` | Pestañas Aulas y Tipos de aula |
| `config/HibernateUtil.java` | Registro de las dos entidades, conservando las existentes |
| `HomeController.java` y `home-view.fxml` | Botón para abrir el módulo sin reemplazar la pantalla del equipo |
| `sql/rolando_aulas.sql` | Preparación/migración de las dos tablas, conservando aulas anteriores |

Las rutas Java comienzan en `src/main/java/com/practice/aulaequilibrio` y las vistas en `src/main/resources/com/practice/aulaequilibrio`.

## Ejecutar y probar

1. Extraiga completamente el ZIP y abra `proyecto-final-javafx/pom.xml` en IntelliJ como Maven. Use un **JDK 21 completo** como SDK y como JDK de Maven.
2. Inicie PostgreSQL y utilice la base del proyecto configurada por el equipo. Si necesita una nueva, cree `aulaequilibrio` desde pgAdmin.
3. Copie `.env.example` a `.env` junto a `pom.xml` y configure localmente `DB_URL`, `DB_USER` y `DB_PASSWORD`. Ejemplo de URL: `jdbc:postgresql://localhost:5432/aulaequilibrio`. No suba sus credenciales.
4. En pgAdmin, conectado a esa base, ejecute `sql/rolando_aulas.sql` **antes del arranque**. Si ya hay aulas, agrega la relación sin perderlas y les asigna el tipo General. El script se revierte si existen datos que no cumplen las restricciones; corrija capacidades no positivas antes de repetirlo.
5. En la terminal de IntelliJ, desde la carpeta que contiene `pom.xml`:

   ```powershell
   .\mvnw.cmd clean compile
   .\mvnw.cmd javafx:run
   ```

6. Pulse **Aulas y tipos de aula** en la pantalla existente. Se abrirá una ventana con dos pestañas.
7. En **Tipos de aula**, agregue **Laboratorio**. En **Aulas**, agregue un código, una capacidad positiva y seleccione Laboratorio.
8. Cierre la aplicación y vuelva a ejecutarla. El aula y el tipo deben seguir apareciendo. Edite la capacidad, filtre por el tipo y elimine un aula sin referencias.
9. Intente eliminar Laboratorio mientras tenga un aula asociada: se muestra un error claro y el tipo se conserva. Después de eliminar o reasignar esas aulas, podrá borrar el tipo.

Maven utiliza las dependencias originales del proyecto. No se agregan librerías ni se modifica `module-info.java`. El objetivo Maven `javafx:run` ejecuta `Launcher` mediante classpath para cargar las dependencias de ejecución de Hibernate; así evita errores de resolución de módulos. Ejecute desde Maven en IntelliJ como se indica arriba. La comprobación de conexión ya existente conserva su consulta JDBC `SELECT 1`; todas las operaciones CRUD nuevas utilizan Hibernate y HQL.

## Relación y transacciones

`Classroom.type` tiene `@ManyToOne(optional=false)` y `@JoinColumn(nullable=false)`. La clave foránea está en `classroom.type_id`. `ClassroomType.classrooms` tiene `@OneToMany(mappedBy="type")`; no borra aulas en cascada.

Cada operación abre su sesión. Guardar y eliminar se ejecutan en una transacción con rollback ante errores. `@Version` permite rechazar ediciones de registros que cambiaron después de cargarlos. Se valida código, capacidad mayor que cero, tipo existente, nombres obligatorios y duplicados. La interfaz trabaja con `Thread` y actualiza controles mediante `Platform.runLater`.

`ClassroomDAO.listar()` y `listarPorTipo()` incluyen `join fetch c.type`: recuperan aulas y su tipo para mostrar la relación en JavaFX después de cerrar la sesión. La protección al eliminar tipos consulta cuántas aulas los referencian antes de borrar y mantiene la clave foránea como respaldo.

## Subir e integrar

La carpeta incluye `.git` con el historial real y la rama seleccionada. No se ha subido nada automáticamente. Desde `proyecto-final-javafx`:

```bash
git branch --show-current
git status
git push -u origin rolando-mayorga
```

Abra un PR **rolando-mayorga → develop** y solicite revisión a otro integrante. Su diferencia contra `develop` contiene únicamente los archivos de este módulo. Si el equipo integrará directamente a `main`, coordine primero la inclusión de la corrección que ya está en `develop`.

Si ya tiene cambios propios en su computadora, consérvelos e integre esta rama en su clon. No use `--force` si el remoto avanzó. Los commits generados no sustituyen su revisión, comprensión y aportes reales exigidos por el docente. El usuario local de Git no queda configurado como el asistente dentro del ZIP.

## Verificación realizada

Se compiló con JDK 21 y se ejecutaron **20 comprobaciones** contra PostgreSQL 17.11: migración repetible de un aula anterior, CRUD de ambos catálogos, filtro HQL, duplicados, capacidad inválida, tipo obligatorio, rollback, control de edición atrasada y conservación de datos después de reconstruir la conexión de Hibernate.

En JavaFX se comprobó la apertura desde la pantalla del equipo, la visualización del aula migrada, el alta de tipo y aula, la edición de capacidad, el error al borrar un tipo asociado, la eliminación de un aula y la eliminación del tipo una vez libre. El arranque se verifica mediante el mismo objetivo Maven indicado en estas instrucciones. Las credenciales de las pruebas eran temporales y no forman parte del paquete.
