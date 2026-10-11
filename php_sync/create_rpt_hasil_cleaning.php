<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

// Create rpt_hasil_cleaning
$sqlCreate = "CREATE TABLE IF NOT EXISTS `rpt_hasil_cleaning` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `id_cleaning` INT NULL,
  `kode_cleaning` VARCHAR(255) NULL,
  `no_room` VARCHAR(255) NOT NULL,
  `kode_room` VARCHAR(255) NULL,
  `officeboy` VARCHAR(255) NULL,
  `iduser` INT NULL,
  `tgl_cleaning` DATE NULL,
  `start_time` TIME NULL,
  `end_time` TIME NULL,
  `foto_hasil` VARCHAR(255) NULL,
  `checklist_data` LONGTEXT NULL,
  `status_kondisi` VARCHAR(50) DEFAULT 'BERSIH_RAPI',
  `catatan` TEXT NULL,
  `create_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
$db->exec($sqlCreate);
echo "rpt_hasil_cleaning table created or verified successfully!\n";

$distinctItems = $db->query("SELECT MIN(id) as id, item_name FROM item_check WHERE item_name IS NOT NULL AND item_name != '' GROUP BY item_name ORDER BY id ASC")->fetchAll(PDO::FETCH_ASSOC);
echo "DISTINCT ITEM_CHECK (" . count($distinctItems) . "):\n";
print_r($distinctItems);
