# One bootstrap iteration of the agent, end to end: play -> pack -> critic -> policy -> measure.
#
#   powershell -File ml/bootstrap_loop.ps1 -Name boot4 -Games 20000 `
#       -Policy D:/ares/runs/policy/boot3 -Critic D:/ares/runs/value/v_boot3_mask
#
# Why this exists: with the candidate widths changed, no policy bundle from before the change can be loaded, so the agent
# has to grow from its own games again. Each iteration is the same five steps, and the whole loop is minutes now that the
# corpora are packed (docs/PIPELINE_IO.md):
#
#   1. self-play with the CURRENT policy and critic, search on BUILD_CARD/DRAFT_CARDS (the search decides build-vs-pass,
#      the policy ranks inside the menu - measured: the policy alone stops on 91.5% of cards and builds 0.0 per game);
#   2. one sequential pass into an observation-only pack (ml/pack_dataset.py --candidates) and its bit-exact check;
#   3. the critic on that corpus with the hand identity block masked - it costs the critic 0.002 loss on two corpora
#      (docs/RESULTS.md), and the export gate proves the deployed model ignores the block;
#   4. the policy by cross-entropy on the search's own choices: expert iteration, self-play with our own critic, no
#      benchmark model anywhere;
#   5. the acceptance instruments BEFORE any win rate: the argmax-fires-no-stop diagnostic, check_player.py (cards per
#      game, phases, phase-3 spends) and only then a compare.
#
# Nothing here touches SMART or NETWORK as a teacher, opponent in training or data source: they appear in the compare only.
param(
  [Parameter(Mandatory = $true)][string]$Name,
  [int]$Games = 20000,
  [string]$Policy = "",
  [string]$Critic = "",
  [int]$CompareGames = 1000,
  [string]$Matchups = "AGENT:AGENT:0.85,AGENT:RANDOM:0.15"
)

# A native program may write to stderr on a recoverable event (one failed game out of thousands) and still
# finish: PowerShell must not treat that as fatal. Exit codes are checked explicitly below.
$ErrorActionPreference = "Continue"
$root = "C:\Users\user\IdeaProjects\ares-expedition"
Set-Location $root
$cp = (Get-Content "$root\target\cp.txt" -Raw).Trim()
$java = "C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
$data = "D:/ares/data/$Name"
$pack = "C:/ares/pack/$Name"
$value = "D:/ares/runs/value/v_${Name}_mask"
$model = "D:/ares/runs/policy/$Name"
$out = "D:/ares/tmp/bootstrap_$Name"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$search = @("-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true",
            "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS", "-Dares.agent.search.pairs=true")
if ($Critic) { $search += "-Dares.agent.valueModel=$Critic" }

Write-Host "=== 1. self-play: $Games games -> $data (policy $Policy, critic $Critic)"
if (Test-Path $data) { Remove-Item -Recurse -Force $data }
$play = @("-Xmx10g") + $search + @("-cp", "target/classes;$cp",
  "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "play", "--out", $data,
  "--games", "$Games", "--threads", "14", "--games-per-chunk", "3000", "--matchups", $Matchups,
  "--max-rows-per-game-type", "8")
if ($Policy) { $play += @("--models", $Policy) }
& $java @play 2>&1 | Select-String -Pattern "done:" | ForEach-Object { "PLAY " + $_.Line.Substring(0, [Math]::Min(140, $_.Line.Length)) }
if ($LASTEXITCODE -ne 0) { throw "self-play exited $LASTEXITCODE (see $data/errors.log)" }

Write-Host "=== 2. pack -> $pack"
if (Test-Path $pack) { Remove-Item -Recurse -Force $pack }
py -3 ml/pack_dataset.py --data $data --out $pack --candidates | Select-Object -Last 5
if ($LASTEXITCODE -ne 0) { throw "packing exited $LASTEXITCODE" }
py -3 ml/pack_check.py --pack $pack --data $data --limit-chunks 2 | Select-Object -Last 4
if ($LASTEXITCODE -ne 0) { throw "the pack check failed: the pack does not reproduce the raw chunks" }

Write-Host "=== 3. critic -> $value (hand identity masked)"
Remove-Item -Recurse -Force $value -ErrorAction SilentlyContinue
py -3 ml/train_value.py --packed $pack --out $value --target margin --epochs 4 --hidden 512,256 `
    --names D:/ares/tmp/names_now.json --drop-prefix "cards:in_my_hand_" > "$out/critic.log" 2>&1
if ($LASTEXITCODE -ne 0) { throw "the critic trainer exited $LASTEXITCODE (see $out/critic.log)" }
Select-String -Path "$out/critic.log" -Pattern '"epoch"|dropped_columns|masked_gain|best_epoch' |
  ForEach-Object { "CRITIC " + $_.Line.Trim() } | Select-Object -Last 6

Write-Host "=== 4. policy -> $model (cross-entropy on the search's choices)"
Remove-Item -Recurse -Force $model -ErrorAction SilentlyContinue
py -3 ml/train_policy_ce.py --packed $pack --out $model --epochs 3 > "$out/policy.log" 2>&1
if ($LASTEXITCODE -ne 0) { throw "the policy trainer exited $LASTEXITCODE (see $out/policy.log)" }
Select-String -Path "$out/policy.log" -Pattern "BUILD_CARD|PICK_PHASE|seconds" |
  ForEach-Object { "POLICY " + $_.Line.Trim().Substring(0, [Math]::Min(150, $_.Line.Trim().Length)) } | Select-Object -Last 4

Write-Host "=== 5. does the argmax stop on everything? (the acceptance gate)"
py -3 ml/policy_argmax.py $model $pack BUILD_CARD > "$out/argmax.log" 2>&1
Get-Content "$out/argmax.log" | Select-Object -Last 9 | ForEach-Object { "ARGMAX " + $_ }

Write-Host "=== 6. profile (cards per game, phases) and the win rate"
py -3 ml/check_player.py --model $model --games 60 --opponent SMART --no-references --search --value-model $value `
  > "$out/profile.log" 2>&1
Get-Content "$out/profile.log" | Select-Object -Last 22

$compare = @("-Xmx10g") + $search + @("-cp", "target/classes;$cp",
  "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "compare", "--a", "AGENT", "--b", "SMART",
  "--games", "$CompareGames", "--concurrency", "14", "--out", "$out/compare.json", "--models", $model)
& $java @compare 2>&1 | Select-String -Pattern "RESULT" | Select-Object -First 1 |
  ForEach-Object { "COMPARE " + $_.Line.Substring(0, [Math]::Min(220, $_.Line.Length)) }
Write-Host "done: $Name"
