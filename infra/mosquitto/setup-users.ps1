# Tao user cho backend va gateway voi mat khau ngau nhien. Mat khau in ra 1 lan o file local (gitignore):
#   backend/src/main/resources/application-local.properties  va  embedded/esp32-gateway/secrets.h
$root = Resolve-Path "$PSScriptRoot\..\.."
$passwd = "C:\Program Files\mosquitto\mosquitto_passwd.exe"
function New-Secret { -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 20 | ForEach-Object { [char]$_ }) }
$be = New-Secret; $gw = New-Secret
& $passwd -b -c "$root\infra\mosquitto\passwd" homesmart_backend $be
& $passwd -b "$root\infra\mosquitto\passwd" homesmart_gateway $gw
"backend user: homesmart_backend / $be"
"gateway user: homesmart_gateway / $gw"
