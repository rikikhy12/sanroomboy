$user = "riki@sansanroom.id"
$pass = "jamborambo"

$files = @(
    @{ Remote = "ftp://ftp.sansanroom.id/public_html/application/controllers/master/Roomchar.php"; Local = "$PSScriptRoot\Roomchar.php" },
    @{ Remote = "ftp://ftp.sansanroom.id/public_html/application/models/master/M_room.php"; Local = "$PSScriptRoot\M_room.php" }
)

foreach ($f in $files) {
    try {
        $req = [System.Net.FtpWebRequest]::Create($f.Remote)
        $req.Credentials = New-Object System.Net.NetworkCredential($user, $pass)
        $req.Method = [System.Net.WebRequestMethods+Ftp]::DownloadFile
        $req.UsePassive = $true
        $req.UseBinary = $true
        $res = $req.GetResponse()
        $stream = $res.GetResponseStream()
        $targetStream = [System.IO.File]::Create($f.Local)
        $stream.CopyTo($targetStream)
        $targetStream.Close()
        $stream.Close()
        $res.Close()
        Write-Output "Downloaded $($f.Remote)"
    } catch {
        Write-Output "Error downloading $($f.Remote): $_"
    }
}
