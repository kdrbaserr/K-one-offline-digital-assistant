$ErrorActionPreference = "Stop"

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$androidStudioAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$adb = if ($adbCommand) { $adbCommand.Source } elseif (Test-Path $androidStudioAdb) { $androidStudioAdb } else { $null }

if (-not $adb) {
    throw "adb bulunamadı. Android SDK Platform Tools kurup PATH'e ekleyin."
}

$devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" })
if ($devices.Count -ne 1) {
    throw "Tam olarak bir yetkili Android cihaz bağlayın. Bulunan: $($devices.Count)"
}

Write-Output "manufacturer=$(& $adb shell getprop ro.product.manufacturer)"
Write-Output "model=$(& $adb shell getprop ro.product.model)"
Write-Output "android=$(& $adb shell getprop ro.build.version.release)"
Write-Output "api=$(& $adb shell getprop ro.build.version.sdk)"
Write-Output "abi=$(& $adb shell getprop ro.product.cpu.abilist)"
Write-Output "hardware=$(& $adb shell getprop ro.hardware)"
Write-Output "memory=$(& $adb shell cat /proc/meminfo | Select-Object -First 1)"
Write-Output "display=$(& $adb shell wm size)"
Write-Output "density=$(& $adb shell wm density)"
