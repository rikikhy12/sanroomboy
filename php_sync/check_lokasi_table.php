<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

$lokasiRows = $db->query("SELECT * FROM lokasi")->fetchAll(PDO::FETCH_ASSOC);
print_r($lokasiRows);
