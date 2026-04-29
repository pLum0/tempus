# Sleep Timer Feature – Änderungsübersicht

## Quick Start
1. Fork von `eddyizm/tempus` erstellen
2. Diese Änderungen auf einen Branch `sleep-timer` pushen
3. GitHub Actions baut automatisch die APK (Workflow ist in `.github/workflows/build-sleep-timer.yml`)
4. Oder lokal: `bash gradlew assembleTempusDebug`

## Was macht das Feature?
- **Sleep Timer** mit Timer-Optionen: 5, 10, 15, 30, 45, 60, 90, 120 Minuten
- **"Ende des Liedes"** – pausiert nach dem aktuellen Track
- **Fade-Out** – Lautstärke wird 3 Sekunden vor Ablauf langsam reduziert
- Sleep Timer Button im Player (zwischen Equalizer und Queue)
- Icon wechselt zu farbig wenn Timer aktiv
- Timer läuft im Service – überlebt App-Minimierung

## Geänderte Dateien

### Neue Dateien
| Datei | Beschreibung |
|---|---|
| `.github/workflows/build-sleep-timer.yml` | CI/CD für automatischen APK Build |
| `app/src/main/res/drawable/ic_sleep_timer.xml` | Mond-Icon (inaktiv) |
| `app/src/main/res/drawable/ic_sleep_timer_active.xml` | Mond-Icon (aktiv, colorPrimary) |
| `app/src/main/res/layout/dialog_sleep_timer.xml` | Dialog-Layout mit NumberPicker |
| `app/src/main/java/.../ui/dialog/SleepTimerDialog.java` | Dialog-Logik (Start/Stop/Anzeige) |

### Modifizierte Dateien
| Datei | Änderung |
|---|---|
| `app/build.gradle` | applicationId → `.sleeptimer` (parallele Installation) |
| `app/src/main/res/values/strings.xml` | App-Name → "Tempus Sleep" + 8 Sleep Timer Strings (EN) |
| `app/src/main/res/values-de/strings.xml` | DE Strings |
| `app/src/main/res/values-fr/strings.xml` | FR Strings |
| `app/src/main/res/values-it/strings.xml` | IT Strings |
| `app/src/main/res/values-es-rES/strings.xml` | ES Strings |
| `app/src/main/res/values-ca/strings.xml` | CA Strings |
| `app/src/main/res/values-ko/strings.xml` | KO Strings |
| `app/src/main/res/values-pl/strings.xml` | PL Strings |
| `app/src/main/res/values-pt/strings.xml` | PT Strings |
| `app/src/main/res/values-ro/strings.xml` | RO Strings |
| `app/src/main/res/values-ru/strings.xml` | RU Strings |
| `app/src/main/res/values-tr/strings.xml` | TR Strings |
| `app/src/main/res/values-zh/strings.xml` | ZH Strings |
| `app/src/main/res/values-zh-rTW/strings.xml` | ZH-TW Strings |
| `app/src/main/res/values-b+es+419/strings.xml` | ES-419 Strings |
| `app/src/main/java/.../util/Preferences.kt` | 7 Preference-Methoden + Konstanten |
| `app/src/main/java/.../service/BaseMediaService.kt` | Sleep Timer Engine (Timer, Fade-Out, End-of-Track, BroadcastReceiver) |
| `app/src/main/res/layout/inner_fragment_player_controller_layout.xml` | Sleep Timer Button im Player |
| `app/src/main/java/.../ui/fragment/PlayerControllerFragment.java` | Button-Verkabelung, Icon-Update |

## Architektur
```
UI (SleepTimerDialog)
  ↓ Broadcast (ACTION_SLEEP_TIMER_START / END_OF_TRACK / CANCEL)
Service (BaseMediaService)
  ├─ CountDownTimer → tickt jede Sekunde → Preferences aktualisieren
  ├─ onFinish() → fadeOutAndPause() → 3s Fade → player.pause()
  ├─ End-of-Track: songId merken → onMediaItemTransition → pause bei Wechsel
  └─ BroadcastReceiver: empfängt Start/Stop/Cancel von UI
```

## Hinweise
- Die applicationId ist `com.eddyizm.tempus.sleeptimer` – kann parallel zum Original installiert werden
- Der Debug-Build ist mit dem Standard-Debug-Key signiert
- Für einen PR an eddyizm sollte die applicationId wieder zurückgesetzt werden
