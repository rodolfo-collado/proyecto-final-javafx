-- Ejecutar después de iniciar la aplicación una vez para crear las tablas.
-- Puede repetirse: solo agrega los docentes/bloques de ejemplo que falten.
BEGIN;
INSERT INTO teacher (name, email)
VALUES ('Ana Pérez', 'ana.rodolfo@example.test'), ('Luis López', 'luis.rodolfo@example.test')
ON CONFLICT (email) DO NOTHING;

INSERT INTO availability (teacher_id, weekday, block_number)
SELECT t.id, example.weekday, example.block_number
FROM (VALUES
    ('ana.rodolfo@example.test', 1, 2),
    ('ana.rodolfo@example.test', 3, 4),
    ('luis.rodolfo@example.test', 2, 6)
) AS example(email, weekday, block_number)
JOIN teacher t ON t.email = example.email
WHERE NOT EXISTS (
    SELECT 1 FROM availability a
    WHERE a.teacher_id = t.id AND a.weekday = example.weekday
      AND a.block_number = example.block_number
);
COMMIT;
