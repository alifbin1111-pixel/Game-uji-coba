package com.example.translation

object MToolLiveHook {

    /**
     * Generates the MTool-like JavaScript injection script that hooks into
     * RPG Maker MV/MZ engine prototypes and standard HTML5/Canvas rendering pipelines.
     */
    fun getInjectionScript(
        initialCacheJson: String = "{}",
        translateUi: Boolean = true,
        translateDialog: Boolean = true,
        translateMenu: Boolean = true,
        translateBattleUi: Boolean = true
    ): String {
        return """
        (function() {
            if (window.__GAMEBRIDGE_HOOK_INSTALLED__) return;
            window.__GAMEBRIDGE_HOOK_INSTALLED__ = true;
            console.log('[GameBridge-MTool] Initializing Real-Time Live Localization Engine...');

            // Translation Cache in JavaScript memory for 0ms frame rendering
            window.__GB_TRANS_CACHE = $initialCacheJson || {};
            window.__GB_PENDING_QUEUE = new Set();
            window.__GB_SETTINGS = {
                translateUi: $translateUi,
                translateDialog: $translateDialog,
                translateMenu: $translateMenu,
                translateBattleUi: $translateBattleUi
            };

            // Regex for control/escape codes e.g. \C[1], \V[2], \N[1], \I[45], \G, \!, \., \|
            const ESCAPE_CODE_REGEX = /\\[A-Za-z]+\[\d+]|\\[.!|^><{}\\\$]/g;
            const PURE_NUMBER_REGEX = /^\d+[\d\s.,:%/\-]*$/;
            const FILE_PATH_REGEX = /^[A-Za-z0-9_\-/\\]+\.(png|jpg|ogg|m4a|rpgmvp|rpgmvo|json|js)$/i;

            function isTranslatable(text) {
                if (!text || typeof text !== 'string') return false;
                const trimmed = text.trim();
                if (trimmed.length < 1) return false;
                if (PURE_NUMBER_REGEX.test(trimmed)) return false;
                if (FILE_PATH_REGEX.test(trimmed)) return false;
                if (trimmed.startsWith('$') || trimmed.startsWith('window.') || trimmed.startsWith('SceneManager')) return false;
                return true;
            }

            // Extract translatable core while preserving RPG Maker escape sequence wrappers
            function extractCleanText(text) {
                return text.replace(ESCAPE_CODE_REGEX, '').trim();
            }

            // Bridge messenger with debounce
            let debounceTimer = null;
            const queuedTexts = [];

            function sendToNative(text, contextType) {
                if (!isTranslatable(text)) return;
                const trimmed = text.trim();
                if (window.__GB_TRANS_CACHE[trimmed] || window.__GB_PENDING_QUEUE.has(trimmed)) {
                    return;
                }
                window.__GB_PENDING_QUEUE.add(trimmed);
                queuedTexts.push({ text: trimmed, context: contextType });

                if (!debounceTimer) {
                    debounceTimer = setTimeout(function() {
                        debounceTimer = null;
                        if (window.GameBridgeNative && window.GameBridgeNative.onBatchCaptureText) {
                            try {
                                window.GameBridgeNative.onBatchCaptureText(JSON.stringify(queuedTexts));
                            } catch (e) {
                                console.error('[GameBridge-MTool] Bridge error:', e);
                            }
                            queuedTexts.length = 0;
                        } else if (window.GameBridgeNative && window.GameBridgeNative.onCaptureText) {
                            while (queuedTexts.length > 0) {
                                const item = queuedTexts.shift();
                                window.GameBridgeNative.onCaptureText(item.text, item.context);
                            }
                        }
                    }, 50);
                }
            }

            // Public method for Kotlin Native Bridge to push translated strings
            window.__GB_ReceiveTranslation = function(original, translated, contextType) {
                if (!original || !translated) return;
                window.__GB_TRANS_CACHE[original] = translated;
                window.__GB_TRANS_CACHE[original.trim()] = translated.trim();
                window.__GB_PENDING_QUEUE.delete(original);
                window.__GB_PENDING_QUEUE.delete(original.trim());

                // Trigger automatic redraw for active RPG Maker MV/MZ windows
                try {
                    if (window.SceneManager && SceneManager._scene) {
                        const scene = SceneManager._scene;
                        if (scene._windowLayer && Array.isArray(scene._windowLayer.children)) {
                            for (let i = 0; i < scene._windowLayer.children.length; i++) {
                                const win = scene._windowLayer.children[i];
                                if (win && typeof win.refresh === 'function' && win.visible) {
                                    win.refresh();
                                }
                            }
                        }
                    }
                } catch(err) {}
            };

            // Bulk synchronization
            window.__GB_SyncTranslations = function(batchMapJson) {
                try {
                    const batch = JSON.parse(batchMapJson);
                    for (let key in batch) {
                        if (batch.hasOwnProperty(key)) {
                            window.__GB_TRANS_CACHE[key] = batch[key];
                        }
                    }
                } catch(e) {}
            };

            // Helper to translate string if cached, otherwise dispatch to native capture
            function translateOrCapture(str, contextType, allowUi) {
                if (!allowUi || !isTranslatable(str)) return str;
                const trimmed = str.trim();
                if (window.__GB_TRANS_CACHE[trimmed]) {
                    return str.replace(trimmed, window.__GB_TRANS_CACHE[trimmed]);
                }
                if (window.__GB_TRANS_CACHE[str]) {
                    return window.__GB_TRANS_CACHE[str];
                }
                // Check if stripped version is cached
                const clean = extractCleanText(str);
                if (clean.length > 0 && window.__GB_TRANS_CACHE[clean]) {
                    return str.replace(clean, window.__GB_TRANS_CACHE[clean]);
                }
                sendToNative(trimmed, contextType);
                return str;
            }

            // =========================================================================
            // 1. HOOK: Bitmap.prototype.drawText (RPG Maker MV & MZ 2D Canvas Engine)
            // =========================================================================
            function installBitmapHook() {
                if (typeof window.Bitmap !== 'undefined' && window.Bitmap.prototype && window.Bitmap.prototype.drawText) {
                    if (window.Bitmap.prototype._gb_drawText_hooked) return;
                    window.Bitmap.prototype._gb_drawText_hooked = true;
                    const origDrawText = window.Bitmap.prototype.drawText;
                    window.Bitmap.prototype.drawText = function(text, x, y, maxWidth, lineHeight, align) {
                        if (text !== undefined && text !== null) {
                            text = translateOrCapture(String(text), 'CANVAS_UI', window.__GB_SETTINGS.translateUi);
                        }
                        return origDrawText.call(this, text, x, y, maxWidth, lineHeight, align);
                    };
                }
            }

            // =========================================================================
            // 2. HOOK: Window_Command.prototype.commandName (Menu, Battle, Title Buttons)
            // =========================================================================
            function installCommandHook() {
                if (typeof window.Window_Command !== 'undefined' && window.Window_Command.prototype) {
                    if (window.Window_Command.prototype._gb_command_hooked) return;
                    window.Window_Command.prototype._gb_command_hooked = true;
                    const origCommandName = window.Window_Command.prototype.commandName;
                    if (origCommandName) {
                        window.Window_Command.prototype.commandName = function(index) {
                            const original = origCommandName.call(this, index);
                            if (original && typeof original === 'string') {
                                return translateOrCapture(original, 'MENU', window.__GB_SETTINGS.translateMenu);
                            }
                            return original;
                        };
                    }
                }
            }

            // =========================================================================
            // 3. HOOK: Window_Message.prototype.startMessage (RPG Maker Story Dialogue)
            // =========================================================================
            function installMessageHook() {
                if (typeof window.Window_Message !== 'undefined' && window.Window_Message.prototype) {
                    if (window.Window_Message.prototype._gb_msg_hooked) return;
                    window.Window_Message.prototype._gb_msg_hooked = true;
                    const origStartMessage = window.Window_Message.prototype.startMessage;
                    if (origStartMessage) {
                        window.Window_Message.prototype.startMessage = function() {
                            const gm = window['$' + 'gameMessage'];
                            if (window.__GB_SETTINGS.translateDialog && gm && Array.isArray(gm._texts)) {
                                for (let i = 0; i < gm._texts.length; i++) {
                                    const line = gm._texts[i];
                                    if (typeof line === 'string' && isTranslatable(line)) {
                                        gm._texts[i] = translateOrCapture(line, 'DIALOG', true);
                                    }
                                }
                            }
                            return origStartMessage.call(this);
                        };
                    }
                }
            }

            // =========================================================================
            // 4. HOOK: Window_ChoiceList.prototype (Dialogue Choices)
            // =========================================================================
            function installChoiceHook() {
                if (typeof window.Window_ChoiceList !== 'undefined' && window.Window_ChoiceList.prototype) {
                    if (window.Window_ChoiceList.prototype._gb_choice_hooked) return;
                    window.Window_ChoiceList.prototype._gb_choice_hooked = true;
                    const origChoiceName = window.Window_ChoiceList.prototype.commandName;
                    if (origChoiceName) {
                        window.Window_ChoiceList.prototype.commandName = function(index) {
                            const choice = origChoiceName.call(this, index);
                            if (choice && typeof choice === 'string') {
                                return translateOrCapture(choice, 'DIALOG_CHOICE', window.__GB_SETTINGS.translateDialog);
                            }
                            return choice;
                        };
                    }
                }
            }

            // =========================================================================
            // 5. HOOK: Window_Help.prototype.setText / setItem (Tooltips & Descriptions)
            // =========================================================================
            function installHelpHook() {
                if (typeof window.Window_Help !== 'undefined' && window.Window_Help.prototype) {
                    if (window.Window_Help.prototype._gb_help_hooked) return;
                    window.Window_Help.prototype._gb_help_hooked = true;
                    const origSetText = window.Window_Help.prototype.setText;
                    if (origSetText) {
                        window.Window_Help.prototype.setText = function(text) {
                            if (text && typeof text === 'string') {
                                text = translateOrCapture(text, 'TOOLTIP', window.__GB_SETTINGS.translateUi);
                            }
                            return origSetText.call(this, text);
                        };
                    }
                }
            }

            // =========================================================================
            // 6. HOOK: Window_BattleLog.prototype.addText (Battle Actions & Events)
            // =========================================================================
            function installBattleLogHook() {
                if (typeof window.Window_BattleLog !== 'undefined' && window.Window_BattleLog.prototype) {
                    if (window.Window_BattleLog.prototype._gb_battle_hooked) return;
                    window.Window_BattleLog.prototype._gb_battle_hooked = true;
                    const origAddText = window.Window_BattleLog.prototype.addText;
                    if (origAddText) {
                        window.Window_BattleLog.prototype.addText = function(text) {
                            if (text && typeof text === 'string') {
                                text = translateOrCapture(text, 'BATTLE_UI', window.__GB_SETTINGS.translateBattleUi);
                            }
                            return origAddText.call(this, text);
                        };
                    }
                }
            }

            // =========================================================================
            // 7. HOOK: Window_NameBox (MZ / Yanfly Name Box)
            // =========================================================================
            function installNameBoxHook() {
                if (typeof window.Window_NameBox !== 'undefined' && window.Window_NameBox.prototype) {
                    if (window.Window_NameBox.prototype._gb_name_hooked) return;
                    window.Window_NameBox.prototype._gb_name_hooked = true;
                    const origRefresh = window.Window_NameBox.prototype.refresh;
                    if (origRefresh) {
                        window.Window_NameBox.prototype.refresh = function() {
                            if (this._name && typeof this._name === 'string') {
                                this._name = translateOrCapture(this._name, 'DIALOG', window.__GB_SETTINGS.translateDialog);
                            }
                            return origRefresh.call(this);
                        };
                    }
                }
            }

            // =========================================================================
            // 8. HOOK: Window_Base.prototype.drawTextEx (RPG Maker Complex Text with Codes)
            // =========================================================================
            function installWindowBaseHook() {
                if (typeof window.Window_Base !== 'undefined' && window.Window_Base.prototype) {
                    if (window.Window_Base.prototype._gb_base_hooked) return;
                    window.Window_Base.prototype._gb_base_hooked = true;
                    const origDrawTextEx = window.Window_Base.prototype.drawTextEx;
                    if (origDrawTextEx) {
                        window.Window_Base.prototype.drawTextEx = function(text, x, y, width) {
                            if (text && typeof text === 'string') {
                                text = translateOrCapture(text, 'CANVAS_UI', window.__GB_SETTINGS.translateUi);
                            }
                            if (width !== undefined) {
                                return origDrawTextEx.call(this, text, x, y, width);
                            } else {
                                return origDrawTextEx.call(this, text, x, y);
                            }
                        };
                    }
                }
            }

            // =========================================================================
            // 9. HOOK: CanvasRenderingContext2D.prototype.fillText (HTML5 Canvas Fallback)
            // =========================================================================
            function installCanvasContextHook() {
                if (window.CanvasRenderingContext2D && window.CanvasRenderingContext2D.prototype.fillText) {
                    if (window.CanvasRenderingContext2D.prototype._gb_fillText_hooked) return;
                    window.CanvasRenderingContext2D.prototype._gb_fillText_hooked = true;
                    const origFillText = window.CanvasRenderingContext2D.prototype.fillText;
                    window.CanvasRenderingContext2D.prototype.fillText = function(text, x, y, maxWidth) {
                        if (text !== undefined && text !== null) {
                            text = translateOrCapture(String(text), 'CANVAS_UI', window.__GB_SETTINGS.translateUi);
                        }
                        if (maxWidth !== undefined) {
                            return origFillText.call(this, text, x, y, maxWidth);
                        } else {
                            return origFillText.call(this, text, x, y);
                        }
                    };
                }
            }

            // =========================================================================
            // 10. HOOK: DOM MutationObserver for HTML/Web UI & Dialogue Elements
            // =========================================================================
            function installDomObserver() {
                if (!window.MutationObserver) return;
                
                function translateDomNode(node) {
                    if (node.nodeType === Node.TEXT_NODE) {
                        const content = node.nodeValue;
                        if (content && isTranslatable(content)) {
                            const trimmed = content.trim();
                            if (window.__GB_SETTINGS.translateUi && window.__GB_TRANS_CACHE[trimmed]) {
                                node.nodeValue = content.replace(trimmed, window.__GB_TRANS_CACHE[trimmed]);
                            } else {
                                sendToNative(trimmed, 'SYSTEM');
                            }
                        }
                    } else if (node.nodeType === Node.ELEMENT_NODE) {
                        if (['SCRIPT', 'STYLE', 'CANVAS', 'INPUT', 'TEXTAREA'].indexOf(node.tagName) !== -1) return;
                        for (let i = 0; i < node.childNodes.length; i++) {
                            translateDomNode(node.childNodes[i]);
                        }
                    }
                }

                const observer = new MutationObserver(function(mutations) {
                    for (let m = 0; m < mutations.length; m++) {
                        const mutation = mutations[m];
                        if (mutation.type === 'childList') {
                            for (let i = 0; i < mutation.addedNodes.length; i++) {
                                translateDomNode(mutation.addedNodes[i]);
                            }
                        } else if (mutation.type === 'characterData') {
                            translateDomNode(mutation.target);
                        }
                    }
                });

                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
                    translateDomNode(document.body);
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        observer.observe(document.body, { childList: true, subtree: true, characterData: true });
                        translateDomNode(document.body);
                    });
                }
            }

            // Repeated checks to hook as soon as RPG Maker scripts load
            let attempts = 0;
            const hookInterval = setInterval(function() {
                attempts++;
                installBitmapHook();
                installCommandHook();
                installMessageHook();
                installChoiceHook();
                installHelpHook();
                installBattleLogHook();
                installNameBoxHook();
                installWindowBaseHook();
                installCanvasContextHook();
                if (attempts > 30 || (typeof window.Bitmap !== 'undefined' && window.Bitmap.prototype)) {
                    clearInterval(hookInterval);
                }
            }, 300);

            installDomObserver();
            installCanvasContextHook();
            console.log('[GameBridge-MTool] Live Localization Engine active and ready.');
        })();
        """.trimIndent()
    }
}
