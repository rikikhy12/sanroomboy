$user = 'riki@sansanroom.id'
$pass = 'jamborambo'

function Get-FtpFile($remotePath, $localPath) {
    try {
        $ftp = [System.Net.FtpWebRequest]::Create($remotePath)
        $ftp.Credentials = New-Object System.Net.NetworkCredential($user, $pass)
        $ftp.Method = [System.Net.WebRequestMethods+Ftp]::DownloadFile
        $ftp.UsePassive = $true
        $ftp.UseBinary = $true
        $resp = $ftp.GetResponse()
        $stream = $resp.GetResponseStream()
        $fs = [System.IO.File]::Create($localPath)
        $stream.CopyTo($fs)
        $fs.Close()
        $stream.Close()
        $resp.Close()
        Write-Output "Downloaded $remotePath to $localPath"
    } catch {
        Write-Warning "Failed to download $($remotePath) : $_"
    }
}

Get-FtpFile "ftp://ftp.sansanroom.id/public_html/error_log" "d:\backuptoko\sanRoomboy\php_sync\error_log.txt"
Get-FtpFile "ftp://ftp.sansanroom.id/public_html/sansan_api/error_log" "d:\backuptoko\sanRoomboy\php_sync\error_log_api.txt"
