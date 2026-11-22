# The critic matrix, resumed: every cell on the SAME pack (the champion's 1118-2332_17), 3 seeds, one training at a time.
# Cells whose train_metrics.json already exists are skipped, so the script can be re-run after an interruption (the first
# attempt lost a seed to an out-of-memory kill when six trainings ran at once).
#
#   powershell -File ml/night_critic_matrix.ps1
param(
    [string]$Pack = "C:/ares/pack/1118-2332_17",
    [string]$Names = "D:/ares/tmp/names_now.json"
)

$ErrorActionPreference = "Continue"
$log = "D:\ares\tmp\night_critic_matrix.log"

function Log($text) {
    $line = "$(Get-Date -Format 'HH:mm:ss') $text"
    Write-Output $line
    Add-Content -Path $log -Value $line -Encoding UTF8
}

function Cell($label, $args_) {
    for ($s = 1; $s -le 3; $s++) {
        $out = "D:/ares/runs/value/$($label)_s$s"
        if (Test-Path "$out\train_metrics.json") { Log "$label seed $s exists, skipped"; continue }
        py -3 ml\train_value.py --packed $Pack --out $out --target margin --hidden 512,256 --names $Names `
            --drop-prefix "cards:in_my_hand_" --seed $s --scale max99 --clip 40 --rows-per-game 0 @args_ `
            *> "D:\ares\tmp\$($label)_s$s.log"
        $val = (Select-String -Path "D:\ares\tmp\$($label)_s$s.log" -Pattern "^VAL mse" | Select-Object -Last 1).Line
        Log "$label seed ${s}: exit=$LASTEXITCODE $val"
    }
}

Log "=== critic matrix: 1k and 5k games, batch 4096/64/10, 1 pass, weight decay, SGD"
Cell "v1k_b4096" @("--batch", "4096", "--epochs", "4", "--games-limit", "1000")
Cell "v1k_b64" @("--batch", "64", "--epochs", "4", "--games-limit", "1000")
Cell "v5k_b4096" @("--batch", "4096", "--epochs", "4")
Cell "v5k_b64" @("--batch", "64", "--epochs", "4")
Cell "v5k_b64e1" @("--batch", "64", "--epochs", "1")
Cell "v5k_b10e1" @("--batch", "10", "--epochs", "1")
Cell "v5k_b64wd" @("--batch", "64", "--epochs", "4", "--weight-decay", "1e-3")
Cell "v5k_b64sgd" @("--batch", "64", "--epochs", "4", "--optimizer", "sgd", "--momentum", "0.9", "--lr", "0.01")
Cell "v5k_b64_bce" @("--batch", "64", "--epochs", "4", "--target", "outcome")
Log "=== critic matrix done"
