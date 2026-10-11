$passwords = @("jamborammbo", "jamborambo")
foreach ($p in $passwords) {
    try {
        $req = [System.Net.FtpWebRequest]::Create("ftp://ftp.sansanroom.id/public_html/sansan_api/api/")
        $req.Credentials = New-Object System.Net.NetworkCredential("riki@sansanroom.id", $p)
        $req.Method = [System.Net.WebRequestMethods+Ftp]::ListDirectory
        $req.UsePassive = $true
        $req.Timeout = 8000
        $resp = $req.GetResponse()
        Write-Output "SUCCESS with password: $p"
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $listing = $reader.ReadToEnd()
        Write-Output "Directory Listing snippet:"
        Write-Output ($listing.Substring(0, [Math]::Min(300, $listing.Length)))
        $reader.Close()
        $resp.Close()
        break
    } catch {
        Write-Output "FAILED with $p : $($_.Exception.Message)"
    }
}
