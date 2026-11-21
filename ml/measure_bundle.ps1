# Measure one value bundle (3 critics) with the champion's deployed flags against a benchmark.
#
#   powershell -File ml/measure_bundle.ps1 -Name v_vol_b4096 -Critics "D:/ares/runs/value/v_vol_b4096_s1,..."
#
# Why a script: the comparison must differ in ONE thing (the critics). Every earlier lever was measured through the same
# flags, and a typo in one of them is indistinguishable from a weaker model - that is how the batch size stayed untested.
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Critics,
    [string]$Bench = "NETWORK",
    [int]$Games = 2000,
    [int]$Concurrency = 12,
    [string]$Policy = "D:/ares/runs/policy/1118-2332_17",
    [string]$PhaseCritic = "D:/ares/runs/value/v_1118-0058_13_s1"
)

$ErrorActionPreference = "Continue"
$cp = (Get-Content target\cp.txt -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
$types = "BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND,BUILD_PAYMENT,PICK_CORPORATION,BLUE_INPUT,BUILD_INPUT,MULLIGAN"
$flags = @(
    "-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true", "-Dares.agent.search.pairs=true",
    "-Dares.agent.search.phasePlayout=true",
    "-Dares.agent.valueModel=$Critics",
    "-Dares.agent.valueModel.phase=$PhaseCritic",
    "-Dares.agent.search.types=$types"
)
$out = "D:/ares/tmp/${Name}_vs_$($Bench.ToLower()).json"
$log = "D:\ares\tmp\${Name}_vs_$($Bench.ToLower()).log"
foreach ($critic in $Critics.Split(",")) {
    foreach ($file in @("model.json", "weights.bin")) {
        if (-not (Test-Path (Join-Path $critic $file))) {
            Write-Output "MISSING $critic\$file - the bundle would start and then fail after the first game; not launching"
            exit 2
        }
    }
}
Write-Output "=== $Name vs $Bench, $Games games"
& $java -Xmx10g @flags -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain --mode compare `
    --a AGENT --b $Bench --games $Games --concurrency $Concurrency --out $out --models $Policy *> $log
Write-Output "exit=$LASTEXITCODE"
Select-String -Path $log -Pattern "CompareResult\[" | Select-Object -Last 1 | ForEach-Object { $_.Line.Trim() }
