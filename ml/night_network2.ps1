# The owner's NETWORK recipe, end to end, in chunks: generate random games with EVERY state recorded, train the value with
# a small batch, measure against RANDOM (the floor) and NETWORK (the real benchmark), then repeat with a second chunk.
#
#   powershell -File ml/night_network2.ps1 -Games 10000 -Chunk A
#
# Why random games: this is the corpus the owner's NETWORK learned the price of a position from (20 000 games, millions of
# states). Why the per-step recorder: one state per round was 66 rows per game against his ~1000; the recorder now writes
# 1123 post-states per game (427 distinct after his own dedup), so 10 000 games are ~4.3 M rows, not 0.7 M.
param(
    [int]$Games = 10000,
    [string]$Chunk = "A",
    [int]$Batch = 64,
    [string]$Optimizer = "adam",
    [double]$Lr = 0.001,
    [int]$Epochs = 1,
    [int]$Seeds = 2,
    [int]$MeasureGames = 1000,
    [int]$Threads = 10,
    [int]$ValSteps = 1000,
    [int]$Patience = 8,
    [double]$Dropout = 0.0,
    [string]$BootModel = "",
    [string]$Matchups = "RANDOM:RANDOM:1.0",
    [string]$WarmStart = ""
)

$ErrorActionPreference = "Continue"
$cp = (Get-Content target\cp.txt -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
$dataDir = "D:/ares/data/netd_step10k_$Chunk"
$scratch = "D:/ares/tmp/night_$Chunk"
$outZip = "D:/ares/runs/network3/net_step10k_$Chunk.zip"
$log = "D:\ares\tmp\night_network2_$Chunk.log"

New-Item -ItemType Directory -Force -Path $dataDir, $scratch | Out-Null
Remove-Item "$dataDir\netdata.bin" -ErrorAction SilentlyContinue

function Log($text) {
    $line = "$(Get-Date -Format 'HH:mm:ss') $text"
    Write-Output $line
    Add-Content -Path $log -Value $line -Encoding UTF8
}

Log "=== chunk ${Chunk}: $Games games ($Matchups), every state, binary"
$boot = @()
if ($BootModel -ne "") { $boot = @("-Dares.nn.model=$BootModel") }
& $java -Xmx8g @boot "-Dares.netdata.dir=$dataDir" -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain `
    --mode play --out $scratch --threads $Threads --games-per-chunk 500 --matchups $Matchups `
    --max-rows-per-game-type 1 --games $Games *> "D:\ares\tmp\night_gen_$Chunk.log"
$genExit = $LASTEXITCODE
$size = 0
if (Test-Path "$dataDir\netdata.bin") { $size = [math]::Round((Get-Item "$dataDir\netdata.bin").Length / 1GB, 2) }
Log "generation exit=$genExit, netdata.bin $size GB"

for ($s = 1; $s -le $Seeds; $s++) {
    Log "train seed ${s}: batch $Batch, $Optimizer, lr $Lr, epochs $Epochs"
    $warm = @()
    if ($WarmStart -ne "") { $warm = @("--warm-start", $WarmStart) }
    py -3 ml\train_network_v2_torch.py --csv "$dataDir\netdata.bin" --out $outZip `
        --epochs $Epochs --batch $Batch --lr $Lr --seed $s --optimizer $Optimizer `
        --val-steps $ValSteps --dropout $Dropout --in-ram --patience $Patience @warm `
        *> "D:\ares\tmp\night_train_$($Chunk)_s$s.log"
    $line = (Select-String -Path "D:\ares\tmp\night_train_$($Chunk)_s$s.log" -Pattern "^epoch|^using the best" |
             Select-Object -Last 2 | ForEach-Object { $_.Line.Trim() }) -join " | "
    Log "train exit=$LASTEXITCODE | $line"

    foreach ($bench in @("RANDOM", "NETWORK")) {
        $name = "net_step10k_$($Chunk)_s$s"
        & $java -Xmx10g "-Dares.nn.model=$outZip" -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain `
            --mode compare --a NETWORK_V2 --b $bench --games $MeasureGames --concurrency 12 `
            --out "D:/ares/tmp/${name}_vs_$($bench.ToLower()).json" *> "D:\ares\tmp\${name}_vs_$($bench.ToLower()).log"
        $result = (Select-String -Path "D:\ares\tmp\${name}_vs_$($bench.ToLower()).log" -Pattern "CompareResult\[" |
                   Select-Object -Last 1).Line
        Log "seed $s vs ${bench}: $result"
    }
}

Log "=== chunk $Chunk done"
