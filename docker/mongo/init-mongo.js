// init-mongo.js
db = db.getSiblingDB('demodb'); // Use your database name

// Create collections and insert data
db.users.insertMany([
    { name: "Alice", email: "alice@example.com", age: 30 },
    { name: "Bob", email: "bob@example.com", age: 25 }
]);

db.products.insertOne({ name: "Laptop", price: 999.99, stock: 10 });