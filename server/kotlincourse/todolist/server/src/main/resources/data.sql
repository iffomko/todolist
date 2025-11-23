INSERT INTO folders (name, expanded)
SELECT 'Productivity', true
WHERE NOT EXISTS (
    SELECT 1 FROM folders as f WHERE f.name = 'Productivity'
);

INSERT INTO folders (name, expanded)
SELECT 'Activity', false
    WHERE NOT EXISTS (
    SELECT 1 FROM folders as f WHERE f.name = 'Activity'
);

INSERT INTO tasks (name, checked, folder_id)
SELECT 'Buy products', false, (select f.id from folders as f where f.name = 'Activity')
    WHERE NOT EXISTS (
    SELECT 1 FROM tasks as t WHERE t.name = 'Buy products'
);

INSERT INTO tasks (name, checked, folder_id)
SELECT 'Watch the film', false, (select f.id from folders as f where f.name = 'Activity')
    WHERE NOT EXISTS (
    SELECT 1 FROM tasks as t WHERE t.name = 'Watch the film'
);

INSERT INTO tasks (name, checked, folder_id)
SELECT 'Read the book', false, (select f.id from folders as f where f.name = 'Productivity')
    WHERE NOT EXISTS (
    SELECT 1 FROM tasks as t WHERE t.name = 'Read the book'
);

INSERT INTO subtasks (name, checked, task_id)
SELECT 'Banana', false, (select t.id from tasks as t where t.name = 'Buy products')
    WHERE NOT EXISTS (
    SELECT 1 FROM subtasks as s WHERE s.name = 'Banana'
);

INSERT INTO subtasks (name, checked, task_id)
SELECT 'Milk', true, (select t.id from tasks as t where t.name = 'Buy products')
    WHERE NOT EXISTS (
    SELECT 1 FROM subtasks as s WHERE s.name = 'Chocolate'
);