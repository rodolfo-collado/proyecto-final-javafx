-- Datos de ejemplo para el arranque de la aplicación. También se puede ejecutar con psql.
DO $seed$
BEGIN
    INSERT INTO teacher (name, email) VALUES
        ('Elena Campos', 'elena.demo@example.test'),
        ('Mateo Reyes', 'mateo.demo@example.test'),
        ('Sofía Vargas', 'sofia.demo@example.test')
    ON CONFLICT (email) DO NOTHING;

    INSERT INTO grupo (nombre, cantidad_estudiantes) VALUES
        ('DEMO-1A', 28),
        ('DEMO-1B', 24),
        ('DEMO-2A', 20)
    ON CONFLICT (nombre) DO NOTHING;

    INSERT INTO asignatura (codigo, nombre, sesiones_requeridas) VALUES
        ('DEMO-PROG', 'Programación I', 2),
        ('DEMO-MAT', 'Matemática discreta', 2),
        ('DEMO-BD', 'Bases de datos', 2)
    ON CONFLICT (codigo) DO NOTHING;

    INSERT INTO classroom_type (name, revision) VALUES
        ('Demo · Aula teórica', 0),
        ('Demo · Laboratorio', 0)
    ON CONFLICT (name) DO NOTHING;

    INSERT INTO classroom (codigo, capacidad, type_id, revision)
    SELECT demo.codigo, demo.capacidad, tipo.id, 0
    FROM (VALUES
        ('DEMO-A101', 35, 'Demo · Aula teórica'),
        ('DEMO-L201', 30, 'Demo · Laboratorio'),
        ('DEMO-L202', 26, 'Demo · Laboratorio')
    ) AS demo(codigo, capacidad, tipo)
    JOIN classroom_type tipo ON tipo.name = demo.tipo
    ON CONFLICT (codigo) DO NOTHING;

    -- Estas tablas permiten nombres o bloques repetidos. Serializar la inserción
    -- del ejemplo evita duplicados si dos sesiones ejecutan la semilla a la vez.
    LOCK TABLE availability, schedule_version IN SHARE ROW EXCLUSIVE MODE;

    INSERT INTO availability (teacher_id, weekday, block_number)
    SELECT docente.id, demo.dia, demo.bloque
    FROM (VALUES
        ('elena.demo@example.test', 1, 1),
        ('elena.demo@example.test', 3, 1),
        ('mateo.demo@example.test', 1, 1),
        ('mateo.demo@example.test', 3, 1),
        ('sofia.demo@example.test', 2, 2),
        ('sofia.demo@example.test', 4, 2)
    ) AS demo(email, dia, bloque)
    JOIN teacher docente ON docente.email = demo.email
    WHERE NOT EXISTS (
        SELECT 1 FROM availability a
        WHERE a.teacher_id = docente.id AND a.weekday = demo.dia AND a.block_number = demo.bloque
    );

    INSERT INTO schedule_version (name, created_on)
    SELECT 'Demo · Semana académica', CURRENT_DATE
    WHERE NOT EXISTS (SELECT 1 FROM schedule_version WHERE name = 'Demo · Semana académica');

    INSERT INTO schedule_assignment (version_id, group_id, subject_id, teacher_id, classroom_id, day_of_week, block_number)
    SELECT version.id, grupo.id, asignatura.id, docente.id, aula.id, demo.dia, demo.bloque
    FROM (VALUES
        ('DEMO-1A', 'DEMO-PROG', 'elena.demo@example.test', 'DEMO-L201', 1, 1),
        ('DEMO-1A', 'DEMO-PROG', 'elena.demo@example.test', 'DEMO-L201', 3, 1),
        ('DEMO-1B', 'DEMO-MAT',  'mateo.demo@example.test', 'DEMO-A101', 1, 1),
        ('DEMO-1B', 'DEMO-MAT',  'mateo.demo@example.test', 'DEMO-A101', 3, 1),
        ('DEMO-2A', 'DEMO-BD',   'sofia.demo@example.test', 'DEMO-L202', 2, 2),
        ('DEMO-2A', 'DEMO-BD',   'sofia.demo@example.test', 'DEMO-L202', 4, 2)
    ) AS demo(grupo, asignatura, email, aula, dia, bloque)
    JOIN grupo ON grupo.nombre = demo.grupo
    JOIN asignatura ON asignatura.codigo = demo.asignatura
    JOIN teacher docente ON docente.email = demo.email
    JOIN classroom aula ON aula.codigo = demo.aula
    CROSS JOIN (
        SELECT min(id) AS id FROM schedule_version WHERE name = 'Demo · Semana académica'
    ) version
    ON CONFLICT DO NOTHING;
END;
$seed$;
