# Standard verification of a critic: one command, the same four measurements every time.
#
#   powershell -File ml/verify_critic.ps1 -Model D:/ares/runs/value/v_x
#
# 1. audit (600 positions): the price of +1 income in MC and its round profile - the quantity that decides whether the
#    critic is worth measuring at all (the rules make it ~15 MC at the median round, falling with the round);
# 2. ranking role, 3 000 games against SMART (the deployed mechanism);
# 3. decider role (the critic chooses build vs pass) with projections, 3 000 games;
# 4. behaviour profile on 150 games: cards per game, phases, phase-3 spends.
param(
  [Parameter(Mandatory = $true)][string]$Model,
  [string]$Label = "",
  [int]$RankGames = 3000,
  [int]$DeciderGames = 3000
)
$root = "C:\Users\user\IdeaProjects\ares-expedition"
Set-Location $root
if (-not $Label) { $Label = Split-Path $Model -Leaf }
$cp = (Get-Content "$root\target\cp.txt" -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
$policy = "D:/ares/runs/policy/r25_rev2"
$out = "D:/ares/tmp/verify_$Label"
New-Item -ItemType Directory -Force -Path $out | Out-Null

# 1. audit
& $java -Xmx6g "-Dares.agent.valueModel=$Model" -cp "target/classes;$cp" `
  com.terraforming.ares.selfplay.SelfPlayMain --mode audit --out "$out/audit.json" 2>&1 | Out-Null
py -3 ml/price_report.py "$out/audit.json" | ForEach-Object { "AUDIT $Label | $_" }

# 2. ranking
& $java -Xmx10g "-Dares.agent.search=oneply" "-Dares.agent.search.symmetric=true" `
  "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS" "-Dares.agent.search.pairs=true" `
  "-Dares.agent.valueModel=$Model" -cp "target/classes;$cp" `
  com.terraforming.ares.selfplay.SelfPlayMain --mode compare --a AGENT --b SMART --games $RankGames `
  --concurrency 16 --out "$out/rank.json" --models $policy 2>&1 | Select-String -Pattern "RESULT" |
  ForEach-Object { "RANK  $Label | " + $_.Line.Substring(0, [Math]::Min(120, $_.Line.Length)) }

# 3. decider with projections
& $java -Xmx10g "-Dares.agent.search=oneply" "-Dares.agent.search.symmetric=true" `
  "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS" "-Dares.agent.search.fullTypes=BUILD_CARD" `
  "-Dares.agent.search.projectBuilds=true" "-Dares.agent.valueModel=$Model" -cp "target/classes;$cp" `
  com.terraforming.ares.selfplay.SelfPlayMain --mode compare --a AGENT --b SMART --games $DeciderGames `
  --concurrency 16 --out "$out/decider.json" --models $policy 2>&1 | Select-String -Pattern "RESULT" |
  ForEach-Object { "DECID $Label | " + $_.Line.Substring(0, [Math]::Min(120, $_.Line.Length)) }

# 4. profile
py -3 ml/check_player.py --model $policy --games 150 --search --value-model $Model --opponent SMART --no-references `
  --keep "$out/profile" 2>&1 | Select-String -Pattern "builds_per_game|build_acceptance|phases|standard_projects" |
  ForEach-Object { "PROF  $Label | " + $_.Line.Trim() }
