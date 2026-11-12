# Aula Equilibrio

Aplicación JavaFX con persistencia en PostgreSQL mediante Hibernate. La ventana principal se abre maximizada con el **Dashboard** visible dentro del escritorio MDI. La barra de tareas superior abre Horarios y los catálogos, marca los módulos abiertos y resalta el que está al frente.

Cada módulo se abre maximizado dentro del área de trabajo. Usa **Minimizar** para ocultarlo sin cerrar sus datos y vuelve a seleccionarlo desde la barra. Usa **Restaurar** para mover la ventana desde su barra de título y cambiar su tamaño desde la esquina inferior derecha.

También puedes arrastrar una ventana directamente desde la barra de título. Al acercarla a un lateral aparece una vista previa de media pantalla. En una esquina ocupa un cuarto del escritorio y en el borde superior se maximiza. Suelta para aplicar el ajuste o arrástrala de nuevo para recuperar su tamaño libre.

Para ver varias ventanas a la vez, abre los módulos y elige **Ventanas → Organizar en cascada**. Se acomodan superpuestas con sus barras de título visibles.

Pulsa una ventana interna para traerla al frente. Los avisos, las confirmaciones y los formularios de aulas y horarios se muestran dentro del escritorio. Resuelve el cuadro activo para continuar con los módulos.

## Ejecutar

Se requiere JDK 21 y PostgreSQL. Copia `.env.example` a `.env` y configura `DB_URL`, `DB_USER` y `DB_PASSWORD` para una base existente. El usuario de la base debe poder crear/actualizar tablas, como requiere la configuración Hibernate existente.

Importa el `pom.xml` como proyecto Maven en IntelliJ IDEA. Maven configura el procesador de anotaciones de Lombok para generar los métodos y constructores al compilar.

Si tu base ya contiene aulas de una versión anterior, ejecuta primero `sql/rolando_aulas.sql` en esa base. El script asigna el tipo **General** a las aulas existentes que todavía no tienen tipo y conserva sus datos.

```bash
./mvnw javafx:run
```

## Datos de demostración

Al abrir la aplicación, Hibernate prepara las tablas y ejecuta `sql/demo_seed.sql` en la base configurada. En una base nueva aparecen tres docentes, tres grupos, tres asignaturas, tres aulas y un horario con seis asignaciones.

La semilla agrega filas de ejemplo que falten y conserva los registros existentes. Abre **Horarios** desde la barra superior y selecciona **Demo · Semana académica** para probar el filtro por grupo.

En **Oferta académica**, administra grupos y asignaturas en dos tablas de una misma ventana. Los botones de alta y edición abren sus formularios dentro del escritorio MDI.

En **Aulas**, crea primero un tipo en el panel lateral y luego registra un aula con ese tipo. Selecciona una fila de **Tipos de aula** para filtrar la tabla principal o pulsa **Todos los tipos** para quitar el filtro. En **Horarios**, el aula aparecerá entre las opciones de una asignación.

En **Docentes y disponibilidad**, pulsa **Nuevo docente** o selecciona una fila y pulsa **Editar**. El formulario se abre en una ventana interna con su propia entrada en la barra de tareas. Al seleccionar un docente, el panel lateral muestra sus bloques.

Busca docentes por nombre o correo y filtra por presencia de correo. En el panel lateral puedes combinar el día y el número exacto de bloque. Si el filtro de docentes oculta la fila seleccionada, el panel lateral se limpia hasta que selecciones otro docente.

- El nombre es obligatorio y admite hasta 120 caracteres. El correo es opcional y único.
- Cada bloque exige un docente existente, un día de lunes a domingo y un número entero mayor que cero. No se impone un máximo de bloques porque el requisito no lo define.
- Usa **Nuevo bloque** para preparar un alta. Para editar o eliminar, selecciona un bloque en el panel lateral.
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

1. Pulsa **Actualizar** y selecciona a Ana en la tabla principal. Deben aparecer únicamente sus dos bloques.
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
