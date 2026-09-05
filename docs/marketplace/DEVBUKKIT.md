<!--
AuraUtils — DevBukkit / CurseForge description (WYSIWYG HTML).
Paste into https://dev.bukkit.org/projects/1669497

How to paste (do not paste Markdown):
1. Open the project Description editor.
2. Keep the dropdown on WYSIWYG.
3. Click the three dots → Source Code (HTML).
4. Select all, delete, paste this file (skip this comment).
5. Save. Do not copy from a Markdown preview — that injects a copy-button tag and collapses the YAML.

CraftBukkit listing. Install AuraUtils-<version>-spigot.jar
-->

<p align="center">
<img src="https://files.catbox.moe/0a2rns.png" alt="AuraUtils" />
</p>

<p align="center">
<a href="https://github.com/TamaWish/AuraUtils/releases"><img src="https://img.shields.io/github/v/release/TamaWish/AuraUtils?style=flat-square&amp;label=Release" alt="Release" /></a>
<a href="https://www.java.com"><img src="https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&amp;logo=openjdk&amp;logoColor=white" alt="Java 21+" /></a>
<a href="https://dev.bukkit.org/projects/1669497"><img src="https://img.shields.io/badge/CraftBukkit-1.21.x%2B%20%2F%2026.x%2B-blue?style=flat-square" alt="CraftBukkit" /></a>
<a href="https://github.com/TamaWish/AuraUtils/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="MIT" /></a>
</p>

<p align="center">
<a href="https://www.spigotmc.org/resources/aurautils-spigot-paper-folia.138193/"><img src="https://img.shields.io/spiget/downloads/138193?style=flat-square&amp;label=Spigot%20downloads&amp;color=yellow" alt="Spigot downloads" /></a>
<a href="https://modrinth.com/project/W2WxC84B"><img src="https://img.shields.io/badge/dynamic/json?style=flat-square&amp;color=1bd96a&amp;label=Modrinth&amp;query=downloads&amp;url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FW2WxC84B&amp;suffix=%20downloads" alt="Modrinth downloads" /></a>
<a href="https://github.com/TamaWish/AuraUtils"><img src="https://img.shields.io/github/stars/TamaWish/AuraUtils?style=flat-square&amp;logo=github" alt="GitHub stars" /></a>
</p>

<p align="center">
<a href="https://hangar.papermc.io/Lozaine/AuraUtils"><img src="https://img.shields.io/hangar/dt/AuraUtils?style=flat-square" alt="Hangar" /></a>
<a href="https://dev.bukkit.org/projects/1669497"><img src="https://img.shields.io/curseforge/dt/1669497?style=flat-square&amp;label=BukkitDev%20downloads" alt="BukkitDev downloads" /></a>
</p>

<p align="center">
<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/fBpYOXKZQlc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen="allowfullscreen"></iframe>
</p>

<p>Lightweight homes, warps, TPA, back, RTP, timber, player inventories, and player toggles for <strong>CraftBukkit</strong>.</p>

<p>Players get a shared teleport countdown, a simple GUI, and translatable messages. Operators get warps, home limits, reload, and an optional GitHub update notice.</p>

<p>AuraUtils does <strong>not</strong> include an economy. Pair it with <a href="https://github.com/TamaWish/PureEconomy">PureEconomy</a> for a lightweight utilities + economy stack. They talk through <a href="https://www.spigotmc.org/resources/vault.4536/">Vault</a>; AuraUtils charges PureEconomy's default currency. Neither plugin requires the other.</p>

<p><img src="https://files.catbox.moe/foxbd8.png" alt="INSTALLATION" /></p>

<p>Install <strong>AuraUtils-&lt;version&gt;-spigot.jar</strong> in <code>plugins/</code>. The spigot and paper-folia filenames are copies of the same shaded bytecode; CraftBukkit behavior is detected at runtime.</p>

<table>
<thead>
<tr><th>Server</th><th>Install</th></tr>
</thead>
<tbody>
<tr><td>CraftBukkit / Bukkit</td><td><code>AuraUtils-&lt;version&gt;-spigot.jar</code></td></tr>
</tbody>
</table>

<ol>
<li>Place the JAR in <code>plugins/</code>.</li>
<li>Restart the server.</li>
<li>Edit <code>plugins/AuraUtils/config.yml</code> if you want.</li>
<li>On first start the plugin copies <code>lang/en.yml</code>. Translate it or add another locale and set <code>language:</code>.</li>
<li>Run <code>/aura reload</code> (<code>aura.admin</code>) after config or language edits.</li>
</ol>

<p><img src="https://files.catbox.moe/p0ee9p.png" alt="FEATURES" /></p>

<ul>
<li>Homes with optional GUI, name rules, overwrite confirm, and optional limits</li>
<li>Server warps with optional GUI and overwrite confirm</li>
<li>Timed TPA plus a trusted list that auto-accepts requests</li>
<li><code>/back</code>, safe <code>/rtp</code>, god, fly, nofall, nohunger, timber</li>
<li>Extra player inventories (<code>/inv 1</code>, <code>/inv 2</code>, …) with rank limits</li>
<li><code>/menu</code> utility GUI</li>
<li>Shared countdown: chat, action bar, title, cancel on move/damage, rising-pitch sounds</li>
<li><code>aura.teleport.bypass</code> for instant teleports</li>
<li>Optional Vault economy costs for home, warp, TPA, RTP, and back</li>
<li>All player-facing text in <code>lang/en.yml</code></li>
</ul>

<p>Trusted TPA: <code>/tpatrust &lt;player&gt;</code> adds them to your list. They can <code>/tpa</code> you without <code>/tpaccept</code>. Manage with <code>/tpatrust list</code> and <code>/tpauntrust &lt;player&gt;</code>. Cancel a countdown or an outgoing TPA with <code>/tpacancel</code> (<code>/tpcancel</code>, <code>/auracancel</code>).</p>

<p>Timber: with <code>timber.enabled: true</code> (default), chopping a log with an axe fells the connected tree. <code>/timber</code> turns it off for you. Sneak to chop a single log.</p>

<p>Player inventories: <code>/inv 1</code> is a personal double chest. Normal ranks get that one inventory. Give a rank more with a single node — <code>aura.inv.3</code> allows <code>/inv 1</code>–<code>3</code>. Works with LuckPerms or any other permission plugin.</p>

<p><img src="https://files.catbox.moe/pwsw8n.png" alt="COMMANDS" /></p>

<table>
<thead>
<tr><th>Command</th><th>Permission</th><th>Description</th></tr>
</thead>
<tbody>
<tr><td><code>/home [name|list]</code></td><td><code>aura.home</code></td><td>Teleport to a home or open the home GUI</td></tr>
<tr><td><code>/sethome &lt;name&gt;</code></td><td><code>aura.home.set</code></td><td>Create or update a home</td></tr>
<tr><td><code>/delhome &lt;name&gt;</code></td><td><code>aura.home.delete</code></td><td>Delete a home</td></tr>
<tr><td><code>/warp [name|list]</code></td><td><code>aura.warp</code></td><td>Teleport to a warp or open the warp GUI</td></tr>
<tr><td><code>/setwarp &lt;name&gt;</code></td><td><code>aura.warp.set</code></td><td>Create or update a warp</td></tr>
<tr><td><code>/delwarp &lt;name&gt;</code></td><td><code>aura.warp.delete</code></td><td>Delete a warp</td></tr>
<tr><td><code>/tpa &lt;player&gt;|list</code></td><td><code>aura.tpa</code></td><td>Send a TPA request or open the TPA GUI</td></tr>
<tr><td><code>/tpaccept</code> / <code>/tpadeny</code></td><td><code>aura.tpa</code></td><td>Accept or deny a pending TPA</td></tr>
<tr><td><code>/tpacancel</code></td><td><code>aura.use</code></td><td>Cancel a countdown or outgoing TPA</td></tr>
<tr><td><code>/tpatrust</code> / <code>/tpauntrust</code></td><td><code>aura.tpa.trust</code></td><td>Manage your trusted TPA list</td></tr>
<tr><td><code>/back</code></td><td><code>aura.back</code></td><td>Return to the last teleport location</td></tr>
<tr><td><code>/rtp</code></td><td><code>aura.rtp</code></td><td>Random safe teleport</td></tr>
<tr><td><code>/god [player]</code></td><td><code>aura.god</code></td><td>Toggle invincibility</td></tr>
<tr><td><code>/fly [player]</code></td><td><code>aura.fly</code></td><td>Toggle flight</td></tr>
<tr><td><code>/nofall [player]</code></td><td><code>aura.nofall</code></td><td>Toggle fall damage</td></tr>
<tr><td><code>/nohunger [player]</code></td><td><code>aura.nohunger</code></td><td>Toggle hunger depletion</td></tr>
<tr><td><code>/timber [player]</code></td><td><code>aura.timber</code></td><td>Toggle chopping a whole tree from one log</td></tr>
<tr><td><code>/inv [number|list]</code></td><td><code>aura.inv</code></td><td>Open a personal extra inventory</td></tr>
<tr><td><code>/menu</code></td><td><code>aura.menu</code></td><td>Open the utility GUI</td></tr>
<tr><td><code>/aura [reload]</code></td><td><code>aura.use</code> / <code>aura.admin</code></td><td>Command list; reload config and language</td></tr>
</tbody>
</table>

<p><img src="https://files.catbox.moe/joazzp.png" alt="REQUIREMENTS" /></p>

<ul>
<li>Java <strong>21+</strong> (Minecraft <strong>26.1+</strong> servers need Java 25)</li>
<li>Minecraft <strong>1.21.x</strong> and <strong>26.1 / 26.2</strong></li>
<li>CraftBukkit</li>
<li>Optional: Vault + an economy plugin if you want paid teleports / set-home / set-warp</li>
</ul>

<p><img src="https://files.catbox.moe/pwsw8n.png" alt="CONFIGURATION" /></p>

<table>
<tr><td>
tpa:<br />
&nbsp;&nbsp;timeout: 60<br />
&nbsp;&nbsp;trusted-max: 50<br />
&nbsp;&nbsp;trusted-instant: false<br />
<br />
homes:<br />
&nbsp;&nbsp;default-limit: 3<br />
&nbsp;&nbsp;limits: []<br />
<br />
inventories:<br />
&nbsp;&nbsp;enabled: true<br />
&nbsp;&nbsp;rows: 6<br />
&nbsp;&nbsp;max: 10<br />
&nbsp;&nbsp;default-limit: 1<br />
&nbsp;&nbsp;limits: []<br />
<br />
rtp:<br />
&nbsp;&nbsp;radius: 2000<br />
&nbsp;&nbsp;minDistance: 250<br />
&nbsp;&nbsp;attempts: 30<br />
&nbsp;&nbsp;generate-unloaded: true<br />
&nbsp;&nbsp;max-sync-generations: 3<br />
<br />
teleport:<br />
&nbsp;&nbsp;countdown: 5<br />
&nbsp;&nbsp;countdown-display: both<br />
&nbsp;&nbsp;chat-at: [3, 2, 1]<br />
&nbsp;&nbsp;title: true<br />
&nbsp;&nbsp;cancel-on-move: true<br />
&nbsp;&nbsp;sound: true<br />
&nbsp;&nbsp;sound-rising-pitch: true<br />
<br />
language: en<br />
prefix: "&amp;8[&amp;bAura&amp;8] &amp;r"<br />
<br />
update-checker:<br />
&nbsp;&nbsp;enabled: true<br />
<br />
economy:<br />
&nbsp;&nbsp;enabled: true<br />
&nbsp;&nbsp;notify: true<br />
&nbsp;&nbsp;costs:<br />
&nbsp;&nbsp;&nbsp;&nbsp;home: 0.0<br />
&nbsp;&nbsp;&nbsp;&nbsp;sethome: 0.0<br />
&nbsp;&nbsp;&nbsp;&nbsp;warp: 0.0<br />
&nbsp;&nbsp;&nbsp;&nbsp;setwarp: 0.0<br />
&nbsp;&nbsp;&nbsp;&nbsp;tpa: 0.0<br />
&nbsp;&nbsp;&nbsp;&nbsp;rtp: 0.0<br />
&nbsp;&nbsp;&nbsp;&nbsp;back: 0.0<br />
<br />
timber:<br />
&nbsp;&nbsp;enabled: true<br />
&nbsp;&nbsp;require-axe: true<br />
&nbsp;&nbsp;sneak-chops-single: true<br />
&nbsp;&nbsp;break-leaves: true<br />
&nbsp;&nbsp;max-logs: 128<br />
&nbsp;&nbsp;max-leaves: 256
</td></tr>
</table>

<p>Home and warp names are 1–32 letters, numbers, <code>_</code>, or <code>-</code>. Overwriting an existing name asks for clickable <strong>[CONFIRM]</strong> / <strong>[CANCEL]</strong> (30 seconds).</p>

<p><code>chat-at: [3, 2, 1]</code> always announces the start (for example 5), then only those remaining seconds. Action bar and title still update every second.</p>

<p>When <code>update-checker.enabled</code> is true, operators with <code>aura.admin</code> get a clickable chat link if a newer GitHub release exists.</p>

<p>Economy is optional. Costs of <code>0</code> are free. Without Vault (or without an economy provider), every action stays free. Money is taken when the action succeeds; a cancelled countdown is not charged; a failed teleport is refunded. <code>aura.economy.bypass</code> skips costs.</p>

<p><img src="https://files.catbox.moe/rc5ojm.png" alt="PERMISSIONS" /></p>

<table>
<thead>
<tr><th>Permission</th><th>Default</th><th>Description</th></tr>
</thead>
<tbody>
<tr><td><code>aura.use</code></td><td>true</td><td>Basic access / <code>/tpacancel</code></td></tr>
<tr><td><code>aura.menu</code></td><td>true</td><td>Open the utility menu</td></tr>
<tr><td><code>aura.back</code></td><td>true</td><td>Use <code>/back</code></td></tr>
<tr><td><code>aura.warp</code></td><td>true</td><td>Use warp commands</td></tr>
<tr><td><code>aura.warp.set</code></td><td>op</td><td>Create/update warps</td></tr>
<tr><td><code>aura.warp.delete</code></td><td>op</td><td>Delete warps</td></tr>
<tr><td><code>aura.home</code></td><td>true</td><td>Use home commands</td></tr>
<tr><td><code>aura.home.set</code></td><td>true</td><td>Set homes</td></tr>
<tr><td><code>aura.home.delete</code></td><td>true</td><td>Delete homes</td></tr>
<tr><td><code>aura.tpa</code></td><td>true</td><td>Use tpa / tpaccept / tpadeny</td></tr>
<tr><td><code>aura.tpa.trust</code></td><td>true</td><td>Manage trusted TPA list</td></tr>
<tr><td><code>aura.god</code> / <code>aura.god.others</code></td><td>op</td><td>God mode for self / others</td></tr>
<tr><td><code>aura.fly</code> / <code>aura.fly.others</code></td><td>op</td><td>Fly for self / others</td></tr>
<tr><td><code>aura.nofall</code> / <code>aura.nofall.others</code></td><td>op</td><td>Fall damage for self / others</td></tr>
<tr><td><code>aura.nohunger</code> / <code>aura.nohunger.others</code></td><td>op</td><td>Hunger for self / others</td></tr>
<tr><td><code>aura.timber</code> / <code>aura.timber.others</code></td><td>true / op</td><td>Easy tree chopping for self / others</td></tr>
<tr><td><code>aura.inv</code></td><td>true</td><td>Open extra inventories (<code>/inv</code>)</td></tr>
<tr><td><code>aura.inv.&lt;n&gt;</code></td><td>false</td><td>Open inventories 1 through <code>n</code></td></tr>
<tr><td><code>aura.rtp</code></td><td>true</td><td>Random safe teleport</td></tr>
<tr><td><code>aura.teleport.bypass</code></td><td>op</td><td>Skip teleport countdown</td></tr>
<tr><td><code>aura.economy.bypass</code></td><td>op</td><td>Skip Vault economy costs</td></tr>
<tr><td><code>aura.admin</code></td><td>op</td><td>All permissions, including <code>/aura reload</code></td></tr>
</tbody>
</table>

<p><img src="https://files.catbox.moe/22kdgh.png" alt="NOTE" /></p>

<p>Replace the jar with <code>AuraUtils-&lt;version&gt;-spigot.jar</code>. The spigot and paper-folia filenames are the same bytecode; the server type is detected at runtime. After first start, translate <code>plugins/AuraUtils/lang/en.yml</code> or add another <code>lang/&lt;code&gt;.yml</code>. Add <code>language:</code>, <code>homes.default-limit</code>, <code>inventories</code>, <code>update-checker.enabled</code>, <code>economy</code>, and <code>timber</code> to an existing <code>config.yml</code> if those keys are missing.</p>

<p><img src="https://files.catbox.moe/qlbzjk.png" alt="METRICS" /></p>

<p>AuraUtils sends <strong>anonymous</strong> usage stats through <a href="https://bstats.org/plugin/bukkit/AuraUtils/33574">bStats</a> (plugin id <strong>33574</strong>). Charts are public. No player names, UUIDs, IPs, chat, or inventory contents.</p>

<p><strong>Every bStats plugin reports</strong> player count, online-mode, Minecraft version, server software, Java version, OS, CPU cores, country, and plugin version.</p>

<p><strong>AuraUtils also reports</strong> (config and usage only): teleport countdown length and display mode; cancel-on-move / cancel-on-damage / teleport sound; RTP countdown and TPA timeout; whether timber, player inventories, and Vault economy are enabled or hooked; how many currently online players have god, fly, nofall, or nohunger on; how many server warps exist.</p>

<p><strong>Opt out</strong> for every bStats plugin on the server: <code>plugins/bStats/config.yml</code> → <code>enabled: false</code>, then restart. There is no separate AuraUtils metrics switch.</p>

<p><a href="https://github.com/TamaWish/AuraUtils/releases">Downloads</a> · <a href="https://github.com/TamaWish/AuraUtils/blob/main/CHANGELOG.md">Changelog</a> · <a href="https://discord.gg/kbKZzxDETU">Support</a></p>

<p>Copyright: <strong>Lozaine@Tamawish</strong> · MIT</p>
