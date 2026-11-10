# AulaEquilibrio — módulo de aulas y tipos de aula

Aporte correspondiente a Rolando Mayorga en `rolando-mayorga`: CRUD Hibernate de aulas y tipos de aula, relación obligatoria entre ambos, formularios JavaFX, validaciones y protección de tipos con aulas asociadas. El resto del código corresponde a la base existente del equipo.

## Ejecutar

1. Abra el proyecto Maven en IntelliJ IDEA con **JDK 21 completo**, tanto para el proyecto como para Maven.
2. Inicie PostgreSQL y utilice la base configurada por el equipo; si necesita una nueva, cree `aulaequilibrio` en pgAdmin.
3. Copie `.env.example` como `.env` junto a `pom.xml`. Configure localmente `DB_URL`, `DB_USER` y `DB_PASSWORD`. No publique ese archivo.
4. Conectado a la base elegida, ejecute `sql/rolando_aulas.sql`. Conserva las aulas anteriores y les asigna el tipo General cuando falta la relación.
5. Desde la carpeta que contiene `pom.xml`, ejecute:

   ```powershell
   .\mvnw.cmd clean compile
   .\mvnw.cmd javafx:run
   ```

6. En la ventana inicial, pulse **Aulas y tipos de aula**. Cree primero un tipo y luego un aula que lo utilice.

Las instrucciones completas, los archivos modificados, las relaciones JPA y el procedimiento de revisión están en [docs/ROLANDO_AULAS.md](docs/ROLANDO_AULAS.md).

## Subir la rama

El ZIP incluye `.git` y deja seleccionada `rolando-mayorga`. Desde la carpeta del proyecto:

```bash
git push -u origin rolando-mayorga
```

Abra un PR a `develop` y solicite revisión real a otro integrante. Se conserva la corrección de `Classroom.java` que Bismarck ya publicó en `develop`. Los commits nuevos están identificados como código generado; revise y comprenda los cambios y registre sus aportes reales. No se ha enviado ningún cambio al repositorio remoto.
