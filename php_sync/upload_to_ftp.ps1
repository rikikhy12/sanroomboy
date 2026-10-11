$ftpServer = "ftp://ftp.sansanroom.id/public_html/sansan_api/api/"
$user = "riki@sansanroom.id"
$pass = "jamborambo"

$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$filesToUpload = @(
    @{ Local = "$baseDir\room_checking.php"; Remote = "room_checking.php" },
    @{ Local = "$baseDir\room_kotor.php"; Remote = "room_kotor.php" },
    @{ Local = "$baseDir\optimize_db.php"; Remote = "optimize_db.php" }
)

# Also copy to Laragon if folder exists
$laragonDir = "C:\laragon\www\sansan_api\api"
if (Test-Path $laragonDir) {
    Copy-Item "$baseDir\room_checking.php" "$laragonDir\room_checking.php" -Force
    Copy-Item "$baseDir\room_kotor.php" "$laragonDir\room_kotor.php" -Force
    Write-Output "Copied to Laragon local server."
}

foreach ($f in $filesToUpload) {
    if (-not (Test-Path $f.Local)) {
        Write-Warning "File not found: $($f.Local)"
        continue
    }

    $targetUrl = $ftpServer + $f.Remote
    Write-Output "Uploading $($f.Local) to $targetUrl ..."
    
    $request = [System.Net.FtpWebRequest]::Create($targetUrl)
    $request.Credentials = New-Object System.Net.NetworkCredential($user, $pass)
    $request.Method = [System.Net.WebRequestMethods+Ftp]::UploadFile
    $request.UsePassive = $true
    $request.UseBinary = $true
    $request.KeepAlive = $false
    
    $fileBytes = [System.IO.File]::ReadAllBytes($f.Local)
    $request.ContentLength = $fileBytes.Length
    
    $reqStream = $request.GetRequestStream()
    $reqStream.Write($fileBytes, 0, $fileBytes.Length)
    $reqStream.Close()
    
    $response = $request.GetResponse()
    Write-Output "Upload response: $($response.StatusDescription)"
    $response.Close()
}
Write-Output "All files uploaded successfully via FTP."
