$user = 'riki@sansanroom.id'
$pass = 'jamborambo'

function List-FtpDir($path) {
    Write-Output "=== Listing $path ==="
    try {
        $ftp = [System.Net.FtpWebRequest]::Create($path)
        $ftp.Credentials = New-Object System.Net.NetworkCredential($user, $pass)
        $ftp.Method = [System.Net.WebRequestMethods+Ftp]::ListDirectoryDetails
        $ftp.UsePassive = $true
        $ftp.UseBinary = $true
        $resp = $ftp.GetResponse()
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $output = $reader.ReadToEnd()
        Write-Output $output
        $reader.Close()
        $resp.Close()
    } catch {
        Write-Warning "Error listing $path : $_"
    }
}

List-FtpDir "ftp://ftp.sansanroom.id/"
List-FtpDir "ftp://ftp.sansanroom.id/public_html/"
List-FtpDir "ftp://ftp.sansanroom.id/public_html/sansan_api/"
List-FtpDir "ftp://ftp.sansanroom.id/public_html/sansan_api/api/"
