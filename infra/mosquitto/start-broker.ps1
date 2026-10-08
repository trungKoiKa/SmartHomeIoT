# Khoi dong broker MQTT dev tu thu muc goc repo. Can file infra/mosquitto/passwd (xem setup-users.ps1).
$root = Resolve-Path "$PSScriptRoot\..\.."
$exe = "C:\Program Files\mosquitto\mosquitto.exe"
if (-not (Test-Path "$root\infra\mosquitto\passwd")) { throw "Thieu infra/mosquitto/passwd - chay setup-users.ps1 truoc" }
Set-Location $root
& $exe -c infra/mosquitto/mosquitto.conf -v
