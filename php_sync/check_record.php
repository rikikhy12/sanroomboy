<?php
require_once __DIR__ . '/../config/database.php';
$database = new Database();
$db = $database->getConnection();
$stmt = $db->query("SHOW FULL COLUMNS FROM room_kotor WHERE Field IN ('status_checking', 'user_checking', 'status_cleaning')");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
echo json_encode($rows, JSON_PRETTY_PRINT);
