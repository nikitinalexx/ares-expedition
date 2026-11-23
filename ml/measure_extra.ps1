# Measure the champion bundle with ONE extra set of -D flags, so a lever is an A/B and not a new bundle.
#
#   powershell -File ml/measure_extra.ps1 -Name stop200 -Games 2000 -Extra "-Dares.agent.search.fullTypes=BUILD_CARD -Dares.agent.search.stopMargins=BUILD_CARD:200"
#
# Everything else (critics, phase critic, policy, search flags) is byte-for-byte the champion's measure_bundle.ps1.
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Extra,
    [string]$Bench = "NETWORK",
    [int]$Games = 2000,
    [int]$Concurrency = 12,
    [string]$Policy = "D:/ares/runs/policy/1118-2332_17",
    [string]$PhaseCritic = "D:/ares/runs/value/v_1118-0058_13_s1",
    [string]$Critics = "D:/ares/runs/value/v_vol_b4096_s1,D:/ares/runs/value/v_vol_b4096_s2,D:/ares/runs/value/v_vol_b4096_s3"
)
$ErrorActionPreference = "Continue"
Set-Location "C:\Users\user\IdeaProjects\ares-expedition"

$cp = (Get-Content target\cp.txt -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
$types = "BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND,BUILD_PAYMENT,PICK_CORPORATION,BLUE_INPUT,BUILD_INPUT,MULLIGAN"
$flags = @(
    "-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true", "-Dares.agent.search.pairs=true",
    "-Dares.agent.search.phasePlayout=true",
    "-Dares.agent.valueModel=$Critics",
    "-Dares.agent.valueModel.phase=$PhaseCritic",
    "-Dares.agent.search.types=$types"
) + ($Extra -split "\s+" | Where-Object { $_ })
$out = "D:/ares/tmp/${Name}_vs_$($Bench.ToLower()).json"
$log = "D:\ares\tmp\${Name}_vs_$($Bench.ToLower()).log"
& $java -Xmx10g @flags -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain --mode compare `
    --a AGENT --b $Bench --games $Games --concurrency $Concurrency --out $out --models $Policy *> $log
$line = (Select-String -Path $log -Pattern "CompareResult\[" | Select-Object -Last 1).Line
Write-Output "$Name : $line"
