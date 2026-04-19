CREATE DATABASE IF NOT EXISTS oneconnect_db;
USE oneconnect_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS favourites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    service_name VARCHAR(100),
    service_url VARCHAR(255),
    added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
