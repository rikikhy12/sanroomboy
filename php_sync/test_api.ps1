$res1 = Invoke-RestMethod -Uri 'https://api.sansanroom.id/api/room_kotor.php?limit=250' -Method Get
Write-Output "=== ROOM_KOTOR SUMMARY ==="
$res1.data.summary | Format-List

$res2 = Invoke-RestMethod -Uri 'https://api.sansanroom.id/api/room_checking.php?limit=250' -Method Get
Write-Output "=== ROOM_CHECKING SUMMARY ==="
$res2.data.summary | Format-List



