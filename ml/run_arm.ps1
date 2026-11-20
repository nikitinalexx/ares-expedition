# One practice arm end to end: generate -> fine-tune -> measure -> health panel.
#
# Why a script: the R (red) and E (economy) arms must be identical except for the colour, or the comparison is worthless. Every step is the one
# used by the practice loop; nothing here changes the player, only which games are produced.
#
#   powershell -File ml/run_arm.ps1 -Name red -Colour red -Rate 0.3 -Games 5000
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Colour,
    [double]$Rate = 0.3,
    [int]$Games = 5000,
    [string]$Base = "D:/ares/runs/policy/1120_clean5000",
    [string]$Out = "",
    [int]$Threads = 14
)

$ErrorActionPreference = "Continue"
$cp = (Get-Content target\cp.txt -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
$types = "BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND,BUILD_PAYMENT,PICK_CORPORATION,BLUE_INPUT,BUILD_INPUT,MULLIGAN"
if ($Out -eq "") { $Out = "D:/ares/data/1120_arm_$Name" }
$policy = "D:/ares/runs/policy/1120_arm_$Name"
$critics = "D:/ares/runs/value/v_1118-2332_17_s1,D:/ares/runs/value/v_1118-2332_17_s2,D:/ares/runs/value/v_1118-2332_17_s3"
$flags = @(
    "-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true", "-Dares.agent.search.pairs=true",
    "-Dares.agent.search.phasePlayout=true", "-Dares.agent.search.playout=MULLIGAN",
    "-Dares.agent.search.junkGate=MULLIGAN", "-Dares.agent.search.junkMargin=50",
    "-Dares.agent.valueModel=$critics",
    "-Dares.agent.valueModel.phase=D:/ares/runs/value/v_1118-0058_13_s1",
    "-Dares.agent.valueModel.mulligan=D:/ares/runs/value/v_1119_hand",
    "-Dares.agent.search.types=$types"
)

New-Item -ItemType Directory -Force -Path $Out | Out-Null
Write-Output "=== $Name arm: colour $Colour at rate $Rate, $Games games, base $Base"
$t0 = Get-Date
& $java -Xmx10g @flags "-Dares.agent.preferColor=$Colour" "-Dares.agent.colorNudgeRate=$Rate" -cp "target/classes;$cp" `
    com.terraforming.ares.selfplay.SelfPlayMain --mode play --out $Out --threads $Threads --games-per-chunk 250 `
    --count-chunks-from $Out --matchups "AGENT:AGENT:1.0" --max-rows-per-game-type 8 --models $Base --games $Games *> "D:/ares/tmp/arm_$Name.play.log"
$playSeconds = [math]::Round(((Get-Date) - $t0).TotalSeconds, 0)
Write-Output "  play: $playSeconds s"

Write-Output "  fine-tune on the whole corpus"
& py -3 ml\train_policy_ce.py --data $Out --out $policy --prev $Base --epochs 3 --max-chunks-per-type 20 *> "D:/ares/tmp/arm_$Name.train.log"

Write-Output "  measure vs SMART, 5000"
& $java -Xmx10g @flags -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain --mode compare `
    --a AGENT --b SMART --games 5000 --concurrency 12 --out "D:/ares/tmp/arm_$Name.smart.json" --models $policy *> "D:/ares/tmp/arm_$Name.smart.log"
$smart = (Select-String -Path "D:/ares/tmp/arm_$Name.smart.log" -Pattern "\[COMPARE\] RESULT" | Select-Object -Last 1).Line
Write-Output "  SMART: $smart"

Write-Output "  measure vs NETWORK, 1000"
& $java -Xmx10g @flags -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain --mode compare `
    --a AGENT --b NETWORK --games 1000 --concurrency 12 --out "D:/ares/tmp/arm_$Name.network.json" --models $policy *> "D:/ares/tmp/arm_$Name.network.log"
$network = (Select-String -Path "D:/ares/tmp/arm_$Name.network.log" -Pattern "\[COMPARE\] RESULT" | Select-Object -Last 1).Line
Write-Output "  NETWORK: $network"

Write-Output "  health panel against the clean reference corpus"
& py -3 ml\health.py --data $Out --reference D:/ares/data/1120_clean5000 --chunks 10 *> "D:/ares/tmp/arm_$Name.health.log"
Get-Content "D:/ares/tmp/arm_$Name.health.log" | Select-Object -Last 22 | ForEach-Object { "  $_" }
Write-Output "=== $Name arm done in $([math]::Round(((Get-Date) - $t0).TotalSeconds / 60, 0)) min"
