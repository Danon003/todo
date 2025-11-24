// Инициализация базы данных и коллекций
db = db.getSiblingDB('todo_db');

// Создаем коллекцию comments
db.createCollection('comments');

// Создаем индексы для оптимизации запросов
db.comments.createIndex({ "taskId": 1 });
db.comments.createIndex({ "parentId": 1 });
db.comments.createIndex({ "createdAt": 1 });
db.comments.createIndex({ "authorId": 1 });

print('MongoDB initialization completed successfully!');