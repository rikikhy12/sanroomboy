$user = "riki@sansanroom.id"
$pass = "jamborambo"

$f = @{ Remote = "ftp://ftp.sansanroom.id/public_html/application/models/transaksi/M_booking.php"; Local = "$PSScriptRoot\M_booking.php" }

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
Write-Output "Downloaded M_booking.php"
