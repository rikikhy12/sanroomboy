<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== COLUMNS IN tbl_check_in ===\n";
$colsCI = $db->query("DESCRIBE tbl_check_in")->fetchAll(PDO::FETCH_ASSOC);
print_r($colsCI);

echo "=== COLUMNS IN tbl_check_in_detail ===\n";
$colsCID = $db->query("DESCRIBE tbl_check_in_detail")->fetchAll(PDO::FETCH_ASSOC);
print_r($colsCID);

echo "=== SAMPLE tbl_check_in ===\n";
$sampleCI = $db->query("SELECT * FROM tbl_check_in ORDER BY id DESC LIMIT 2")->fetchAll(PDO::FETCH_ASSOC);
print_r($sampleCI);

echo "=== SAMPLE tbl_check_in_detail ===\n";
$sampleCID = $db->query("SELECT * FROM tbl_check_in_detail ORDER BY id DESC LIMIT 2")->fetchAll(PDO::FETCH_ASSOC);
print_r($sampleCID);
