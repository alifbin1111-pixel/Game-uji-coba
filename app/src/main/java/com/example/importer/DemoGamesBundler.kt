package com.example.importer

import android.content.Context
import com.example.data.local.GameRepository
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

object DemoGamesBundler {

    suspend fun installSampleGamesIfEmpty(context: Context, repository: GameRepository) = withContext(Dispatchers.IO) {
        val gamesDir = File(context.getExternalFilesDir(null), "games")
        if (!gamesDir.exists()) gamesDir.mkdirs()

        // 1. Install RPG Maker MV Demo: Blade of Destiny
        val rpgDir = File(gamesDir, "rpg_blade_of_destiny")
        if (!rpgDir.exists()) {
            rpgDir.mkdirs()
            createRpgMakerDemo(rpgDir)
            val rpgGame = GameEntity(
                id = "demo_rpg_mv",
                title = "Blade of Destiny",
                gamePath = rpgDir.absolutePath,
                engineType = "RPG_MAKER_MV",
                engineVersion = "1.6.2 (PixiJS)",
                confidence = 0.99f,
                executablePath = "index.html",
                fileSizeBytes = 450000L,
                addedAt = System.currentTimeMillis()
            )
            repository.insertGame(rpgGame)
            repository.saveSettings(
                GameSettingsEntity(
                    gameId = "demo_rpg_mv",
                    translationEnabled = true,
                    virtualControllerEnabled = true,
                    sourceLanguage = "ja",
                    targetLanguage = "id"
                )
            )
        }

        // 2. Install HTML5 / Canvas Arcade: Cyber Neon Dash
        val htmlDir = File(gamesDir, "cyber_neon_dash")
        if (!htmlDir.exists()) {
            htmlDir.mkdirs()
            createHtml5ArcadeDemo(htmlDir)
            val htmlGame = GameEntity(
                id = "demo_html5_neon",
                title = "Cyber Neon Dash",
                gamePath = htmlDir.absolutePath,
                engineType = "HTML5",
                engineVersion = "WebGL / 60FPS Canvas",
                confidence = 0.95f,
                executablePath = "index.html",
                fileSizeBytes = 320000L,
                addedAt = System.currentTimeMillis() - 100000L
            )
            repository.insertGame(htmlGame)
            repository.saveSettings(
                GameSettingsEntity(
                    gameId = "demo_html5_neon",
                    translationEnabled = false,
                    virtualControllerEnabled = true
                )
            )
        }

        // 3. Install Ren'Py Visual Novel: Sakura Memories
        val renpyDir = File(gamesDir, "renpy_sakura")
        if (!renpyDir.exists()) {
            renpyDir.mkdirs()
            createRenPyDemo(renpyDir)
            val renpyGame = GameEntity(
                id = "demo_renpy_sakura",
                title = "Sakura Memories",
                gamePath = renpyDir.absolutePath,
                engineType = "RENPY",
                engineVersion = "Ren'Py 8 (Python 3)",
                confidence = 0.96f,
                executablePath = "index.html",
                fileSizeBytes = 280000L,
                addedAt = System.currentTimeMillis() - 200000L
            )
            repository.insertGame(renpyGame)
            repository.saveSettings(
                GameSettingsEntity(
                    gameId = "demo_renpy_sakura",
                    translationEnabled = true,
                    virtualControllerEnabled = false,
                    sourceLanguage = "ja",
                    targetLanguage = "id"
                )
            )
        }

        // 4. Install Unity Windows Standalone Test Package
        val unityDir = File(gamesDir, "unity_valhalla_test")
        if (!unityDir.exists()) {
            unityDir.mkdirs()
            createUnityMockPackage(unityDir)
            val unityGame = GameEntity(
                id = "demo_unity_test",
                title = "Project Valhalla (Unity x86)",
                gamePath = unityDir.absolutePath,
                engineType = "UNITY",
                engineVersion = "Unity 2022.3 (Windows x86_64)",
                confidence = 0.99f,
                executablePath = "Valhalla.exe",
                fileSizeBytes = 12500000L,
                addedAt = System.currentTimeMillis() - 300000L
            )
            repository.insertGame(unityGame)
            repository.saveSettings(GameSettingsEntity(gameId = "demo_unity_test"))
        }

        // 5. Install Godot .pck Test Package
        val godotDir = File(gamesDir, "godot_aetheria_test")
        if (!godotDir.exists()) {
            godotDir.mkdirs()
            createGodotMockPackage(godotDir)
            val godotGame = GameEntity(
                id = "demo_godot_test",
                title = "Aetheria Realm (Godot)",
                gamePath = godotDir.absolutePath,
                engineType = "GODOT",
                engineVersion = "Godot 4.2",
                confidence = 0.95f,
                executablePath = "index.html",
                fileSizeBytes = 980000L,
                addedAt = System.currentTimeMillis() - 400000L
            )
            repository.insertGame(godotGame)
            repository.saveSettings(GameSettingsEntity(gameId = "demo_godot_test"))
        }
    }

    private fun createRpgMakerDemo(dir: File) {
        val dataDir = File(dir, "data").apply { mkdirs() }
        val jsDir = File(dir, "js").apply { mkdirs() }

        // System.json marker
        File(dataDir, "System.json").writeText("""{"gameTitle":"Blade of Destiny","versionId":1}""")
        // rpg_core.js marker
        File(jsDir, "rpg_core.js").writeText("// RPG Maker MV Core Library Simulation")
        File(jsDir, "rpg_managers.js").writeText("// RPG Maker Managers")

        // Playable HTML5 RPG game with live Japanese dialogue, chest interaction, and virtual gamepad hooks!
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Blade of Destiny</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
                    body { background: #0b0d14; color: #fff; font-family: sans-serif; overflow: hidden; height: 100vh; display: flex; flex-direction: column; }
                    #canvasContainer { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
                    canvas { background: #161b26; border: 2px solid #2a3449; border-radius: 8px; max-width: 100%; max-height: 100%; }
                    #dialogBox {
                        position: absolute; bottom: 20px; left: 5%; width: 90%;
                        background: rgba(13, 17, 26, 0.92); border: 2px solid #00e5ff;
                        border-radius: 10px; padding: 14px; box-shadow: 0 4px 20px rgba(0,229,255,0.25);
                        display: none; animation: fadeIn 0.2s;
                    }
                    #speakerName { font-weight: bold; color: #00e5ff; font-size: 14px; margin-bottom: 4px; }
                    #dialogText { font-size: 15px; line-height: 1.4; color: #f0f4fc; }
                    #dialogChoices { display: flex; gap: 8px; margin-top: 10px; }
                    .choiceBtn { background: #1e293b; border: 1px solid #7c4dff; color: #fff; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: bold; }
                    .choiceBtn:active { background: #7c4dff; }
                    #inGameMenu { position: absolute; top: 12px; right: 12px; background: rgba(15,23,42,0.92); border: 2px solid #7c4dff; border-radius: 10px; padding: 10px; width: 150px; display: none; }
                    .menuItem { padding: 6px 10px; color: #e2e8f0; font-size: 13px; border-bottom: 1px solid #334155; cursor: pointer; font-weight: 500; }
                    .menuItem:hover, .menuItem:active { color: #00e5ff; background: rgba(124,77,255,0.2); }
                    #hudBar { position: absolute; top: 10px; left: 10px; background: rgba(15,23,42,0.8); padding: 4px 10px; border-radius: 6px; font-size: 11px; color: #94a3b8; border: 1px solid #334155; }
                </style>
            </head>
            <body>
                <div id="canvasContainer">
                    <canvas id="gameCanvas" width="640" height="400"></canvas>
                    <div id="hudBar">RPG Maker MV Engine Core | MTool Live Localization Active</div>
                    <div id="dialogBox">
                        <div id="speakerName">村の長老 (Elder)</div>
                        <div id="dialogText">「勇者よ、目覚めの時が来た！古代の洞窟へ向かい、聖なる剣を探すのだ。」</div>
                        <div id="dialogChoices">
                            <button class="choiceBtn" onclick="chooseOption('はい')">はい (Ya)</button>
                            <button class="choiceBtn" onclick="chooseOption('いいえ')">いいえ (Tidak)</button>
                        </div>
                    </div>
                    <div id="inGameMenu">
                        <div class="menuItem" onclick="showDialog('アイテム (Item)', '「所持アイテム: やくそうx3, まほうのせいすいx1」')">アイテム</div>
                        <div class="menuItem" onclick="showDialog('スキル (Skill)', '「習得スキル: ファイア, ヒール」')">スキル</div>
                        <div class="menuItem" onclick="showDialog('装備 (Equip)', '「装備: はがねのつるぎ, かわのよろい」')">装備</div>
                        <div class="menuItem" onclick="showDialog('ステータス (Status)', '「Lv 12 | HP: 120/120 | MP: 45/45 | 攻撃力: 34」')">ステータス</div>
                        <div class="menuItem" onclick="showDialog('セーブ (Save)', '「冒険の書にセーブしました！」')">セーブ</div>
                        <div class="menuItem" onclick="toggleInGameMenu()">閉じる</div>
                    </div>
                </div>

                <script>
                    // Simulate RPG Maker core structures for MTool Live Interceptor
                    window.Bitmap = function() {};
                    window.Bitmap.prototype.drawText = function(text, x, y, maxWidth, lineHeight, align) {
                        ctx.fillText(text, x, y);
                    };
                    window.Window_Base = function() {};
                    window.Window_Command = function() {};

                    const canvas = document.getElementById('gameCanvas');
                    const ctx = canvas.getContext('2d');
                    const dialogBox = document.getElementById('dialogBox');
                    const dialogText = document.getElementById('dialogText');
                    const speakerName = document.getElementById('speakerName');
                    const inGameMenu = document.getElementById('inGameMenu');

                    let player = { x: 5, y: 4, size: 32, hp: 100, gold: 50 };
                    const tileSize = 40;
                    const mapWidth = 16;
                    const mapHeight = 10;

                    const map = [
                        [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                        [1,0,0,0,0,1,0,0,0,0,0,0,0,2,0,1],
                        [1,0,0,3,0,1,0,0,0,0,0,0,0,0,0,1],
                        [1,0,0,0,0,0,0,0,0,1,1,0,0,0,0,1],
                        [1,0,0,0,0,0,0,0,0,1,1,0,0,0,0,1],
                        [1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,1],
                        [1,0,1,1,0,0,0,2,0,0,0,0,0,4,0,1],
                        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
                        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
                        [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                    ];

                    let chestOpened = false;

                    function drawMap() {
                        ctx.clearRect(0, 0, canvas.width, canvas.height);
                        for (let r = 0; r < mapHeight; r++) {
                            for (let c = 0; c < mapWidth; c++) {
                                const tile = map[r][c];
                                if (tile === 1) {
                                    ctx.fillStyle = '#1e293b';
                                    ctx.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                                    ctx.strokeStyle = '#334155';
                                    ctx.strokeRect(c * tileSize, r * tileSize, tileSize, tileSize);
                                } else if (tile === 2) {
                                    ctx.fillStyle = chestOpened ? '#475569' : '#ffb300';
                                    ctx.fillRect(c * tileSize + 8, r * tileSize + 8, 24, 24);
                                } else if (tile === 3) {
                                    ctx.fillStyle = '#00e5ff';
                                    ctx.beginPath();
                                    ctx.arc(c * tileSize + 20, r * tileSize + 20, 14, 0, Math.PI * 2);
                                    ctx.fill();
                                } else if (tile === 4) {
                                    ctx.fillStyle = '#7c4dff';
                                    ctx.fillRect(c * tileSize + 4, r * tileSize + 4, 32, 32);
                                } else {
                                    ctx.fillStyle = '#0f172a';
                                    ctx.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                                }
                            }
                        }

                        // Draw Player
                        ctx.fillStyle = '#00e676';
                        ctx.shadowColor = '#00e676';
                        ctx.shadowBlur = 10;
                        ctx.beginPath();
                        ctx.arc(player.x * tileSize + 20, player.y * tileSize + 20, 16, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.shadowBlur = 0;

                        // Canvas RPG Title overlay button simulation
                        ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
                        ctx.fillRect(450, 20, 170, 70);
                        ctx.strokeStyle = '#7c4dff';
                        ctx.strokeRect(450, 20, 170, 70);
                        ctx.fillStyle = '#00e5ff';
                        ctx.font = 'bold 12px sans-serif';
                        ctx.fillText('所持金: ' + player.gold + ' G', 465, 45);
                        ctx.fillStyle = '#f8fafc';
                        ctx.font = '11px sans-serif';
                        ctx.fillText('コマンド: Xキーでメニュー', 465, 68);
                    }

                    function showDialog(speaker, text) {
                        speakerName.innerText = speaker;
                        dialogText.innerText = text;
                        dialogBox.style.display = 'block';
                        if (window.GameBridgeNative) {
                            window.GameBridgeNative.onCaptureText(text, 'DIALOG');
                        }
                    }

                    function hideDialog() {
                        dialogBox.style.display = 'none';
                    }

                    function toggleInGameMenu() {
                        if (inGameMenu.style.display === 'block') {
                            inGameMenu.style.display = 'none';
                        } else {
                            inGameMenu.style.display = 'block';
                        }
                    }

                    function chooseOption(choice) {
                        hideDialog();
                        if (choice === 'はい') {
                            showDialog('村の長老 (Elder)', '「よく言ってくれた！さあ、この回復薬を受け取って出発せよ！」');
                        } else {
                            showDialog('村の長老 (Elder)', '「無理はするな。準備ができたらまた声をかけてくれ。」');
                        }
                    }

                    function move(dx, dy) {
                        if (dialogBox.style.display === 'block') {
                            hideDialog();
                            return;
                        }
                        const nx = player.x + dx;
                        const ny = player.y + dy;
                        if (nx >= 0 && nx < mapWidth && ny >= 0 && ny < mapHeight) {
                            const target = map[ny][nx];
                            if (target === 1) return;
                            if (target === 3) {
                                showDialog('村の長老 (Elder)', '「勇者よ、よくぞ来た！魔王を倒すため、聖なる剣を受け取るがよい！」');
                                return;
                            }
                            if (target === 2) {
                                if (!chestOpened) {
                                    chestOpened = true;
                                    player.gold += 100;
                                    showDialog('宝箱 (Treasure Chest)', '「宝箱を開けた！100ゴールドと回復薬を手に入れた！」');
                                } else {
                                    showDialog('宝箱 (Treasure Chest)', '「宝箱は空っぽだ。」');
                                }
                                return;
                            }
                            if (target === 4) {
                                showDialog('ダンジョン入口 (Dungeon)', '「ダンジョンの扉は固く閉ざされている。鍵が必要だ。」');
                                return;
                            }
                            player.x = nx;
                            player.y = ny;
                            drawMap();
                        }
                    }

                    function interact() {
                        if (dialogBox.style.display === 'block') {
                            hideDialog();
                        } else {
                            const coords = [[0,-1], [0,1], [-1,0], [1,0]];
                            for (let c of coords) {
                                const checkX = player.x + c[0];
                                const checkY = player.y + c[1];
                                if (checkX >= 0 && checkX < mapWidth && checkY >= 0 && checkY < mapHeight) {
                                    if (map[checkY][checkX] === 3) {
                                        showDialog('村の長老 (Elder)', '「勇者よ、古代の洞窟に潜む邪悪なドラゴンを討伐してくれ！」');
                                        return;
                                    }
                                }
                            }
                            showDialog('プレイヤー (Hero)', '「周りを調べたが、特に怪しいものは見当たらない。」');
                        }
                    }

                    // Native GamePad & Keyboard Handler
                    window.addEventListener('keydown', (e) => {
                        if (e.key === 'ArrowUp' || e.key === 'w') move(0, -1);
                        if (e.key === 'ArrowDown' || e.key === 's') move(0, 1);
                        if (e.key === 'ArrowLeft' || e.key === 'a') move(-1, 0);
                        if (e.key === 'ArrowRight' || e.key === 'd') move(1, 0);
                        if (e.key === 'Enter' || e.key === 'z' || e.key === ' ') interact();
                        if (e.key === 'Escape' || e.key === 'x') toggleInGameMenu();
                    });

                    canvas.addEventListener('click', (e) => {
                        const rect = canvas.getBoundingClientRect();
                        const clickX = Math.floor((e.clientX - rect.left) / (rect.width / mapWidth));
                        const clickY = Math.floor((e.clientY - rect.top) / (rect.height / mapHeight));
                        const dx = Math.sign(clickX - player.x);
                        const dy = Math.sign(clickY - player.y);
                        if (dx !== 0) move(dx, 0);
                        else if (dy !== 0) move(0, dy);
                    });

                    window.GameBridgeController = {
                        pressUp: () => move(0, -1),
                        pressDown: () => move(0, 1),
                        pressLeft: () => move(-1, 0),
                        pressRight: () => move(1, 0),
                        pressA: () => interact(),
                        pressB: () => hideDialog(),
                        pressX: () => toggleInGameMenu(),
                        pressY: () => showDialog('セーブ (Save)', '「進捗状況をスロット1に保存しました！」')
                    };

                    drawMap();
                    setTimeout(() => {
                        showDialog('システム (System)', '「RPG Maker MV ランタイムが正常にロードされました。バーチャルコントローラーまたはタッチで操作できます。」');
                    }, 500);
                </script>
            </body>
            </html>
        """.trimIndent()

        File(dir, "index.html").writeText(htmlContent)
    }

    private fun createHtml5ArcadeDemo(dir: File) {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Cyber Neon Dash</title>
                <style>
                    * { margin:0; padding:0; box-sizing:border-box; }
                    body { background: #05070a; color: #fff; overflow:hidden; display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; font-family:sans-serif; }
                    canvas { background: #0b0f19; border: 2px solid #00e5ff; border-radius: 12px; max-width: 96%; max-height: 96%; }
                </style>
            </head>
            <body>
                <canvas id="arcadeCanvas" width="480" height="640"></canvas>
                <script>
                    const canvas = document.getElementById('arcadeCanvas');
                    const ctx = canvas.getContext('2d');
                    let score = 0, highScore = 0, gameOver = false;
                    let ship = { x: 240, y: 550, size: 24, speed: 6 };
                    let meteors = [];

                    function spawnMeteor() {
                        meteors.push({
                            x: Math.random() * (canvas.width - 40) + 20,
                            y: -20,
                            size: Math.random() * 20 + 15,
                            speed: Math.random() * 3 + 3,
                            color: ['#ff4081', '#b388ff', '#ffb300'][Math.floor(Math.random()*3)]
                        });
                    }

                    let lastSpawn = 0;
                    function update(time) {
                        if (!gameOver) {
                            if (time - lastSpawn > 600) {
                                spawnMeteor();
                                lastSpawn = time;
                                score += 10;
                            }

                            for (let i = meteors.length - 1; i >= 0; i--) {
                                meteors[i].y += meteors[i].speed;
                                // Collision
                                const dist = Math.hypot(ship.x - meteors[i].x, ship.y - meteors[i].y);
                                if (dist < ship.size + meteors[i].size - 8) {
                                    gameOver = true;
                                    if (score > highScore) highScore = score;
                                }
                                if (meteors[i].y > canvas.height + 50) meteors.splice(i, 1);
                            }
                        }

                        // Render
                        ctx.fillStyle = 'rgba(11, 15, 25, 0.3)';
                        ctx.fillRect(0, 0, canvas.width, canvas.height);

                        // Draw Ship
                        ctx.fillStyle = '#00e5ff';
                        ctx.shadowColor = '#00e5ff';
                        ctx.shadowBlur = 15;
                        ctx.beginPath();
                        ctx.moveTo(ship.x, ship.y - ship.size);
                        ctx.lineTo(ship.x - ship.size, ship.y + ship.size);
                        ctx.lineTo(ship.x + ship.size, ship.y + ship.size);
                        ctx.closePath();
                        ctx.fill();

                        // Draw Meteors
                        for (let m of meteors) {
                            ctx.fillStyle = m.color;
                            ctx.shadowColor = m.color;
                            ctx.shadowBlur = 10;
                            ctx.beginPath();
                            ctx.arc(m.x, m.y, m.size, 0, Math.PI * 2);
                            ctx.fill();
                        }
                        ctx.shadowBlur = 0;

                        // HUD
                        ctx.fillStyle = '#ffffff';
                        ctx.font = '16px monospace';
                        ctx.fillText('SCORE: ' + score, 20, 35);
                        ctx.fillText('BEST: ' + highScore, 20, 60);

                        if (gameOver) {
                            ctx.fillStyle = 'rgba(0,0,0,0.7)';
                            ctx.fillRect(0,0,canvas.width,canvas.height);
                            ctx.fillStyle = '#ff4081';
                            ctx.font = 'bold 32px sans-serif';
                            ctx.textAlign = 'center';
                            ctx.fillText('GAME OVER', canvas.width/2, canvas.height/2 - 20);
                            ctx.fillStyle = '#00e5ff';
                            ctx.font = '16px sans-serif';
                            ctx.fillText('Tekan tombol A atau Layar untuk Restart', canvas.width/2, canvas.height/2 + 20);
                            ctx.textAlign = 'left';
                        }

                        requestAnimationFrame(update);
                    }

                    function restart() {
                        meteors = [];
                        score = 0;
                        gameOver = false;
                        ship.x = 240;
                    }

                    window.GameBridgeController = {
                        pressLeft: () => { ship.x = Math.max(30, ship.x - 30); },
                        pressRight: () => { ship.x = Math.min(canvas.width - 30, ship.x + 30); },
                        pressUp: () => { ship.y = Math.max(50, ship.y - 30); },
                        pressDown: () => { ship.y = Math.min(canvas.height - 50, ship.y + 30); },
                        pressA: () => { if (gameOver) restart(); },
                        pressB: () => { restart(); }
                    };

                    canvas.addEventListener('touchmove', (e) => {
                        const rect = canvas.getBoundingClientRect();
                        const touch = e.touches[0];
                        ship.x = (touch.clientX - rect.left) * (canvas.width / rect.width);
                    });
                    canvas.addEventListener('click', () => { if (gameOver) restart(); });

                    requestAnimationFrame(update);
                </script>
            </body>
            </html>
        """.trimIndent()
        File(dir, "index.html").writeText(htmlContent)
    }

    private fun createRenPyDemo(dir: File) {
        val gameDir = File(dir, "game").apply { mkdirs() }
        File(gameDir, "options.rpy").writeText("define config.name = _('Sakura Memories')\ndefine config.version = '1.0'")
        File(gameDir, "script.rpy").writeText("""
            # Ren'Py Visual Novel Script
            label start:
                scene bg sakura_school with dissolve
                show aoi happy at center
                aoi "春の風がとても心地いいね、先輩！(Angin musim semi terasa sangat nyaman ya, Senpai!)"
                menu:
                    "一緒に桜を見に行こう (Ayo pergi melihat bunga sakura bersama)":
                        jump route_date
                    "図書館で勉強しよう (Ayo belajar di perpustakaan)":
                        jump route_study
        """.trimIndent())

        // Playable Ren'Py Visual Novel Web Runner
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Sakura Memories</title>
                <style>
                    * { margin:0; padding:0; box-sizing:border-box; user-select:none; }
                    body { background: #120d1c; color:#fff; font-family:sans-serif; height:100vh; overflow:hidden; display:flex; align-items:center; justify-content:center; }
                    #vnContainer { position:relative; width:100%; max-width:720px; height:100%; max-height:450px; background:#221538; border:2px solid #ff4081; border-radius:12px; overflow:hidden; display:flex; flex-direction:column; justify-content:flex-end; }
                    #sceneArt { position:absolute; top:0; left:0; width:100%; height:100%; background: linear-gradient(135deg, #311b92, #880e4f); display:flex; align-items:center; justify-content:center; }
                    #avatar { width:140px; height:200px; background:#ff80ab; border-radius:70px 70px 0 0; border:4px solid #fff; display:flex; align-items:center; justify-content:center; font-size:40px; }
                    #dialogueCard { position:relative; z-index:10; background:rgba(18, 13, 28, 0.95); border-top:3px solid #ff4081; padding:18px; min-height:130px; cursor:pointer; }
                    #charName { color:#ff4081; font-weight:bold; font-size:16px; margin-bottom:6px; }
                    #charLine { color:#fce4ec; font-size:16px; line-height:1.5; }
                    #choiceBox { position:absolute; top:35%; left:10%; width:80%; z-index:20; display:none; flex-direction:column; gap:10px; }
                    .choiceBtn { background:#ff4081; color:#fff; border:none; padding:12px; border-radius:8px; font-size:14px; font-weight:bold; cursor:pointer; }
                </style>
            </head>
            <body>
                <div id="vnContainer">
                    <div id="sceneArt">
                        <div id="avatar">🌸</div>
                    </div>
                    <div id="choiceBox">
                        <button class="choiceBtn" onclick="choose(0)">1. 一緒に桜を見に行こう (Ayo pergi melihat sakura bersama)</button>
                        <button class="choiceBtn" onclick="choose(1)">2. 図書館で勉強しよう (Ayo belajar di perpustakaan)</button>
                    </div>
                    <div id="dialogueCard" onclick="nextLine()">
                        <div id="charName">葵 (Aoi)</div>
                        <div id="charLine">春の風がとても心地いいね、先輩！放課後はどこかへ寄って行かない？</div>
                    </div>
                </div>
                <script>
                    const lines = [
                        { name: "葵 (Aoi)", text: "春の風がとても心地いいね、先輩！放課後はどこかへ寄って行かない？" },
                        { name: "主人公 (Protagonist)", text: "そうだね、今日は天気がいいからね。" },
                        { name: "葵 (Aoi)", text: "先輩、私たちの次の目的地はどうする？" }
                    ];
                    let step = 0;
                    function nextLine() {
                        step++;
                        if (step < lines.length) {
                            document.getElementById('charName').innerText = lines[step].name;
                            document.getElementById('charLine').innerText = lines[step].text;
                            if (window.GameBridgeNative) {
                                window.GameBridgeNative.onTextExtracted(lines[step].text, 'ja');
                            }
                        } else if (step === lines.length) {
                            document.getElementById('choiceBox').style.display = 'flex';
                        }
                    }
                    function choose(idx) {
                        document.getElementById('choiceBox').style.display = 'none';
                        if (idx === 0) {
                            document.getElementById('charName').innerText = "葵 (Aoi)";
                            document.getElementById('charLine').innerText = "やったぁ！満開の桜並木へ行きましょう、先輩！";
                        } else {
                            document.getElementById('charName').innerText = "葵 (Aoi)";
                            document.getElementById('charLine').innerText = "ふふっ、先輩は本当に真面目ですね！一緒に頑張りましょう！";
                        }
                    }
                    window.GameBridgeController = {
                        pressA: () => nextLine(),
                        pressB: () => nextLine()
                    };
                </script>
            </body>
            </html>
        """.trimIndent()
        File(dir, "index.html").writeText(htmlContent)
    }

    private fun createUnityMockPackage(dir: File) {
        val managedDir = File(dir, "Data/Managed").apply { mkdirs() }
        File(dir, "Valhalla.exe").writeText("MZ... Windows PE Binary Header")
        File(dir, "UnityPlayer.dll").writeText("Unity Player Dynamic Link Library")
        File(managedDir, "UnityEngine.dll").writeText("UnityEngine Core Assembly")
        File(managedDir, "Assembly-CSharp.dll").writeText("User Game Scripts")
        File(dir, "globalgamemanagers").writeText("Unity Metadata Container")
    }

    private fun createGodotMockPackage(dir: File) {
        File(dir, "project.godot").writeText("[application]\nconfig/name=\"Aetheria Realm\"\nconfig/features=PackedStringArray(\"4.2\", \"Forward+\")")
        File(dir, "game.pck").writeText("GODOT PCK PACKAGE DUMMY")

        // Also add HTML5 web export version for seamless in-app preview
        File(dir, "index.html").writeText("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Godot Demo</title></head>
            <body style="background:#131722;color:#448aff;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;text-align:center;">
                <div>
                    <h2 style="color:#00e5ff">Godot Engine 4.x PCK Loader</h2>
                    <p style="color:#94a3b8;margin-top:10px;">Package: game.pck (Loaded & Verified)</p>
                    <p style="color:#00e676;margin-top:10px;">✓ WebGL 2.0 Canvas Initialized</p>
                </div>
            </body></html>
        """.trimIndent())
    }
}
