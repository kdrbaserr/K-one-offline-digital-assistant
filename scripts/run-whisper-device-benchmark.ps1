param(
    [string]$OutputPath = "measurements/stt-whisper-raw.csv",
    [int]$ThreadCount = 4
)

$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb bulunamadı: $adb" }

$deviceRoot = "/data/local/tmp/kone-whisper"
$clips = @(& $adb shell "ls $deviceRoot/clips/*.wav") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
if ($clips.Count -lt 50) { throw "En az 50 klip gerekli; bulunan: $($clips.Count)" }

$models = @(
    @{ Engine = "whisper-tiny-q5_1"; File = "ggml-tiny-q5_1.bin" },
    @{ Engine = "whisper-base-q5_1"; File = "ggml-base-q5_1.bin" }
)
$rows = [System.Collections.Generic.List[object]]::new()

foreach ($model in $models) {
    foreach ($clipPath in $clips | Select-Object -First 60) {
        $clip = Split-Path $clipPath -Leaf
        $temperatureRaw = ((& $adb shell "dumpsys battery | grep temperature") | Select-Object -First 1).Trim()
        $temperatureTenths = [int]([regex]::Match($temperatureRaw, "temperature:\s*(\d+)").Groups[1].Value)
        $command = "cd $deviceRoot && export LD_LIBRARY_PATH=./lib && toybox time -v ./whisper-cli -m $($model.File) -f clips/$clip -l tr -np -nt -t $ThreadCount"
        $oldPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $raw = (& $adb shell $command 2>&1 | ForEach-Object { "$_" }) -join "`n"
        $ErrorActionPreference = $oldPreference

        $latencySeconds = [double]::Parse(
            [regex]::Match($raw, "Real time \(s\):\s*([0-9.]+)").Groups[1].Value,
            [Globalization.CultureInfo]::InvariantCulture
        )
        $rssKb = [int64]([regex]::Match($raw, "Max RSS \(KiB\):\s*(\d+)").Groups[1].Value)
        $transcriptLines = $raw -split "`n" | Where-Object {
            $_ -notmatch "^(adb\.exe\s*:|At line:|\+|\s*~|\s*\+ CategoryInfo|\s*\+ FullyQualifiedErrorId|read_audio_data:|Real time|User time|System time|Max RSS|Major faults|Minor faults|File system|Voluntary context|Involuntary context)" -and $_.Trim()
        }
        $transcript = ($transcriptLines -join " ").Trim()

        $rows.Add([pscustomobject]@{
            engine = $model.Engine
            clip = $clip
            latency_ms = [math]::Round($latencySeconds * 1000)
            max_rss_kb = $rssKb
            temp_c = $temperatureTenths / 10.0
            transcript = $transcript
        })
        Write-Host "$($model.Engine): $($rows.Count % 60)/60 $clip"
    }
}

$absoluteOutput = Join-Path (Get-Location) $OutputPath
$rows | Export-Csv -Path $absoluteOutput -NoTypeInformation -Encoding utf8
Write-Host "Yazıldı: $absoluteOutput ($($rows.Count) satır)"
