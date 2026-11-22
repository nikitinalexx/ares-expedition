# A tournament of checkpoints: train several variants of one corpus, screen each against a fixed benchmark, keep the best.
#
#   powershell -File ml/tournament.ps1 -Data D:/ares/data/netd_vssmart_i2s_10k/netdata.bin -Base <parent zip>
#
# Why (owner, 22/11): "I did an eval on a test set; a network after 100 batches sometimes beat the same network after 200,
# and I picked the best of the whole variety - sometimes it is simply luck." This script makes that selection explicit:
# every variant is trained the same way except the seed (and optionally the number of epochs), each is measured on the SAME
# benchmark, and the winner is copied to `best.zip`. A variant is never accepted on training metrics alone.
param(
    [Parameter(Mandatory = $true)][string]$Data,
    [string]$Base = "",
    [string]$OutDir = "",
    [string]$Seeds = "1,2,3,4",
    [string]$Epochs = "1,2",
    [int]$Batch = 64,
    [double]$Lr = 0.00005,
    [double]$Dropout = 0.1,
    [int]$Games = 200,
    [string]$Bench = "NETWORK",
    [int]$Concurrency = 8
)

$ErrorActionPreference = "Continue"
$cp = (Get-Content target\cp.txt -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
if ($OutDir -eq "") { $OutDir = "D:/ares/runs/network3/tournament_" + (Get-Date -Format "MMdd_HHmm") }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$warm = @()
if ($Base -ne "") { $warm = @("--warm-start", $Base) }
$results = @()

foreach ($epochs in $Epochs.Split(",")) {
    foreach ($seed in $Seeds.Split(",")) {
        $name = "s${seed}_e${epochs}"
        $zip = "$OutDir/$name.zip"
        py -3 ml\train_network_v2_torch.py --csv $Data --out $zip --epochs $epochs --batch $Batch --lr $Lr `
            --seed $seed --val-steps 500 --val-rows 50000 --patience 8 --in-ram --dropout $Dropout @warm `
            *> "$OutDir/train_$name.log"
        $val = (Select-String -Path "$OutDir/train_$name.log" -Pattern "using the best" | Select-Object -Last 1).Line
        & $java -Xmx10g "-Dares.nn.model=$zip" -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain `
            --mode compare --a NETWORK_V2 --b $Bench --games $Games --concurrency $Concurrency `
            --out "$OutDir/${name}_vs.json" *> "$OutDir/eval_$name.log"
        $line = (Select-String -Path "$OutDir/eval_$name.log" -Pattern "CompareResult\[" | Select-Object -Last 1).Line
        $score = if ($line -match "aScore=([\d.]+)") { [double]$Matches[1] } else { -1 }
        $results += [pscustomobject]@{ variant = $name; score = $score; val = ($val -replace "\s+", " ").Trim() }
        Write-Output "$name : $([math]::Round($score * 100, 2))% ($Games games) $val"
    }
}

$best = $results | Sort-Object score -Descending | Select-Object -First 1
$ranking = $results | Sort-Object score -Descending
Write-Output ""
Write-Output "=== ranking (${Bench}, $Games games each)"
$ranking | ForEach-Object { Write-Output ("  {0,-8} {1,6:N2}%   {2}" -f $_.variant, ($_.score * 100), $_.val) }
Copy-Item "$OutDir/$($best.variant).zip" "$OutDir/best.zip" -Force
Write-Output "best -> $OutDir/best.zip ($($best.variant), $([math]::Round($best.score * 100, 2))%)"
