"""Generate the list of playable project cards (BASE + DISCOVERY) as a Java class.

Owner's rule (2026-11-16): a card identity bit is needed in the HAND (nothing else reflects a card that is not built yet),
and only cards of the expansions this player actually plays may appear. `--mode cards` lists the cards per expansion; the
fully-qualified class names come from the source tree.
"""
import glob
import json
import os
import re

root = r'C:\Users\user\IdeaProjects\ares-expedition'
cards_json = os.path.join(r'D:/ares/tmp', 'cards_count.json')
out = os.path.join(root, r'src\main\java\com\terraforming\ares\services\agent\PlayableCards.java')

data = json.load(open(cards_json, encoding='utf-8'))
wanted = []
for expansion in ('BASE', 'DISCOVERY'):
    for name in data.get(expansion, []):
        if name not in wanted:
            wanted.append(name)

# class name -> fully qualified name
fqn = {}
for path in glob.glob(os.path.join(root, 'src', 'main', 'java', '**', '*.java'), recursive=True):
    text = open(path, encoding='utf-8', errors='ignore').read()
    match = re.search(r'public\s+(?:abstract\s+)?class\s+(\w+)', text)
    if not match:
        continue
    name = match.group(1)
    if name in wanted:
        package = re.search(r'^package\s+([\w.]+);', text, re.M)
        if package:
            fqn[name] = package.group(1) + '.' + name

missing = [name for name in wanted if name not in fqn]
resolved = [fqn[name] for name in wanted if name in fqn]
packages = sorted({name.rsplit('.', 1)[0] for name in resolved})

lines = ['package com.terraforming.ares.services.agent;', '']
for package in packages:
    lines.append(f'import {package}.*;')
lines += ['', 'import com.terraforming.ares.model.Card;', '', 'import java.util.List;', '',
          '/**',
          ' * Every project card of the expansions this player actually plays (BASE, DISCOVERY), as classes.',
          ' *',
          ' * Generated from `SelfPlayMain --mode cards` by `ml/gen_playable_cards.py`. It feeds the hand-identity bits: the',
          ' * owner\'s rule is that a card which is not built yet needs its own bit, because nothing else in the observation',
          ' * reflects its presence, and that no card of another expansion may appear.',
          ' */',
          'public final class PlayableCards {', '',
          '    private PlayableCards() {', '    }', '',
          '    public static final List<Class<? extends Card>> ALL = List.of(']
for i, name in enumerate(resolved):
    simple = name.rsplit('.', 1)[1]
    comma = ',' if i + 1 < len(resolved) else ''
    lines.append(f'            {simple}.class{comma}')
lines += ['    );', '}', '']
os.makedirs(os.path.dirname(out), exist_ok=True)
open(out, 'w', encoding='utf-8').write('\n'.join(lines))
print(f'cards wanted {len(wanted)}, resolved {len(resolved)}, missing {len(missing)}')
if missing:
    print('missing:', missing[:20])
