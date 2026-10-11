<?php
$ftp_server = "ftp.sansanroom.id";
$ftp_user = "riki@sansanroom.id";
$ftp_pass = "jamborambo";

$conn = ftp_connect($ftp_server);
if (!ftp_login($conn, $ftp_user, $ftp_pass)) {
    die("FTP login failed\n");
}
ftp_pasv($conn, true);

$remote_files = [
    "/public_html/application/controllers/master/Roomchar.php",
    "/public_html/application/models/master/M_room.php"
];

foreach ($remote_files as $remote) {
    $local = __DIR__ . "/" . basename($remote);
    if (ftp_get($conn, $local, $remote, FTP_BINARY)) {
        echo "Downloaded: $remote -> $local\n";
    } else {
        echo "Failed to download: $remote\n";
    }
}
ftp_close($conn);
