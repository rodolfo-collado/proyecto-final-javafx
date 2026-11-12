# Aulas y tipos de aula

## Preparar la base

Sigue la configuración de PostgreSQL del README principal. Si tu base ya contiene aulas, ejecuta `sql/rolando_aulas.sql` antes de iniciar. El script agrega el tipo **General** a las aulas anteriores que todavía no tienen tipo. Si existen capacidades menores o iguales a cero, corrígelas antes de repetir la migración.

## Probar desde JavaFX

1. Inicia con `./mvnw javafx:run` o `mvnw.cmd javafx:run` en Windows.
2. Abre **Aulas → Tipos de aula** y crea el tipo **Laboratorio**.
3. En **Aulas → Aulas**, registra un aula con un código único, una capacidad positiva y el tipo **Laboratorio**.
4. Filtra por **Laboratorio** y edita la capacidad. Reinicia la aplicación para comprobar que el cambio se guardó.
5. Intenta eliminar **Laboratorio** mientras tenga un aula asociada. Reasigna o elimina el aula antes de borrar el tipo.
6. Abre **Horarios** desde la barra superior y comprueba que el aula aparece al crear una asignación.

`ClassroomDAO` lista las aulas y sus tipos mediante HQL. La clave foránea protege los tipos referenciados y el campo de versión detecta actualizaciones concurrentes.
